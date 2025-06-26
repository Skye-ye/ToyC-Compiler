package toyc.type;

import java.util.List;

public class FunctionType extends Type {
    private final Type returnType;
    private final List<Type> parameterTypes;

    public FunctionType(Type returnType, List<Type> parameterTypes) {
        this.returnType = returnType;
        this.parameterTypes = parameterTypes;
    }

    public boolean checkArguments(List<Type> arguments) {
        if (this.parameterTypes.size() != arguments.size()) {
            return false;
        }
        for (int i = 0; i < this.parameterTypes.size(); i++) {
            if (!this.parameterTypes.get(i).equals(arguments.get(i))) {
                return false;
            }
        }
        return true;
    }

    public Type getReturnType() {
        return returnType;
    }

    public List<Type> getParameterTypes() {
        return parameterTypes;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        for (int i = 0; i < parameterTypes.size(); i++) {
            sb.append(parameterTypes.get(i));
            if (i < parameterTypes.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append(") -> ");
        sb.append(returnType);
        return sb.toString();
    }
}
