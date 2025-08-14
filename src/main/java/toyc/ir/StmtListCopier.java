package toyc.ir;

import toyc.ir.exp.*;
import toyc.ir.stmt.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Standalone utility class for copying statement lists with proper
 * variable consistency and reference handling.
 */
public class StmtListCopier {

    /**
     * Copy a list of statements with consistent variable mapping.
     * This is the main method for copying statements.
     *
     * @param originalStmts the statements to copy
     * @param originalVars  the original variables used in the statements
     * @return copied statements with consistent variable references
     */
    public static List<Stmt> copy(List<Stmt> originalStmts,
                                  List<Var> originalVars) {
        if (originalStmts.isEmpty()) {
            return new ArrayList<>();
        }

        // create variable mapping
        Map<Var, Var> varMapping = createVariableMapping(originalVars);
        StmtCopier copier = new StmtCopier(varMapping);

        // clone statements with variable mapping
        List<Stmt> clonedStmts = new ArrayList<>();
        Map<Stmt, Stmt> stmtMapping = new HashMap<>();

        for (Stmt stmt : originalStmts) {
            Stmt clonedStmt = stmt.accept(copier);
            clonedStmts.add(clonedStmt);
            clonedStmt.setIndex(stmt.getIndex());
            clonedStmt.setLineNumber(stmt.getLineNumber());
            stmtMapping.put(stmt, clonedStmt);
        }

        // fix statement references (If targets, Goto targets, etc.)
        for (Stmt stmt : clonedStmts) {
            if (stmt instanceof If ifStmt) {
                Stmt originalTarget = ifStmt.getTarget();
                ifStmt.setTarget(stmtMapping.get(originalTarget));
            } else if (stmt instanceof Goto goStmt) {
                Stmt originalTarget = goStmt.getTarget();
                goStmt.setTarget(stmtMapping.get(originalTarget));
            }
        }

        return clonedStmts;
    }

    /**
     * Extract all variables from statements and create mapping to cloned variables
     */
    private static Map<Var, Var> createVariableMapping(List<Var> originalVars) {
        // Create mapping from original to cloned variables
        Map<Var, Var> varMapping = new HashMap<>();
        for (Var originalVar : originalVars) {
            Var clonedVar = new Var(
                    originalVar.getFunction(),
                    originalVar.getName(),
                    originalVar.getType(),
                    originalVar.getIndex(),
                    originalVar.isConst() ? originalVar.getConstValue() : null);
            varMapping.put(originalVar, clonedVar);
        }

        return varMapping;
    }

    private static class StmtCopier implements StmtVisitor<Stmt> {

        private final Map<Var, Var> varMapping;

        ExpCopier expCopier;

        public StmtCopier(Map<Var, Var> varMapping) {
            this.varMapping = varMapping;
            expCopier = new ExpCopier(varMapping);
        }

        @Override
        public Stmt visit(AssignLiteral stmt) {
            return new AssignLiteral(
                    varMapping.get(stmt.getLValue()), stmt.getRValue());
        }

        @Override
        public Stmt visit(Copy stmt) {
            return new Copy(varMapping.get(stmt.getLValue()),
                    varMapping.get(stmt.getRValue()));
        }

        @Override
        public Stmt visit(Binary stmt) {
            return new Binary(varMapping.get(stmt.getLValue()),
                    (BinaryExp) stmt.getRValue().accept(expCopier));
        }

        @Override
        public Stmt visit(Goto stmt) {
            Goto clonedGoto = new Goto();
            clonedGoto.setTarget(stmt.getTarget()); // target will be fixed later
            return clonedGoto;
        }

        @Override
        public Stmt visit(Unary stmt) {
            return new Unary(varMapping.get(stmt.getLValue()),
                    (UnaryExp) stmt.getRValue().accept(expCopier));
        }

        @Override
        public Stmt visit(If stmt) {
            If clonedIf = new If((ConditionExp) stmt.getCondition().accept(expCopier));
            clonedIf.setTarget(stmt.getTarget()); // target will be fixed later
            return clonedIf;
        }

        @Override
        public Stmt visit(Call stmt) {
            return new Call(
                    stmt.getContainer(),
                    (CallExp) stmt.getCallExp().accept(expCopier),
                    stmt.getResult() == null ? null : varMapping.get(stmt.getResult()));
        }

        @Override
        public Stmt visit(Return stmt) {
            Var result = stmt.getValue();
            if (result != null) {
                result = varMapping.get(result);
            }
            return new Return(result);
        }

        @Override
        public Stmt visit(Nop stmt) {
            return new Nop();
        }

        @Override
        public Stmt visitDefault(Stmt stmt) {
            throw new UnsupportedOperationException("Unsupported statement type: " + stmt.getClass().getName());
        }

        private record ExpCopier(
                Map<Var, Var> varMapping) implements ExpVisitor<Exp> {

            @Override
            public Exp visit(CallExp invoke) {
                List<Var> originalVars = invoke.getArgs();
                List<Var> clonedVars = new ArrayList<>();
                for (Var originalVar : originalVars) {
                    clonedVars.add(varMapping.get(originalVar));
                }
                return new CallExp(invoke.getFunction(), clonedVars);
            }

            @Override
            public Exp visit(NegExp exp) {
                return new NegExp(varMapping.get(exp.getOperand()));
            }

            @Override
            public Exp visit(NotExp exp) {
                return new NotExp(varMapping.get(exp.getOperand()));
            }

            @Override
            public Exp visit(ArithmeticExp exp) {
                return new ArithmeticExp(
                        exp.getOperator(),
                        varMapping.get(exp.getOperand1()),
                        varMapping.get(exp.getOperand2()));
            }

            @Override
            public Exp visit(ConditionExp exp) {
                return new ConditionExp(
                        exp.getOperator(),
                        varMapping.get(exp.getOperand1()),
                        varMapping.get(exp.getOperand2()));
            }

            @Override
            public Exp visitDefault(Exp exp) {
                throw new UnsupportedOperationException("Unsupported " +
                        "expression type: " + exp.getClass().getName());
            }
        }
    }
}