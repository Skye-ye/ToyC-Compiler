package toyc.analysis.dataflow.analysis.constprop;

import toyc.util.AnalysisException;

/**
 * Represents lattice values in constant propagation.
 * A value can be either UNDEF, a constant, or NAC.
 */
public class Value {

    /**
     * The object representing UNDEF.
     */
    private static final Value UNDEF = new Value(Kind.UNDEF);

    /**
     * The object representing NAC.
     */
    private static final Value NAC = new Value(Kind.NAC);

    /**
     * Cache frequently used values for saving space.
     */
    private static final Value[] cache = new Value[128 + 127 + 1];

    static {
        for (int i = 0; i < cache.length; i++) {
            cache[i] = new Value(i - 128);
        }
    }

    private final Kind kind;

    private final int value;

    private Value(Kind kind) {
        this.kind = kind;
        this.value = 0;
    }

    private Value(int value) {
        this.kind = Kind.CONSTANT;
        this.value = value;
    }

    /**
     * @return the UNDEF.
     */
    public static Value getUndef() {
        return UNDEF;
    }

    /**
     * Makes a constant value.
     *
     * @return the constant for given value.
     */
    public static Value makeConstant(int value) {
        final int offset = 128;
        if (value >= -128 && value <= 127) { // will cache
            return cache[value + offset];
        }
        return new Value(value);
    }

    /**
     * @return the NAC.
     */
    public static Value getNAC() {
        return NAC;
    }

    /**
     * @return true if this value is UNDEF, otherwise false.
     */
    public boolean isUndef() {
        return kind == Kind.UNDEF;
    }

    /**
     * @return true if this value represents a constant, otherwise false.
     */
    public boolean isConstant() {
        return kind == Kind.CONSTANT;
    }

    /**
     * @return true if this value is NAC, otherwise false.
     */
    public boolean isNAC() {
        return kind == Kind.NAC;
    }

    /**
     * If this value represents a (integer) constant, then returns the integer.
     * The client code should call {@link #isConstant()} to check if this Value
     * is constant before calling this method.
     *
     * @throws AnalysisException if this value is not a constant
     */
    public int getConstant() {
        if (!isConstant()) {
            throw new AnalysisException(this + " is not a constant");
        }
        return value;
    }

    @Override
    public int hashCode() {
        return value;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof Value)) {
            return false;
        }
        Value other = (Value) obj;
        return kind == other.kind
                && value == other.value;
    }

    @Override
    public String toString() {
        return switch (kind) {
            case UNDEF -> "UNDEF";
            case CONSTANT -> Integer.toString(value);
            case NAC -> "NAC";
        };
    }

    private enum Kind {
        UNDEF, // undefined value
        CONSTANT, // an integer constant
        NAC, // not a constant
    }
}
