package toyc.ir.exp;

import toyc.language.Function;
import toyc.language.type.Type;
import toyc.util.collection.ArraySet;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Representation of function call expression.
 */
public class CallExp implements RValue {

    /**
     * The function at the invocation.
     */
    protected final Function function;

    /**
     * The arguments of the invocation.
     */
    protected final List<Var> args;

    public CallExp(Function function, List<Var> args) {
        this.function = function;
        this.args = List.copyOf(args);
    }

    @Override
    public Type getType() {
        return function.getReturnType();
    }

    /**
     * @return the function at the invocation.
     */
    public Function getFunction() {
        return function;
    }

    /**
     * @return the number of the arguments of the invocation.
     */
    public int getArgCount() {
        return args.size();
    }

    /**
     * @return the i-th argument of the invocation.
     * @throws IndexOutOfBoundsException if the index is out of range
     *                                   (index &lt; 0 || index &ge; getArgCount())
     */
    public Var getArg(int i) {
        return args.get(i);
    }

    /**
     * @return a list of arguments of the invocation.
     */
    public List<Var> getArgs() {
        return args;
    }


    public String getArgsString() {
        return "(" + args.stream()
                .map(Var::toString)
                .collect(Collectors.joining(", ")) + ")";
    }

    @Override
    public Set<RValue> getUses() {
        return new ArraySet<>(args);
    }

    @Override
    public <T> T accept(ExpVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return function.getName() + getArgsString();
    }
}
