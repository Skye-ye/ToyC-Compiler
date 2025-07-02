package toyc.analysis.graph.callgraph;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import toyc.World;
import toyc.ir.IR;
import toyc.ir.stmt.Call;
import toyc.language.Function;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.HashSet;

/**
 * Builds call graph for ToyC via simple function call analysis.
 * Since ToyC is a procedural language without inheritance or dynamic dispatch,
 * this is much simpler than class hierarchy analysis for object-oriented languages.
 */
public class ToyCCallGraphBuilder implements CGBuilder<Call, Function> {

    private static final Logger logger = LogManager.getLogger(ToyCCallGraphBuilder.class);

    @Override
    public CallGraph<Call, Function> build() {
        return buildCallGraph();
    }

    private CallGraph<Call, Function> buildCallGraph() {
        Map<String, IR> allIRs = World.get().getIRBuilder().getFunctions();
        Function mainFunction = World.get().getMainFunction();
        
        logger.info("Building call graph for ToyC starting from function: {}", mainFunction.getName());
        
        DefaultCallGraph callGraph = new DefaultCallGraph();
        callGraph.addEntryFunction(mainFunction);
        
        Queue<Function> workList = new ArrayDeque<>();
        Set<Function> processed = new HashSet<>();
        workList.add(mainFunction);
        
        while (!workList.isEmpty()) {
            Function function = workList.poll();
            
            if (!processed.add(function)) {
                // Already processed this function
                continue;
            }
            
            callGraph.addReachableFunction(function);
            
            // Find the IR for this function
            IR ir = allIRs.get(function.getName());
            if (ir != null) {
                // Process all call sites in this function
                ir.forEach(stmt -> {
                    if (stmt instanceof Call call) {
                        Function callee = call.getCallExp().getFunction();
                        if (callee != null) {
                            if (!processed.contains(callee)) {
                                workList.add(callee);
                            }
                            callGraph.addEdge(new Edge<>(call, callee));
                        }
                    }
                });
            }
        }
        
        logger.info("Call graph built with {} functions and {} call edges", 
                   callGraph.getNumberOfFunctions(), callGraph.getNumberOfEdges());
        
        return callGraph;
    }
}