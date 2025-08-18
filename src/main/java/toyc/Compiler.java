package toyc;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import toyc.algorithm.Algorithm;
import toyc.algorithm.AlgorithmManager;
import toyc.algorithm.analysis.graph.callgraph.CallGraph;
import toyc.algorithm.analysis.graph.callgraph.CallGraphBuilder;
import toyc.config.*;
import toyc.frontend.cache.CachedWorldBuilder;
import toyc.ir.IR;
import toyc.ir.IRPrinter;
import toyc.language.Function;
import toyc.util.AnalysisException;
import toyc.util.Timer;
import toyc.util.collection.Lists;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class Compiler {
    private static final Logger logger = LogManager.getLogger(Compiler.class);

    public static void main(String... args) throws IOException {
        Timer.runAndCount(() -> {
            Options options = processArgs(args);
            // Only proceed with analysis if not showing help
            LoggerConfigs.setOutput(options.getOutputDir());
            Plan plan = processConfigs(options);
            if (plan.analyses().isEmpty()) {
                logger.info("No analyses are specified");
                System.exit(0);
            }
            buildWorld(options, plan.analyses());
            executePlan(plan);
        }, "ToyC Compiler");
        LoggerConfigs.reconfigure();
        printIR();
    }

    private static Options processArgs(String... args) {
        Options options = Options.parse(args);
        if (options.isPrintHelp() || args.length == 0) {
            options.printHelp();
            System.exit(0);
        }
        return options;
    }

    private static Plan processConfigs(Options options) {
        InputStream content = Configs.getAlgorithmConfig();
        List<AlgorithmConfig> algorithmConfigs = AlgorithmConfig.parseConfigs(content);
        ConfigManager manager = new ConfigManager(algorithmConfigs);
        AlgorithmPlanner planner = new AlgorithmPlanner(
                manager, options.getKeepResult());
        boolean reachableScope = options.getScope().equals(Scope.REACHABLE);
        if (!options.getAnalyses().isEmpty()) {
            // Analyses are specified by options
            List<PlanConfig> planConfigs = PlanConfig.readConfigs(options);
            manager.overwriteOptions(planConfigs);
            Plan plan = planner.expandPlan(
                    planConfigs, reachableScope);
            // Output analysis plan to file.
            // For outputting purpose, we first convert AnalysisConfigs
            // in the expanded plan to PlanConfigs
            planConfigs = Lists.map(plan.analyses(),
                    ac -> new PlanConfig(ac.getId(), ac.getOptions()));
            // TODO: turn off output in testing?
            PlanConfig.writeConfigs(planConfigs, options.getOutputDir());
            if (!options.isOnlyGenPlan()) {
                // This run not only generates plan file but also executes it
                return plan;
            }
        } else if (options.getPlanFile() != null) {
            // Analyses are specified by file
            List<PlanConfig> planConfigs = PlanConfig.readConfigs(options.getPlanFile());
            manager.overwriteOptions(planConfigs);
            return planner.makePlan(planConfigs, reachableScope);
        }
        // No analyses are specified
        return Plan.emptyPlan();
    }

    private static void buildWorld(Options options, List<AlgorithmConfig> analyses) {
        Timer.runAndCount(() -> {
            try {
                Class<? extends WorldBuilder> builderClass = options.getWorldBuilderClass();
                Constructor<? extends WorldBuilder> builderCtor = builderClass.getConstructor();
                WorldBuilder builder = builderCtor.newInstance();
                if (options.isWorldCacheMode()) {
                    builder = new CachedWorldBuilder(builder);
                }
                builder.build(options, analyses);
                logger.info("{} functions in the world",
                        World.get().getProgram().getFunctionCount());
            } catch (InstantiationException | IllegalAccessException |
                     NoSuchMethodException | InvocationTargetException e) {
                System.err.println("Failed to build world due to " + e);
                System.exit(1);
            }
        }, "WorldBuilder");
    }

    private static void executePlan(Plan plan) {
        AlgorithmManager am = new AlgorithmManager(plan);
        List<IR> previousIRs = null;
        List<IR> currentIRs;

        do {
            currentIRs = am.execute();

            // Check if this is not the first iteration and IR has changed
            if (previousIRs != null && previousIRs.equals(currentIRs)) {
                break;
            }

            previousIRs = currentIRs;
        } while (true); // Continue until IR stops changing
    }

    private static void printIR() {
        System.out.println("\n========== IR Output ==========");
        Scope scope = World.get().getOptions().getScope();
        List<Function> functionScope = switch (scope) {
            case ALL -> World.get().getProgram().allFunctions().toList();
            case REACHABLE -> {
                CallGraph<?, Function> callGraph =
                        World.get().getResult(CallGraphBuilder.ID);
                yield callGraph.reachableFunctions().toList();
            }
        };
        for (Function function : functionScope) {
            IR ir = function.getIR();
            IRPrinter.print(ir, System.out);
            System.out.println();
        }
        System.out.println("========== End IR Output ==========");
    }
}