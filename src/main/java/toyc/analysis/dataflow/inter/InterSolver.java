package toyc.analysis.dataflow.inter;

import toyc.analysis.dataflow.fact.DataflowResult;
import toyc.analysis.graph.icfg.ICFG;
import toyc.util.collection.SetQueue;

import java.util.Queue;

/**
 * Solver for inter-procedural data-flow analysis.
 * The workload of inter-procedural analysis is heavy, thus we always
 * adopt work-list algorithm for efficiency.
 */
class InterSolver<Function, Node, Fact> {

    private final InterDataflowAnalysis<Node, Fact> analysis;

    private final ICFG<Function, Node> icfg;

    private DataflowResult<Node, Fact> result;

    private Queue<Node> workList;

    InterSolver(InterDataflowAnalysis<Node, Fact> analysis,
                ICFG<Function, Node> icfg) {
        this.analysis = analysis;
        this.icfg = icfg;
    }

    DataflowResult<Node, Fact> solve() {
        result = new DataflowResult<>();
        initialize();
        doSolve();
        return result;
    }

    private void initialize() {
        Node entryNode = icfg.getEntryOf(icfg.entryFunction());
        result.setInFact(entryNode, analysis.newBoundaryFact(entryNode));
        result.setOutFact(entryNode, analysis.newBoundaryFact(entryNode));
        icfg.forEach(node -> {
            if (node.equals(entryNode)) {
                return;
            }
            result.setInFact(node, analysis.newInitialFact());
            result.setOutFact(node, analysis.newInitialFact());
        });
    }

    private void doSolve() {
        workList = new SetQueue<>();
        icfg.forEach(workList::add);
        while (!workList.isEmpty()) {
            Node node = workList.poll();
            // meet incoming facts
            Fact in = result.getInFact(node);
            icfg.getInEdgesOf(node).forEach(inEdge -> {
                Fact predOut = result.getOutFact(inEdge.source());
                analysis.meetInto(analysis.transferEdge(inEdge, predOut), in);
            });
            Fact out = result.getOutFact(node);
            boolean changed = analysis.transferNode(node, in, out);
            if (changed) {
                propagate(node);
            }
        }
    }

    void propagate(Node node) {
        workList.addAll(icfg.getSuccsOf(node));
    }

    Fact getOutFact(Node node) {
        return result.getOutFact(node);
    }
}
