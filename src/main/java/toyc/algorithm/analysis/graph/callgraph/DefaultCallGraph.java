package toyc.algorithm.analysis.graph.callgraph;

import toyc.ir.stmt.Call;
import toyc.ir.stmt.Stmt;
import toyc.language.Function;

import java.util.Set;

/**
 * Default implementation of call graph.
 */
public class DefaultCallGraph extends AbstractCallGraph<Call, Function> {

    /**
     * Adds an entry Function to this call graph.
     */
    public void addEntryFunction(Function entryFunction) {
        super.addEntryFunction(entryFunction);
        // Also process the entry function's Call statements
        entryFunction.getIR().forEach(stmt -> {
            if (stmt instanceof Call call) {
                callSiteToContainer.put(call, entryFunction);
                callSitesIn.put(entryFunction, call);
            }
        });
    }

    /**
     * Adds a reachable function to this call graph.
     *
     * @return true if this call graph changed as a result of the call,
     * otherwise false.
     */
    public boolean addReachableFunction(Function function) {
        boolean wasAdded = reachableFunctions.add(function);
        
        // Always process the IR statements to find call sites, even if the function was already reachable
        // This ensures that all call sites are collected properly
        if (!callSitesIn.containsKey(function)) {
            function.getIR().forEach(stmt -> {
                if (stmt instanceof Call call) {
                    callSiteToContainer.put(call, function);
                    callSitesIn.put(function, call);
                }
            });
        }
        
        return wasAdded;
    }

    /**
     * Adds a new call graph edge to this call graph.
     *
     * @param edge the call edge to be added
     * @return true if the call graph changed as a result of the call,
     * otherwise false.
     */
    public boolean addEdge(Edge<Call, Function> edge) {
        Call callSite = edge.getCallSite();
        Function callee = edge.getCallee();
        Function container = callSite.getContainer(); // Use the container from the Call object directly
        
        if (container == null) {
            // If container is not set, we can't add this edge
            return false;
        }
        
        if (!callSiteToCallee.containsKey(callSite)) {
            // Add the edge mappings
            callSiteToCallee.put(callSite, callee);
            calleeToCallSites.put(callee, callSite);
            callSiteToContainer.put(callSite, container);
            callSitesIn.put(container, callSite);
            reachableFunctions.add(callee);
            return true;
        }
        return false;
    }

    @Override
    public Function getContainerOf(Call call) {
        return call.getContainer();
    }

    @Override
    public boolean isRelevant(Stmt stmt) {
        return stmt instanceof Call;
    }

    @Override
    public Set<Function> getResult(Stmt stmt) {
        Function callee = getCalleeOf((Call) stmt);
        return callee != null ? Set.of(callee) : Set.of();
    }
}
