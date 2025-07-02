package toyc.ir.stmt;

import toyc.ir.exp.*;
import toyc.language.Function;
import toyc.util.collection.ArraySet;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.Set;

/**
 * Representation of call statement, e.g., r = f(...) or f(...).
 */
public class Call extends DefinitionStmt<Var, CallExp> {

    /**
     * The variable receiving the result of the call. This field
     * is null if no variable receives the invocation result, e.g., f(...).
     */
    @Nullable
    private final Var result;

    /**
     * The call expression.
     */
    private final CallExp callExp;

    /**
     * The function containing this statement.
     */
    private final Function container;

    public Call(Function container, CallExp CallExp, @Nullable Var result) {
        this.callExp = CallExp;
        this.result = result;
        this.container = container;
    }

    public Call(Function container, CallExp CallExp) {
        this(container, CallExp, null);
    }

    @Override
    @Nullable
    public Var getLValue() {
        return result;
    }

    @Nullable
    public Var getResult() {
        return result;
    }

    @Override
    public CallExp getRValue() {
        return callExp;
    }

    /**
     * @return the invocation expression of this call.
     * @see CallExp
     */
    public CallExp getCallExp() {
        return callExp;
    }

    public Function getContainer() {
        return container;
    }

    @Override
    public Optional<LValue> getDef() {
        return Optional.ofNullable(result);
    }

    @Override
    public Set<RValue> getUses() {
        Set<RValue> uses = new ArraySet<>(callExp.getUses());
        uses.add(callExp);
        return uses;
    }

    @Override
    public <T> T accept(StmtVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        String ret = result == null ? "" : result + " = ";
        String functionSignature = String.format("<%s %s(%s)>",
                callExp.getFunction().getReturnType(),
                callExp.getFunction().getName(),
                getParamTypesString());
        return ret + "call " + functionSignature + callExp.getArgsString();
    }
    
    private String getParamTypesString() {
        return callExp.getFunction().getParamTypes().stream()
                .map(Object::toString)
                .collect(java.util.stream.Collectors.joining(", "));
    }
}
