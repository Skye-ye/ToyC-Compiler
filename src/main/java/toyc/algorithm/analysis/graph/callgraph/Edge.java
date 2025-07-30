package toyc.algorithm.analysis.graph.callgraph;

import toyc.util.Hashes;

/**
 * Represents call edges in the call graph.
 *
 * @param <CallSite> type of call sites
 * @param <Function>   type of Functions
 */
public class Edge<CallSite, Function> {

    private final CallSite callSite;

    private final Function callee;

    private final int hashCode;

    public Edge(CallSite callSite, Function callee) {
        this.callSite = callSite;
        this.callee = callee;
        hashCode = Hashes.hash(callSite, callee);
    }

    /**
     * @return the call site (i.e., the source) of the call edge.
     */
    public CallSite getCallSite() {
        return callSite;
    }

    /**
     * @return the callee Function (i.e., the target) of the call edge.
     */
    public Function getCallee() {
        return callee;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Edge<?, ?> edge = (Edge<?, ?>) o;
        return callSite.equals(edge.callSite) &&
                callee.equals(edge.callee);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public String toString() {
        return callSite + " -> " + callee;
    }
}
