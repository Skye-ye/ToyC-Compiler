package toyc.codegen.regalloc;

public class LocalDataLocation {
    public enum LocationType {
        REGISTER,
        STACK
    }

    private final LocationType type;
    private final String register;    // useful when type is REGISTER
    private final int offset;         // useful when type is STACK

    private LocalDataLocation(LocationType type, String register, int offset) {
        this.type = type;
        this.register = register;
        this.offset = offset;
    }

    public static LocalDataLocation createRegister(String register) {
        return new LocalDataLocation(LocationType.REGISTER, register, 0);
    }

    public static LocalDataLocation createStack(int offset) {
        return new LocalDataLocation(LocationType.STACK, null, offset);
    }

    public LocationType getType() {
        return type;
    }

    public String getRegister() {
        if (type != LocationType.REGISTER) {
            throw new IllegalStateException("Location is not a register");
        }
        return register;
    }

    public int getOffset() {
        if (type != LocationType.STACK) {
            throw new IllegalStateException("Location is not a stack offset");
        }
        return offset;
    }
}