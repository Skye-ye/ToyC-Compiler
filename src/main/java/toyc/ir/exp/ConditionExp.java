package toyc.ir.exp;

import toyc.language.type.IntType;
import toyc.util.Hashes;

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
        if (!(obj instanceof ConditionExp other)) {
            return false;
        }

        if((this.op == Op.LT && other.op == Op.GT)||
           (this.op == Op.GT && other.op == Op.LT) ||
           (this.op == Op.LE && other.op == Op.GE) ||
           (this.op == Op.GE && other.op == Op.LE)) {
            return operand1.equals(other.operand2) && operand2.equals(other.operand1);
        }

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
        // 对于对称操作符对 (LT<->GT, LE<->GE)，需要产生相同的哈希码
        if ((op == Op.LT) || (op == Op.GT)) {
            // 使用统一的哈希计算，确保 a < b 和 b > a 产生相同结果
            int opHash = Op.LT.hashCode() + Op.GT.hashCode();
            return opHash + operand1.hashCode() + operand2.hashCode();
        }
        
        if ((op == Op.LE) || (op == Op.GE)) {
            // 使用统一的哈希计算，确保 a <= b 和 b >= a 产生相同结果
            int opHash = Op.LE.hashCode() + Op.GE.hashCode();
            return opHash + operand1.hashCode() + operand2.hashCode();
        }
        
        // 对于等于和不等于操作符，满足交换律
        if (op == Op.EQ || op == Op.NE) {
            return op.hashCode() + operand1.hashCode() + operand2.hashCode();
        }
        
        // 对于其他情况，考虑操作数顺序
        return Hashes.hash(op, operand1, operand2);
    }
}
