package toyc.analysis;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import toyc.World;
import toyc.analysis.graph.callgraph.CallGraph;
import toyc.analysis.graph.callgraph.CallGraphBuilder;
import toyc.config.AnalysisConfig;
import toyc.config.ConfigException;
import toyc.config.Plan;
import toyc.config.Scope;
import toyc.ir.IR;
import toyc.language.Function;
import toyc.util.AnalysisException;
import toyc.util.Timer;
import toyc.util.graph.SimpleGraph;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * Creates and executes analyses based on given analysis plan.
 */
public class AnalysisManager {

    private static final Logger logger = LogManager.getLogger(AnalysisManager.class);

    private final Plan plan;

    /**
     * Whether keep results of all analyses. If the value is {@code false},
     * this manager will clear unused results after it finishes each analysis.
     */
    private final boolean keepAllResults;

    /**
     * Graph that describes the dependencies among analyses (represented
     * by their IDs) in the plan. This graph is used to check whether
     * certain analysis results are useful.
     */
    private SimpleGraph<String> dependenceGraph;

    /**
     * List of analyses that have been executed. For an element in this list,
     * once its result is clear, it will also be removed from this list.
     */
    private List<Analysis> executedAnalyses;

    private List<Function> functionScope;

    public AnalysisManager(Plan plan) {
        this.plan = plan;
        this.keepAllResults = plan.keepResult().contains(Plan.KEEP_ALL);
    }

    /**
     * Executes the analysis plan.
     */
    public void execute() {
        // initialize
        if (!keepAllResults) {
            dependenceGraph = new SimpleGraph<>();
            for (AnalysisConfig c : plan.dependenceGraph()) {
                for (AnalysisConfig succ : plan.dependenceGraph().getSuccsOf(c)) {
                    dependenceGraph.addEdge(c.getId(), succ.getId());
                }
            }
            executedAnalyses = new ArrayList<>();
        }
        functionScope = null;
        // execute analyses
        plan.analyses().forEach(config -> {
            Analysis analysis = Timer.runAndCount(
                    () -> runAnalysis(config), config.getId(), Level.INFO);
            if (!keepAllResults) {
                executedAnalyses.add(analysis);
                clearUnusedResults(analysis);
            }
        });
    }

    private Analysis runAnalysis(AnalysisConfig config) {
        Analysis analysis;
        // Create analysis instance
        try {
            Class<?> clazz = Class.forName(config.getAnalysisClass());
            Constructor<?> ctor = clazz.getConstructor(AnalysisConfig.class);
            analysis = (Analysis) ctor.newInstance(config);
        } catch (ClassNotFoundException e) {
            throw new AnalysisException("Analysis class " +
                    config.getAnalysisClass() + " is not found", e);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new AnalysisException("Failed to get constructor " +
                    config.getAnalysisClass() + "(AnalysisConfig), " +
                    "thus the analysis cannot be executed by Tai-e", e);
        } catch (InstantiationException | InvocationTargetException e) {
            throw new AnalysisException("Failed to initialize " +
                    config.getAnalysisClass(), e);
        } catch (ClassCastException e) {
            throw new ConfigException(
                    config.getAnalysisClass() + " is not an analysis class");
        }
        // Run the analysis
        if (analysis instanceof ProgramAnalysis<?> pa) {
            runProgramAnalysis(pa);
        } else if (analysis instanceof FunctionAnalysis<?> ma) {
            runMethodAnalysis(ma);
        } else {
            throw new ConfigException(config.getAnalysisClass() +
                    " is not a supported analysis class");
        }
        return analysis;
    }

    private void runProgramAnalysis(ProgramAnalysis<?> analysis) {
        Object result = analysis.analyze();
        if (result != null) {
            World.get().storeResult(analysis.getId(), result);
        }
    }

    private void runMethodAnalysis(FunctionAnalysis<?> analysis) {
        getFunctionScope()
                .parallelStream()
                .forEach(m -> {
                    IR ir = m.getIR();
                    Object result = analysis.analyze(ir);
                    if (result != null) {
                        ir.storeResult(analysis.getId(), result);
                    }
                });
    }

    private List<Function> getFunctionScope() {
        if (functionScope == null) {
            Scope scope = World.get().getOptions().getScope();
            functionScope = switch (scope) {
                case ALL -> World.get().getProgram().allFunctions().toList();
                case REACHABLE -> {
                    CallGraph<?, Function> callGraph =
                            World.get().getResult(CallGraphBuilder.ID);
                    yield callGraph.reachableFunctions().toList();
                }
            };
            logger.info("{} methods in scope ({}) of method analyses",
                    functionScope.size(), scope);
        }
        return functionScope;
    }

    /**
     * @param analysis the analysis that just finished.
     */
    private void clearUnusedResults(Analysis analysis) {
        // analysis has finished, we can remove its dependencies
        // copy in-edges to a new list to avoid concurrent modifications
        var edgesToRemove = new ArrayList<>(
                dependenceGraph.getInEdgesOf(analysis.getId()));
        edgesToRemove.forEach(e ->
                dependenceGraph.removeEdge(e.source(), e.target()));
        // select the analyses whose results are unused and not in keepResult
        List<String> unused = executedAnalyses.stream()
                .map(Analysis::getId)
                .filter(id -> dependenceGraph.getOutDegreeOf(id) == 0)
                .filter(id -> !plan.keepResult().contains(id))
                .toList();
        if (!unused.isEmpty()) {
            logger.info("Clearing unused results of {}", unused);
            for (String id : unused) {
                int i;
                for (i = 0; i < executedAnalyses.size(); ++i) {
                    Analysis a = executedAnalyses.get(i);
                    if (a.getId().equals(id)) {
                        if (a instanceof ProgramAnalysis) {
                            World.get().clearResult(id);
                        } else if (a instanceof FunctionAnalysis) {
                            getFunctionScope().forEach(f -> f.getIR().clearResult(id));
                        }
                        break;
                    }
                }
                executedAnalyses.remove(i);
            }
        }
    }
}
