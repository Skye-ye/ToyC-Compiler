package toyc.ir.exp;


import toyc.language.type.IntType;

import javax.annotation.Nonnull;

/**
 * Representation of logical NOT expression, e.g., !o;
 */
public record NotExp(Var value) implements UnaryExp {

    public NotExp(Var value) {
        this.value = value;
        assert value.getType() instanceof IntType;
    }

    @Override
    public Var getOperand() {
        return value;
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
    @Nonnull
    public String toString() {
        return "!" + value;
    }
}
