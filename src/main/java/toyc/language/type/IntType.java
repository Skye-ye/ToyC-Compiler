package toyc.language.type;

public enum IntType implements Type {

    INT;

    @Override
    public String getName() {
        return "int";
    }

    @Override
    public String toString() {
        return getName();
    }
}
