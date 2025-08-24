package toyc.algorithm;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import toyc.World;
import toyc.algorithm.analysis.FunctionAnalysis;
import toyc.algorithm.analysis.ProgramAnalysis;
import toyc.algorithm.analysis.graph.callgraph.CallGraph;
import toyc.algorithm.analysis.graph.callgraph.CallGraphBuilder;
import toyc.algorithm.optimization.Optimization;
import toyc.config.*;
import toyc.ir.IR;
import toyc.language.Function;
import toyc.util.AnalysisException;
import toyc.util.Timer;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * Creates and executes analyses based on given analysis plan.
 */
public class AlgorithmManager {

    private static final Logger logger = LogManager.getLogger(AlgorithmManager.class);

    private final Plan plan;

    private List<Function> functionScope;

    private static final int MAX_ITERATIONS = 100;

    public AlgorithmManager(Plan plan) {
        this.plan = plan;
    }

    /**
     * Executes the analysis plan.
     */
    public void execute() {
        executePlan(plan);
    }

    private boolean executePlan(Plan plan) {
        if (plan == null || plan.analyses().isEmpty()) {
            logger.info("No analyses to execute");
            return false; // No changes made
        }

        boolean globalChanges = false;
        boolean hasChanges;
        int iterations = 0;

        do {
            functionScope = null; // Reset scope for each iteration
            hasChanges = false;

            // Execute each plan element and check for changes
            for (PlanElement element : plan.analyses()) {
                boolean elementChanged = Timer.runAndCount(
                        () -> runPlanElement(element),
                        getPlanElementName(element),
                        Level.INFO);

                if (elementChanged) {
                    hasChanges = true;
                    globalChanges = true;
                    logger.debug("Changes detected in {}", getPlanElementName(element));
                }
            }

            iterations++;
            if (iterations >= MAX_ITERATIONS) {
                break;
            }
        } while (hasChanges);

        return globalChanges;
    }

    private boolean runPlanElement(PlanElement planElement) {
        return switch (planElement) {
            case AlgorithmConfig ac -> runAlgorithm(ac);
            case Plan p -> executePlan(p);
        };
    }

    private String getPlanElementName(PlanElement element) {
        return switch (element) {
            case AlgorithmConfig ac -> ac.getId();
            case Plan p -> "NestedPlan(" + p.analyses().size() + " analyses)";
        };
    }

    private boolean runAlgorithm(AlgorithmConfig config) {
        Algorithm algorithm;
        // Create analysis instance
        try {
            Class<?> clazz = Class.forName(config.getAlgorithmClass());
            Constructor<?> ctor = clazz.getConstructor(AlgorithmConfig.class);
            algorithm = (Algorithm) ctor.newInstance(config);
        } catch (ClassNotFoundException e) {
            throw new AnalysisException("Analysis class " +
                    config.getAlgorithmClass() + " is not found", e);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new AnalysisException("Failed to get constructor " +
                    config.getAlgorithmClass() + "(AnalysisConfig), " +
                    "thus the analysis cannot be executed by compiler", e);
        } catch (InstantiationException | InvocationTargetException e) {
            throw new AnalysisException("Failed to initialize " +
                    config.getAlgorithmClass(), e);
        } catch (ClassCastException e) {
            throw new ConfigException(
                    config.getAlgorithmClass() + " is not an analysis class");
        }
        // Run the analysis
        return switch (algorithm) {
            case ProgramAnalysis<?> pa -> runProgramAnalysis(pa);
            case FunctionAnalysis<?> ma -> runFunctionAnalysis(ma);
            case Optimization opt -> runOptimization(opt);
            default -> throw new ConfigException(config.getAlgorithmClass() +
                    " is not a supported analysis class");
        };
    }

    private boolean runProgramAnalysis(ProgramAnalysis<?> analysis) {
        Object result = analysis.analyze();
        if (result != null) {
            World.get().storeResult(analysis.getId(), result);
        }
        return false; // Program analyses does not modify IR
    }

    private boolean runFunctionAnalysis(FunctionAnalysis<?> analysis) {
        getFunctionScope()
                .forEach(m -> {
                    IR ir = m.getIR();
                    Object result = analysis.analyze(ir);
                    if (result != null) {
                        ir.storeResult(analysis.getId(), result);
                    }
                });
        return false; // Function analyses does not modify IR
    }

    // Run optimization sequentially to avoid concurrent modifications
    // TODO: consider parallel optimizations
    private boolean runOptimization(Optimization optimization) {
        List<Function> functions = getFunctionScope();
        boolean modified = false;

        for (Function function : functions) {
            IR ir = function.getIR();
            IR optimizedIR = optimization.optimize(ir);
            if (!optimizedIR.equals(ir)) {
                modified = true;
            }
            function.setIR(optimizedIR);
        }

        return modified;
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
}
