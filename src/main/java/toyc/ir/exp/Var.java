package toyc.ir.exp;

import toyc.language.Function;
import toyc.language.type.Type;
import toyc.util.Indexable;

/**
 * Representation of method/constructor parameters, lambda parameters,
 * exception parameters, and local variables.
 */
public class Var implements LValue, RValue, Indexable {

    /**
     * The function that contains this Var.
     */
    private final Function function;

    /**
     * The name of this Var.
     */
    private final String name;

    /**
     * The type of this Var.
     */
    private final Type type;

    /**
     * The index of this variable in {@link #function}.
     */
    private final int index;

    public Var(Function function, String name, Type type, int index) {
        this.function = function;
        this.name = name;
        this.type = type;
        this.index = index;
    }

    /**
     * @return the index of this variable in the container IR.
     */
    @Override
    public int getIndex() {
        return index;
    }

    /**
     * @return name of this Var.
     */
    public String getName() {
        return name;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public <T> T accept(ExpVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return name;
    }
}
