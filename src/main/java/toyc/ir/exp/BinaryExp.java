package toyc.ir.exp;

import toyc.language.type.IntType;

/**
 * Representation of binary expression.
 */
public interface BinaryExp extends RValue {

    /**
     * Representation of binary operators.
     */
    interface Op {
    }

    /**
     * @return the operator.
     */
    Op getOperator();

    /**
     * @return the first operand.
     */
    Var getOperand1();

    /**
     * @return the second operand.
     */
    Var getOperand2();

    @Override
    IntType getType();
}
