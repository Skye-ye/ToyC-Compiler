package toyc.analysis.graph.callgraph;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import toyc.World;
import toyc.ir.stmt.Call;
import toyc.language.Function;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.Set;

/**
 * Builds call graph for ToyC via simple function call analysis.
 * Since ToyC is a procedural language without inheritance or dynamic dispatch,
 * this is much simpler than class hierarchy analysis for object-oriented languages.
 */
public class ToyCCallGraphBuilder implements CGBuilder<Call, Function> {

    private static final Logger logger = LogManager.getLogger(ToyCCallGraphBuilder.class);

    @Override
    public CallGraph<Call, Function> build() {
        return buildCallGraph(World.get().getMainFunction());
    }

    private CallGraph<Call, Function> buildCallGraph(Function entry) {
        DefaultCallGraph callGraph = new DefaultCallGraph();
        callGraph.addEntryFunction(entry);

        Queue<Function> workList = new ArrayDeque<>();
        workList.add(entry);

        while (!workList.isEmpty()) {
            Function function = workList.poll();

            // Always process call sites, even if the function was already reachable
            callGraph.addReachableFunction(function);
            Set<Call> callSites = callGraph.getCallSitesIn(function);
            callSites.forEach(
                    call -> {
                        Function callee = call.getCallExp().getFunction();
                        if (!callGraph.contains(callee)) {
                            workList.add(callee);
                        }
                        callGraph.addEdge(
                                new Edge<>(call, callee)
                        );
                    }
            );
        }

        logger.info("Call graph built with {} functions and {} call edges",
                callGraph.getNumberOfFunctions(), callGraph.getNumberOfEdges());

        return callGraph;
    }
}