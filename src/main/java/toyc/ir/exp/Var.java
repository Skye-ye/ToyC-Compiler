package toyc.ir.exp;

import toyc.language.Function;
import toyc.language.type.Type;
import toyc.util.AnalysisException;
import toyc.util.Indexable;

import javax.annotation.Nullable;

/**
 * Representation of function parameters, and local variables.
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

    /**
     * If this variable is a (temporary) variable generated for holding
     * a constant value, then this field holds that constant value;
     * otherwise, this field is null.
     */
    private final Literal constValue;

    public Var(Function function, String name, Type type, int index) {
        this(function, name, type, index, null);
    }

    public Var(Function function, String name, Type type, int index,
               @Nullable Literal constValue) {
        this.function = function;
        this.name = name;
        this.type = type;
        this.index = index;
        this.constValue = constValue;
    }

    /**
     * @return the method containing this Var.
     */
    public Function getFunction() {
        return function;
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

    /**
     * @return true if this variable is a (temporary) variable
     * generated for holding constant value, otherwise false.
     */
    public boolean isConst() {
        return constValue != null;
    }

    /**
     * @return the constant value held by this variable.
     * @throws AnalysisException if this variable does not hold const value
     */
    public Literal getConstValue() {
        if (!isConst()) {
            throw new AnalysisException(this
                    + " is not a (temporary) variable for holding const value");
        }
        return constValue;
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
