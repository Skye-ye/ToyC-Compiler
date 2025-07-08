package toyc.language.type;

import javax.annotation.Nonnull;
import java.util.List;

public record FunctionType(
        List<Type> parameterTypes, Type returnType) implements Type {

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

    @Override
    @Nonnull
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

    @Override
    public String getName() {
        return "function";
    }
}
