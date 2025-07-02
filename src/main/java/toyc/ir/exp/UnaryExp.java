package toyc.ir.exp;

import toyc.language.type.IntType;

import java.util.Set;

/**
 * Representation of unary expression.
 */
public interface UnaryExp extends RValue {

    Var getOperand();

    @Override
    default Set<RValue> getUses() {
        return Set.of(getOperand());
    }

    @Override
    IntType getType();
}
