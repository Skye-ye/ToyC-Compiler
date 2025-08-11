package toyc.algorithm.analysis.graph.callgraph;

import toyc.algorithm.analysis.StmtResult;
import toyc.util.graph.Graph;

import java.util.Set;
import java.util.stream.Stream;

/**
 * Representation of call graph.
 *
 * @param <CallSite> type of call sites
 * @param <Function>   type of Functions
 */
public interface CallGraph<CallSite, Function>
        extends Graph<Function>, StmtResult<Set<Function>> {

    /**
     * @return the call sites that invoke the given Function.
     */
    Set<CallSite> getCallersOf(Function callee);

    /**
     * @return the Function that are called by the given call site.
     */
    Function getCalleeOf(CallSite callSite);

    /**
     * @return the Functions that are called by all call sites in the given Function.
     */
    Set<Function> getCalleesOfF(Function caller);

    /**
     * @return the Function that contains the given call site.
     */
    Function getContainerOf(CallSite callSite);

    /**
     * @return the call sites within the given Function.
     */
    Set<CallSite> getCallSitesIn(Function Function);

    /**
     * @return the call sites within the given Function.
     */
    default Stream<CallSite> callSitesIn(Function Function) {
        return getCallSitesIn(Function).stream();
    }

    /**
     * @return the call edges out of the given call site.
     */
    Edge<CallSite, Function> edgeOutOf(CallSite callSite);

    /**
     * @return the call edges targeting to the given Function.
     */
    Stream<Edge<CallSite, Function>> edgesInTo(Function Function);

    /**
     * @return all call edges in this call graph.
     */
    Stream<Edge<CallSite, Function>> edges();

    /**
     * @return the number of call graph edges in this call graph.
     */
    int getNumberOfEdges();

    /**
     * @return the entry Function of this call graph.
     */
    Function entryFunction();

    /**
     * @return all reachable Functions in this call graph.
     */
    Stream<Function> reachableFunctions();

    /**
     * @return the number of reachable Functions in this call graph.
     */
    int getNumberOfFunctions();

    /**
     * @return true if this call graph contains the given Function, otherwise false.
     */
    boolean contains(Function Function);
}
