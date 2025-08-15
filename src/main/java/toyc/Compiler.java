package toyc;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import toyc.algorithm.AlgorithmManager;
import toyc.algorithm.analysis.graph.callgraph.CallGraph;
import toyc.algorithm.analysis.graph.callgraph.CallGraphBuilder;
import toyc.codegen.RISCV32Generator;
import toyc.config.*;
import toyc.frontend.cache.CachedWorldBuilder;
import toyc.ir.IR;
import toyc.ir.IRPrinter;
import toyc.language.Function;
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
            // For OJ submission, ignore command line args
            Options options = processArgs();
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
        generateAssembly();
    }

    private static Options processArgs(String... args) {
        // For OJ submission, always parse without arguments
        Options options = Options.parse();
        if (options.isPrintHelp()) {
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

    public static void buildWorld(String... args) {
        Options options = Options.parse();
        LoggerConfigs.setOutput(options.getOutputDir());
        Plan plan = processConfigs(options);
        buildWorld(options, plan.analyses());
        LoggerConfigs.reconfigure();
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
        //System.out.println("\n========== IR Output ==========");
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
            //IRPrinter.print(ir, System.out);
            //System.out.println();
        }
        //System.out.println("========== End IR Output ==========");
    }

    private static void generateAssembly() {
        Timer.runAndCount(() -> {
            logger.info("Generating RISC-V assembly...");
            
            // Get functions to generate code for
            Scope scope = World.get().getOptions().getScope();
            List<Function> functionScope = switch (scope) {
                case ALL -> World.get().getProgram().allFunctions().toList();
                case REACHABLE -> {
                    CallGraph<?, Function> callGraph =
                            World.get().getResult(CallGraphBuilder.ID);
                    yield callGraph.reachableFunctions().toList();
                }
            };
            
            try {
                RISCV32Generator generator = new RISCV32Generator();
                String assembly = generator.generateProgramAssembly(functionScope);
                
                // Output to file
                String outputPath = World.get().getOptions().getOutputDir().toPath()
                        .resolve("output.s").toString();
                try (java.io.PrintWriter writer = new java.io.PrintWriter(outputPath)) {
                    writer.print(assembly);
                    logger.info("Assembly generated: {}", outputPath);
                }
                
                // Also print to console
                //System.out.println("\n========== Assembly Output ==========");
                System.out.print(assembly);
                //System.out.println("========== End Assembly Output ==========");
                
            } catch (Exception e) {
                logger.error("Assembly generation failed: {}", e.getMessage());
                System.exit(1);
            }
        }, "AssemblyGeneration");
    }
}