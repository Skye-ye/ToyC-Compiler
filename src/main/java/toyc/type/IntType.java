package toyc.type;

public class IntType extends Type {
    private static final IntType instance = new IntType();

    private IntType() {}

    public static IntType getIntType() {
        return instance;
    }

    @Override
    public String toString() {
        return "int";
    }
}
