package toyc.type;

import java.util.List;

public class FunctionType extends Type {
    private final Type returnType;
    private final List<Type> arguments;

    public FunctionType(Type returnType, List<Type> arguments) {
        this.returnType = returnType;
        this.arguments = arguments;
    }

    public boolean checkArguments(List<Type> arguments) {
        if (this.arguments.size() != arguments.size()) {
            return false;
        }
        for (int i = 0; i < this.arguments.size(); i++) {
            if (!this.arguments.get(i).equals(arguments.get(i))) {
                return false;
            }
        }
        return true;
    }

    public Type getReturnType() {
        return returnType;
    }

    public int getArity() {
        return arguments.size();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        for (int i = 0; i < arguments.size(); i++) {
            sb.append(arguments.get(i));
            if (i < arguments.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append(") -> ");
        sb.append(returnType);
        return sb.toString();
    }
}
