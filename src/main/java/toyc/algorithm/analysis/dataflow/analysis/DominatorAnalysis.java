package toyc.algorithm.analysis.dataflow.analysis;

import toyc.algorithm.analysis.dataflow.fact.SetFact;
import toyc.algorithm.analysis.graph.cfg.CFG;
import toyc.config.AlgorithmConfig;
import toyc.ir.stmt.Stmt;

public class DominatorAnalysis extends AnalysisDriver<Stmt, SetFact<Stmt>> {

    public static final String ID = "dominator";

    public DominatorAnalysis(AlgorithmConfig config) {
        super(config);
    }

    @Override
    protected DataflowAnalysis<Stmt, SetFact<Stmt>> makeAnalysis(CFG<Stmt> cfg) {
        return new Analysis(cfg);
    }

    private static class Analysis extends AbstractDataflowAnalysis<Stmt,
            SetFact<Stmt>> {

        private Analysis(CFG<Stmt> cfg) {
            super(cfg);
        }

        @Override
        public boolean isForward() {
            return true;
        }

        @Override
        public SetFact<Stmt> newBoundaryFact() {
            SetFact<Stmt> entryDom = new SetFact<>();
            entryDom.add(cfg.getEntry());
            return entryDom;
        }

        @Override
        public SetFact<Stmt> newInitialFact() {
            SetFact<Stmt> allNodes = new SetFact<>();
            cfg.getIR().stmts().forEach(allNodes::add);
            return allNodes;
        }

        @Override
        public void meetInto(SetFact<Stmt> fact, SetFact<Stmt> target) {
            target.intersect(fact);
        }

        @Override
        public boolean transferNode(Stmt stmt, SetFact<Stmt> in, SetFact<Stmt> out) {
            // Transfer function: Dom(n) = {n} ∪ Dom_in(n)
            SetFact<Stmt> oldOut = out.copy();

            out.clear();
            out.union(in);      // Copy input dominators
            out.add(stmt);      // Every node dominates itself

            return !out.equals(oldOut);
        }
    }
}
