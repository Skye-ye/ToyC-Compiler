package toyc.analysis.dataflow.analysis.cse;

import toyc.analysis.dataflow.analysis.AbstractDataflowAnalysis;
import toyc.analysis.dataflow.analysis.AnalysisDriver;
import toyc.analysis.graph.cfg.CFG;
import toyc.analysis.graph.cfg.CFGEdge;
import toyc.config.AnalysisConfig;
import toyc.ir.IR;
import toyc.ir.exp.ConditionExp;
import toyc.ir.exp.Exp;
import toyc.ir.exp.Var;
import toyc.ir.stmt.DefinitionStmt;
import toyc.ir.stmt.If;
import toyc.ir.stmt.Stmt;

import java.util.Set;

/**
 * implementation of common subexpression elimination analysis (global).
 */
public class CommonSubexpressionElimination extends AnalysisDriver<Stmt, CSEFact> {

    public static final String ID = "common-subexp-eliminate";

    public CommonSubexpressionElimination(AnalysisConfig config) {
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
            return newBoundaryFact(cfg.getIR());
        }

        public CSEFact newBoundaryFact(IR ir) {
            CSEFact entryFact = newInitialFact();
            Stmt statement = ir.getStmt(0);
            Set<Exp> subexpression = RExpExtractor.extract(statement);
            if (subexpression == null || subexpression.isEmpty()) {
                return entryFact; // No subexpressions to initialize
            }
            for (Exp exp : subexpression) {
                entryFact.update(exp, 0);
            }
            return entryFact;
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
        }

        @Override
        public boolean transferNode(Stmt stmt, CSEFact in, CSEFact out) {
            Boolean changed = false;
            changed |= out.copyFrom(in);

            // Gen
            Set<Exp> subexpressions = RExpExtractor.extract(stmt);
            if (!(subexpressions.isEmpty() || subexpressions == null)) {
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
                return changed;
            }
            else {
                changed |= out.eliminate(lvalue);
            }
            return changed;
        }

    }
}