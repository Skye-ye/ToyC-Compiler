package toyc.algorithm.analysis.dataflow.analysis.constprop;

import toyc.ir.exp.*;
import toyc.util.AnalysisException;

/**
 * Evaluates expressions in constant propagation. Since this functionality
 * is used not only by {@link ConstantPropagation} but also other classes,
 * we implement it as static methods to make it easily accessible.
 */
public final class Evaluator {

    private Evaluator() {
    }

    /**
     * Evaluates the {@link Value} of given expression.
     *
     * @param exp the expression to be evaluated
     * @param in  IN fact of the statement
     * @return the resulting {@link Value}
     */
    public static Value evaluate(Exp exp, CPFact in) {
        if (exp instanceof IntLiteral) {
            return Value.makeConstant(((IntLiteral) exp).getValue());
        } else if (exp instanceof Var var) {
            return in.get(var);
        } else if (exp instanceof UnaryExp unary) {
            Value v = evaluate(unary.getOperand(), in);
            if (v.isConstant()) {
                if (unary instanceof NegExp) {
                    return Value.makeConstant(-v.getConstant());
                } else if (unary instanceof NotExp) {
                    if (v.getConstant() != 0) {
                        return Value.makeConstant(0);
                    } else {
                        return Value.makeConstant(1);
                    }
                }
            } else if (v.isNAC()) {
                return Value.getNAC();
            } else {
                return Value.getUndef();
            }
        } else if (exp instanceof BinaryExp binary) {
            BinaryExp.Op op = binary.getOperator();
            Value v1 = evaluate(binary.getOperand1(), in);
            Value v2 = evaluate(binary.getOperand2(), in);
            // handle division-by-zero by returning UNDEF
            if ((op == ArithmeticExp.Op.DIV || op == ArithmeticExp.Op.REM) &&
                    v2.isConstant() && v2.getConstant() == 0) {
                return Value.getUndef();
            }
            if (v1.isConstant() && v2.isConstant()) {
                int i1 = v1.getConstant();
                int i2 = v2.getConstant();
                return Value.makeConstant(evaluate(op, i1, i2));
            }
            // handle zero * NAC by returning 0
            if (op == ArithmeticExp.Op.MUL
                    && (v1.isConstant() && v1.getConstant() == 0 && v2.isNAC() || // 0 * NAC
                    v2.isConstant() && v2.getConstant() == 0 && v1.isNAC())) { // NAC * 0
                return Value.makeConstant(0);
            }
            if (v1.isNAC() || v2.isNAC()) {
                return Value.getNAC();
            }
            return Value.getUndef();
        }
        // return NAC for other cases
        return Value.getNAC();
    }

    private static int evaluate(BinaryExp.Op op, int i1, int i2) {
        if (op instanceof ArithmeticExp.Op) {
            return switch ((ArithmeticExp.Op) op) {
                case ADD -> i1 + i2;
                case SUB -> i1 - i2;
                case MUL -> i1 * i2;
                case DIV -> i1 / i2;
                case REM -> i1 % i2;
            };
        } else if (op instanceof ConditionExp.Op) {
            return switch ((ConditionExp.Op) op) {
                case EQ -> i1 == i2 ? 1 : 0;
                case NE -> i1 != i2 ? 1 : 0;
                case LT -> i1 < i2 ? 1 : 0;
                case GT -> i1 > i2 ? 1 : 0;
                case LE -> i1 <= i2 ? 1 : 0;
                case GE -> i1 >= i2 ? 1 : 0;
            };
        }
        throw new AnalysisException("Unexpected op: " + op);
    }
}
