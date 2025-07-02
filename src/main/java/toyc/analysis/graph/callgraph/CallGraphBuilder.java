package toyc.analysis.graph.callgraph;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import toyc.World;
import toyc.analysis.ProgramAnalysis;
import toyc.ir.stmt.Call;
import toyc.language.Function;

import java.io.File;

public class CallGraphBuilder extends ProgramAnalysis<CallGraph<Call, Function>> {

    public static final String ID = "cg";

    private static final Logger logger = LogManager.getLogger(CallGraphBuilder.class);

    private static final String CALL_GRAPH_FILE = "call-graph.dot";

    private static final String REACHABLE_FUNCTIONS_FILE = "reachable-functions.txt";

    private static final String CALL_EDGES_FILE = "call-edges.txt";

    public CallGraphBuilder(String id) {
        super(id);
    }

    @Override
    public CallGraph<Call, Function> analyze() {
        CGBuilder<Call, Function> builder = new ToyCCallGraphBuilder();
        CallGraph<Call, Function> callGraph = builder.build();
        File outputDir = World.get().getOutputDir();
        CallGraphs.dumpCallGraph(callGraph, new File(outputDir, CALL_GRAPH_FILE));
        CallGraphs.dumpFunctions(callGraph, new File(outputDir, REACHABLE_FUNCTIONS_FILE));
        CallGraphs.dumpCallEdges(callGraph, new File(outputDir, CALL_EDGES_FILE));
        return callGraph;
    }

    private static void logStatistics(CallGraph<Call, Function> callGraph) {
        logger.info("Call graph has {} reachable functions and {} edges",
                callGraph.getNumberOfFunctions(),
                callGraph.getNumberOfEdges());
    }
}
