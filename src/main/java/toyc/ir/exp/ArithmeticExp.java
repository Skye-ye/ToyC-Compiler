package toyc.ir.exp;
import toyc.util.Hashes;
import toyc.language.type.IntType;

/**
 * Representation of arithmetic expression, e.g., a + b.
 */
public class ArithmeticExp extends AbstractBinaryExp {

    public enum Op implements BinaryExp.Op {

        ADD("+"),
        SUB("-"),
        MUL("*"),
        DIV("/"),
        REM("%"),
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

    public ArithmeticExp(Op op, Var value1, Var value2) {
        super(value1, value2);
        this.op = op;
    }

    @Override
    protected void validate() {
        assert operand1.getType() instanceof IntType
                && operand2.getType() instanceof IntType;
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

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ArithmeticExp other)) {
            return false;
        }

        // operator
        if (this.op != other.op) {
            return false;
        }

        // 对于加法和乘法，满足交换律
        if (this.op == Op.ADD || this.op == Op.MUL) {
            return (operand1.equals(other.operand1) && operand2.equals(other.operand2)) ||
                (operand1.equals(other.operand2) && operand2.equals(other.operand1));
        }
        
        return operand1.equals(other.operand1) && operand2.equals(other.operand2);
    }

    @Override
    public int hashCode() {
        // 对于加法和乘法，满足交换律，使用对称的hash计算
        if (this.op == Op.ADD || this.op == Op.MUL) {
            return op.hashCode() + operand1.hashCode() + operand2.hashCode();
        } else {
            // 对于减法、除法、取余，不满足交换律，考虑操作数顺序
            return Hashes.hash(op, operand1, operand2);
        }
    }
}
