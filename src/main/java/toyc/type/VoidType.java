package toyc.type;

public class VoidType extends Type {
    private static final VoidType instance = new VoidType();

    private VoidType() {}

    public static VoidType getVoidType() {
        return instance;
    }

    @Override
    public String toString() {
        return "void";
    }
}
