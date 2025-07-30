package toyc.algorithm.analysis.dataflow.analysis.csd;

import toyc.algorithm.analysis.dataflow.analysis.AbstractDataflowAnalysis;
import toyc.algorithm.analysis.dataflow.analysis.AnalysisDriver;
import toyc.algorithm.analysis.graph.cfg.CFG;
import toyc.config.AlgorithmConfig;
import toyc.ir.exp.Exp;
import toyc.ir.exp.Var;
import toyc.ir.stmt.Stmt;

import java.util.Set;

/**
 * implementation of common subexpression elimination analysis (global).
 */
public class CommonSubexpressionDetection extends AnalysisDriver<Stmt, CSEFact> {

    public static final String ID = "common-subexp-detection";

    public CommonSubexpressionDetection(AlgorithmConfig config) {
        super(config);
    }

    @Override
    protected Analysis makeAnalysis(CFG<Stmt> cfg) {
        return new Analysis(cfg);
    }

    public static class Analysis extends AbstractDataflowAnalysis<Stmt, CSEFact> {

        public Analysis(CFG<Stmt> cfg) {
            super(cfg);
        }

        @Override
        public boolean isForward() {
            return true; // CSE is a forward analysis
        }

        @Override
        public CSEFact newBoundaryFact() {
            return newInitialFact();
        }

        @Override
        public CSEFact newInitialFact() {
            return new CSEFact();
        }

        @Override
        public void meetInto(CSEFact fact, CSEFact target) {
            if (target.isEmpty()) {
                target.copyFrom(fact);
                return ;
            }

            CSEFact intersection = new CSEFact();
            fact.forEach((exp, id)-> {
                if (target.contains(exp)) {
                    intersection.update(exp, id);
                }
            });
            target.copyFrom(intersection);
        }

        @Override
        public boolean transferNode(Stmt stmt, CSEFact in, CSEFact out) {
            boolean changed = false;
            changed |= out.copyFrom(in);

            // Gen
            Set<Exp> subexpressions = RExpExtractor.extract(stmt);
            if (!subexpressions.isEmpty()) {
                for (Exp exp : subexpressions) {
                    if (in.contains(exp)) {
                        continue;
                    }
                    // Add the new subexpression to the out fact
                    changed |= out.update(exp, stmt.getIndex());
                }
            }

            // Kill
            Var lvalue = LExpExtractor.extract(stmt);
            if (lvalue != null) {
                changed |= out.eliminate(lvalue);
            }
            return changed;
        }

    }
}