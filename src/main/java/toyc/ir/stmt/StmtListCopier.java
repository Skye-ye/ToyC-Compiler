package toyc.ir.stmt;

import toyc.ir.exp.*;
import toyc.language.Function;

import javax.annotation.Nullable;
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
     * @param varMapping the mapping from original variables to new variables
     * @param targetFunction the target function for copied stmts
     * @return copied statements with consistent variable references
     */
    public static List<Stmt> copy(List<Stmt> originalStmts,
                                  Map<Var, Var> varMapping,
                                  @Nullable Function targetFunction) {
        if (originalStmts.isEmpty()) {
            return new ArrayList<>();
        }

        // create variable mapping
        StmtCopier copier = new StmtCopier(varMapping, targetFunction);

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
            if (stmt instanceof JumpStmt jumpStmt) {
                Stmt originalTarget = jumpStmt.getTarget();
                jumpStmt.setTarget(stmtMapping.getOrDefault(originalTarget, originalTarget));
            }
        }

        return clonedStmts;
    }

    private static class StmtCopier implements StmtVisitor<Stmt> {

        private final Map<Var, Var> varMapping;

        private final Function targetFunction;

        ExpCopier expCopier;

        public StmtCopier(Map<Var, Var> varMapping, Function targetFunction) {
            this.varMapping = varMapping;
            this.targetFunction = targetFunction;
            expCopier = new ExpCopier(varMapping);
        }

        @Override
        public Stmt visit(AssignLiteral stmt) {
            Var lValue = stmt.getLValue();
            return new AssignLiteral(
                    varMapping.getOrDefault(lValue, lValue),
                    stmt.getRValue());
        }

        @Override
        public Stmt visit(Copy stmt) {
            Var lValue = stmt.getLValue();
            Var rValue = stmt.getRValue();
            return new Copy(varMapping.getOrDefault(lValue, lValue),
                    varMapping.getOrDefault(rValue, rValue));
        }

        @Override
        public Stmt visit(Binary stmt) {
            Var lValue = stmt.getLValue();
            return new Binary(varMapping.getOrDefault(lValue, lValue),
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
            Var lValue = stmt.getLValue();
            return new Unary(varMapping.getOrDefault(lValue, lValue),
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
            Var result = stmt.getResult();
            return new Call(
                    targetFunction != null ? targetFunction :
                            stmt.getContainer(),
                    (CallExp) stmt.getCallExp().accept(expCopier),
                    result == null ? null : varMapping.getOrDefault(result, result));
        }

        @Override
        public Stmt visit(Return stmt) {
            Var result = stmt.getValue();
            if (result != null) {
                result = varMapping.getOrDefault(result, result);
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
                    clonedVars.add(varMapping.getOrDefault(originalVar, originalVar));
                }
                return new CallExp(invoke.getFunction(), clonedVars);
            }

            @Override
            public Exp visit(NegExp exp) {
                Var operand = exp.getOperand();
                return new NegExp(varMapping.getOrDefault(operand, operand));
            }

            @Override
            public Exp visit(NotExp exp) {
                Var operand = exp.getOperand();
                return new NotExp(varMapping.getOrDefault(operand, operand));
            }

            @Override
            public Exp visit(ArithmeticExp exp) {
                Var operand1 = exp.getOperand1();
                Var operand2 = exp.getOperand2();
                return new ArithmeticExp(
                        exp.getOperator(),
                        varMapping.getOrDefault(operand1, operand1),
                        varMapping.getOrDefault(operand2, operand2));
            }

            @Override
            public Exp visit(ConditionExp exp) {
                Var operand1 = exp.getOperand1();
                Var operand2 = exp.getOperand2();
                return new ConditionExp(
                        exp.getOperator(),
                        varMapping.getOrDefault(operand1, operand1),
                        varMapping.getOrDefault(operand2, operand2));
            }

            @Override
            public Exp visitDefault(Exp exp) {
                throw new UnsupportedOperationException("Unsupported " +
                        "expression type: " + exp.getClass().getName());
            }
        }
    }
}