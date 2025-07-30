package toyc.algorithm.analysis.graph.callgraph;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import toyc.World;
import toyc.algorithm.analysis.ProgramAnalysis;
import toyc.config.AlgorithmConfig;
import toyc.config.AlgorithmOptions;
import toyc.ir.stmt.Call;
import toyc.language.Function;

import java.io.File;

public class CallGraphBuilder extends ProgramAnalysis<CallGraph<Call, Function>> {

    public static final String ID = "cg";

    private static final Logger logger = LogManager.getLogger(CallGraphBuilder.class);

    private static final String CALL_GRAPH_FILE = "call-graph.dot";

    private static final String REACHABLE_METHODS_FILE = "reachable-functions.txt";

    private static final String CALL_EDGES_FILE = "call-edges.txt";

    public CallGraphBuilder(AlgorithmConfig config) {
        super(config);
    }

    @Override
    public CallGraph<Call, Function> analyze() {
        CGBuilder<Call, Function> builder;
        builder = new ToyCCallGraphBuilder();
        CallGraph<Call, Function> callGraph = builder.build();
        logStatistics(callGraph);
        processOptions(callGraph, getOptions());
        return callGraph;
    }

    private static void logStatistics(CallGraph<Call, Function> callGraph) {
        logger.info("Call graph has {} reachable methods and {} edges",
                callGraph.getNumberOfFunctions(),
                callGraph.getNumberOfEdges());
    }

    private static void processOptions(CallGraph<Call, Function> callGraph,
                                       AlgorithmOptions options) {
        File outputDir = World.get().getOptions().getOutputDir();
        if (options.getBoolean("dump")) {
            CallGraphs.dumpCallGraph(callGraph,
                    new File(outputDir, CALL_GRAPH_FILE));
        }
        if (options.getBoolean("dump-functions")) {
            CallGraphs.dumpFunctions(callGraph,
                    new File(outputDir, REACHABLE_METHODS_FILE));
        }
        if (options.getBoolean("dump-call-edges")) {
            CallGraphs.dumpCallEdges(callGraph,
                    new File(outputDir, CALL_EDGES_FILE));
        }
    }
}
