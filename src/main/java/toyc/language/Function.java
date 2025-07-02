package toyc.language;

import toyc.World;
import toyc.ir.IR;
import toyc.language.type.Type;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Represents functions in the program. Each instance contains various
 * information of a function, including function name, signature,
 * function body (IR), etc.
 */
public class Function {
    private final String name;

    private final List<Type> paramTypes;

    private final Type returnType;

    @Nullable
    private final List<String> paramNames;

    private transient IR ir;

    public Function(String name, List<Type> paramTypes, Type returnType,
                    @Nullable List<String> paramNames) {
        this.name = name;
        this.paramTypes = List.copyOf(paramTypes);
        this.returnType = returnType;
        this.paramNames = paramNames;
    }

    public int getParamCount() {
        return paramTypes.size();
    }

    public Type getParamType(int i) {
        return paramTypes.get(i);
    }

    public List<Type> getParamTypes() {
        return paramTypes;
    }

    @Nullable
    public String getParamName(int i) {
        return paramNames == null ? null : paramNames.get(i);
    }

    public String getName() {
        return name;
    }

    public Type getReturnType() {
        return returnType;
    }

    public IR getIR() {
        if (ir == null) {
            ir = World.get().getIRBuilder().buildIR(this);
        }
        return ir;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append("(");
        for (int i = 0; i < paramTypes.size(); i++) {
            sb.append(paramTypes.get(i));
            if (i < paramTypes.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append(") -> ").append(returnType);
        return sb.toString();
    }
}
