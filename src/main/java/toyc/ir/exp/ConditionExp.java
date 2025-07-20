package toyc.ir.exp;

import toyc.language.type.IntType;

/**
 * Representation of condition expression, e.g., a == b.
 */
public class ConditionExp extends AbstractBinaryExp {

    public enum Op implements BinaryExp.Op {

        EQ("=="),
        NE("!="),
        LT("<"),
        GT(">"),
        LE("<="),
        GE(">="),
        ;

        private final String symbol;

        Op(String symbol) {
            this.symbol = symbol;
        }

        @Override
        public String toString() {
            return symbol;
        }
    }

    private final Op op;

    public ConditionExp(Op op, Var value1, Var value2) {
        super(value1, value2);
        this.op = op;
    }

    @Override
    protected void validate() {
        assert operand1.getType() == IntType.INT
                && operand2.getType() == IntType.INT;
    }

    @Override
    public Op getOperator() {
        return op;
    }

    @Override
    public IntType getType() {
        return IntType.INT;
    }

    @Override
    public <T> T accept(ExpVisitor<T> visitor) {
        return visitor.visit(this);
    }

    // equals override
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ConditionExp)) {
            return false;
        }
        ConditionExp other = (ConditionExp) obj;

        if (this.op != other.op) {
            return false;
        }

        if (this.op == Op.EQ || this.op == Op.NE) {
            return (operand1.equals(other.operand1) && operand2.equals(other.operand2)) ||
                (operand1.equals(other.operand2) && operand2.equals(other.operand1));
        }

        return operand1.equals(other.operand1) && operand2.equals(other.operand2);
    }

    // 为了保持equals和hashCode的一致性，需要重写hashCode
    @Override
    public int hashCode() {
        if (op == Op.EQ || op == Op.NE) {
            return op.hashCode() + operand1.hashCode() + operand2.hashCode();
        } else {
            return op.hashCode() * 31 * 31 + operand1.hashCode() * 31 + operand2.hashCode();
        }
    }
}
