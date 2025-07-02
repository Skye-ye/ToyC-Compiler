package toyc.analysis.graph.icfg;

import toyc.language.Function;

/**
 * The edge connecting a call site to method entry of the callee.
 *
 * @param <Node> type of nodes
 */
public class CallEdge<Node> extends ICFGEdge<Node> {

    /**
     * Callee of the call edge.
     */
    private final Function callee;

    CallEdge(Node source, Node target, Function callee) {
        super(source, target);
        this.callee = callee;
    }

    /**
     * @return the callee of the call edge.
     */
    public Function getCallee() {
        return callee;
    }
}
