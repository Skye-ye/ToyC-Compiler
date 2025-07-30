package toyc.algorithm.analysis.graph.callgraph;

import toyc.util.graph.Edge;

/**
 * Represents call edge between caller and callee.
 */
record FunctionEdge<CallSite, Function>(
        Function caller, Function callee, CallSite callSite)
        implements Edge<Function> {

    @Override
    public Function source() {
        return caller;
    }

    @Override
    public Function target() {
        return callee;
    }
}
