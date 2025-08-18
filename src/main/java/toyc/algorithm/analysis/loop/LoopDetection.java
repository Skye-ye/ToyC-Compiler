package toyc.algorithm.analysis.loop;

import toyc.algorithm.analysis.FunctionAnalysis;
import toyc.algorithm.analysis.dataflow.analysis.DominatorAnalysis;
import toyc.algorithm.analysis.dataflow.fact.DataflowResult;
import toyc.algorithm.analysis.dataflow.fact.SetFact;
import toyc.algorithm.analysis.graph.cfg.CFG;
import toyc.algorithm.analysis.graph.cfg.CFGBuilder;
import toyc.algorithm.analysis.graph.cfg.CFGEdge;
import toyc.config.AlgorithmConfig;
import toyc.ir.IR;
import toyc.ir.stmt.Goto;
import toyc.ir.stmt.If;
import toyc.ir.stmt.Stmt;
import toyc.util.collection.Sets;

import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class LoopDetection extends FunctionAnalysis<Set<Loop>> {
    public static final String ID = "loop-detection";

    private IR ir;

    private CFG<Stmt> cfg;

    private DataflowResult<Stmt, SetFact<Stmt>> dominators;

    public LoopDetection(AlgorithmConfig config) {
        super(config);
    }

    @Override
    public Set<Loop> analyze(IR ir) {
        this.ir = ir;
        cfg = ir.getResult(CFGBuilder.ID);
        dominators = ir.getResult(DominatorAnalysis.ID);

        Set<CFGEdge<Stmt>> backEdges = findBackEdges();

        // Group back edges by header and select max tail for each header
        Map<Stmt, CFGEdge<Stmt>> headerToMaxBackEdge = backEdges.stream()
                .collect(Collectors.toMap(
                        CFGEdge::target,
                        edge -> edge,
                        (edge1, edge2) -> {
                            int index1 = edge1.source().getIndex();
                            int index2 = edge2.source().getIndex();
                            return index1 >= index2 ? edge1 : edge2;
                        }
                ));

        // Construct loops using representative back edges
        return headerToMaxBackEdge.values().stream()
                .map(this::constructNaturalLoop)
                .collect(Collectors.toSet());
    }

    /**
     * Find back edges: edges from node N to node H where H dominates N
     */
    private Set<CFGEdge<Stmt>> findBackEdges() {
        Set<CFGEdge<Stmt>> backEdges = Sets.newSet();

        for (Stmt stmt : cfg.getNodes()) {
            for (CFGEdge<Stmt> edge : cfg.getOutEdgesOf(stmt)) {
                Stmt source = edge.source();
                Stmt target = edge.target();

                // Check if target dominates source
                if (dominates(target, source)) {
                    backEdges.add(edge);
                }
            }
        }

        return backEdges;
    }

    /**
     * Check if dominator dominates node using dominator analysis results
     */
    private boolean dominates(Stmt dominator, Stmt node) {
        if (dominator.equals(node)) {
            return true;
        }

        SetFact<Stmt> nodeDominators = dominators.getResult(node);
        return nodeDominators != null && nodeDominators.contains(dominator);
    }

    /**
     * Construct natural loop for a back edge (tail -> header)
     */
    private Loop constructNaturalLoop(CFGEdge<Stmt> backEdge) {
        Stmt header = backEdge.target();  // Loop header
        Set<Stmt> tails = Sets.newSet();

        Set<Stmt> loopBody =
                Sets.newOrderedSet(Comparator.comparing(Stmt::getIndex));

        int headerIndex = header.getIndex();
        int tailIndex = backEdge.source().getIndex();

        for (int i = headerIndex; i <= tailIndex; i++) {
            Stmt stmt = ir.getStmt(i);
            loopBody.add(stmt);
            if (stmt instanceof If ifStmt && ifStmt.getTarget() == header) {
                tails.add(stmt);
            } else if (stmt instanceof Goto gotoStmt && gotoStmt.getTarget() == header) {
                tails.add(stmt);
            }
        }

        return new Loop(header, tails, loopBody);
    }
}
