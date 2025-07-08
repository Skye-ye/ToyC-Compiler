package toyc.ir;

import toyc.ir.exp.Var;
import toyc.ir.stmt.Return;
import toyc.ir.stmt.Stmt;
import toyc.language.Function;
import toyc.language.type.Type;
import toyc.language.type.VoidType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Helper for building IR for a method from scratch.
 * Given a {@link Function}, this helper automatically creates {@code this}
 * variable (for instance method), parameters, and return variable (if method
 * return type is not {@code void}) for the method.
 */
public class IRBuildHelper {

    private static final String PARAM = "%param";

    private static final String TEMP = "%temp";

    private static final String RETURN = "%return";

    private final Function function;

    private final List<Var> params;

    private final Var returnVar;

    private final Set<Var> returnVars;

    private final List<Var> vars;

    /**
     * Counter for indexing all variables.
     */
    private int varCounter = 0;

    /**
     * Counter for naming temporary variables.
     */
    private int tempCounter = 0;

    public IRBuildHelper(Function function) {
        this.function = function;
        // build this variable
        vars = new ArrayList<>();
        // build parameters
        params = new ArrayList<>(function.getParamCount());
        for (int i = 0; i < function.getParamCount(); ++i) {
            params.add(newVar(PARAM + i, function.getParamType(i)));
        }
        // build return variable
        Type retType = function.getReturnType();
        if (!retType.equals(VoidType.VOID)) {
            returnVar = newVar(RETURN, retType);
            returnVars = Set.of(returnVar);
        } else {
            returnVar = null;
            returnVars = Set.of();
        }
    }

    public Var getParam(int i) {
        return params.get(i);
    }

    /**
     * @return the return variable of the IR being built.
     */
    public Var getReturnVar() {
        return returnVar;
    }

    /**
     * @return a new temporary variable of given type.
     */
    public Var newTempVar(Type type) {
        return newVar(TEMP + tempCounter++, type);
    }

    /**
     * @return a new return statement of the IR being built.
     */
    public Return newReturn() {
        return returnVar != null ? new Return(returnVar) : new Return();
    }

    /**
     * Builds an empty IR which contains only a {@link Return} statement.
     */
    public IR buildEmpty() {
        return build(List.of(newReturn()));
    }

    /**
     * Builds an IR with given {@link Stmt}s. This method sets the indexes
     * of given {@link Stmt}s, so client code does not need to set the indexes.
     *
     * @param stmts statements of the IR being built.
     */
    public IR build(List<Stmt> stmts) {
        int i = 0;
        for (Stmt stmt : stmts) {
            stmt.setIndex(i++);
        }
        return new DefaultIR(function, params, returnVars, vars, stmts);
    }

    private Var newVar(String name, Type type) {
        Var var = new Var(function, name, type, varCounter++);
        vars.add(var);
        return var;
    }
}
