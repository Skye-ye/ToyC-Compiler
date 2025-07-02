package toyc.analysis.graph.callgraph;

interface CGBuilder<CallSite, Function> {

    CallGraph<CallSite, Function> build();
}
