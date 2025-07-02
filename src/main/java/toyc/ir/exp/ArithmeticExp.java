package toyc.ir.exp;

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
}
