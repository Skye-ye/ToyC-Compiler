package toyc.analysis.graph.icfg;

import toyc.analysis.graph.callgraph.CallGraph;

import java.util.Set;

abstract class AbstractICFG<Function, Node> implements ICFG<Function, Node> {

    protected final CallGraph<Node, Function> callGraph;

    protected AbstractICFG(CallGraph<Node, Function> callGraph) {
        this.callGraph = callGraph;
    }

    public Function entryFunction() {
        return callGraph.entryFunction();
    }

    @Override
    public Function getCalleeOf(Node callSite) {
        return callGraph.getCalleeOf(callSite);
    }

    @Override
    public Set<Node> getCallersOf(Function method) {
        return callGraph.getCallersOf(method);
    }
}
