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

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class Compiler {
    private static final Logger logger = LogManager.getLogger(Compiler.class);

    public static void main(String... args) throws IOException {
        Timer.runAndCount(() -> {
            Options options = Options.parse();
            // Only proceed with analysis if not showing help
            LoggerConfigs.setOutput(options.getOutputDir());
            Plan plan = processConfigs(options);
            if (plan.analyses().isEmpty()) {
                logger.info("No analyses are specified");
                System.exit(0);
            }
            buildWorld(options);
            executePlan(plan);
        }, "ToyC Compiler");
        LoggerConfigs.reconfigure();
        generateAssembly();
    }

    private static Plan processConfigs(Options options) {
        InputStream content = Configs.getAlgorithmConfig();
        List<AlgorithmConfig> algorithmConfigs = AlgorithmConfig.parseConfigs(content);
        ConfigManager manager = new ConfigManager(algorithmConfigs);
        AlgorithmPlanner planner = new AlgorithmPlanner(manager);
        boolean reachableScope = options.getScope().equals(Scope.REACHABLE);
        if (options.getPlanFile() != null) {
            // Analyses are specified by file
            List<PlanConfig> planConfigs = PlanConfig.readConfigs(options.getPlanFile());
            return planner.makePlan(planConfigs, reachableScope);
        }
        // No analyses are specified
        return Plan.emptyPlan();
    }

    private static void buildWorld(Options options) {
        Timer.runAndCount(() -> {
            try {
                Class<? extends WorldBuilder> builderClass = options.getWorldBuilderClass();
                Constructor<? extends WorldBuilder> builderCtor = builderClass.getConstructor();
                WorldBuilder builder = builderCtor.newInstance();
                if (options.isWorldCacheMode()) {
                    builder = new CachedWorldBuilder(builder);
                }
                builder.build(options);
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
        new AlgorithmManager(plan).execute();
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

                System.out.print(assembly);

            } catch (Exception e) {
                logger.error("Assembly generation failed: {}", e.getMessage());
                System.exit(1);
            }
        }, "AssemblyGeneration");
    }
}