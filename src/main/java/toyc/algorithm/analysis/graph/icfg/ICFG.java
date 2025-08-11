package toyc.algorithm.analysis.graph.icfg;

import toyc.util.graph.Graph;

import java.util.Set;

/**
 * Represents an inter-procedural control-flow graph.
 */
public interface ICFG<Function, Node> extends Graph<Node> {

    /**
     * @return entry functions of the ICFG.
     */
    Function entryFunction();

    /**
     * @return the incoming edges of the given node.
     */
    @Override
    Set<ICFGEdge<Node>> getInEdgesOf(Node node);

    /**
     * @return the outgoing edges of the given node.
     */
    @Override
    Set<ICFGEdge<Node>> getOutEdgesOf(Node node);

    /**
     * @return the function that is called by the given call site.
     */
    Function getCalleeOf(Node callSite);

    /**
     * @return the return sites of the given call site.
     */
    Set<Node> getReturnSitesOf(Node callSite);

    /**
     * @return the entry node of the given function.
     */
    Node getEntryOf(Function function);

    /**
     * @return the exit node of the given function.
     */
    Node getExitOf(Function function);

    /**
     * @return the call sites that invoke the given function.
     */
    Set<Node> getCallersOf(Function function);

    /**
     * @return the function that contains the given node.
     */
    Function getContainingFunctionOf(Node node);

    /**
     * @return true if the given node is a call site, otherwise false.
     */
    boolean isCallSite(Node node);
}
