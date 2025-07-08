package toyc.ir;

import toyc.language.Function;
import toyc.ir.exp.Var;
import toyc.ir.stmt.Call;
import toyc.ir.stmt.Stmt;
import toyc.util.Indexer;
import toyc.util.ResultHolder;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Intermediate representation for method body of non-abstract methods.
 * Each IR contains the variables and statements defined in a method.
 */
public interface IR extends Iterable<Stmt>, Indexer<Stmt>, ResultHolder, Serializable {

    /**
     * @return the function that defines the content of this IR.
     */
    Function getFunction();

    /**
     * @return the parameters in this IR ("this" variable is excluded).
     * The order of the parameters in the resulting list is the same as
     * the order they are declared in the method.
     */
    List<Var> getParams();

    /**
     * @return the i-th parameter in this IR. The indexes start from 0.
     */
    Var getParam(int i);

    /**
     * @return {@code true} if {@code var} is a parameter of this IR.
     */
    boolean isParam(Var var);

    /**
     * @return all returned variables. If the method return type is void,
     * then returns empty list.
     */
    List<Var> getReturnVars();

    /**
     * @return the i-th {@link Var} in this IR. The indexes start from 0.
     */
    Var getVar(int i);

    /**
     * @return the variables in this IR.
     */
    List<Var> getVars();

    /**
     * @return an indexer for the variables in this IR.
     */
    Indexer<Var> getVarIndexer();

    /**
     * @return the i-th {@link Stmt} in this IR. The indexes start from 0.
     */
    Stmt getStmt(int i);

    /**
     * @return a list of Stmts in this IR.
     */
    List<Stmt> getStmts();

    /**
     * @return a stream of Stmts in this IR.
     */
    default Stream<Stmt> stmts() {
        return getStmts().stream();
    }

    /**
     * Convenient method to obtain Functions in this IR.
     *
     * @return a stream of Invokes in this IR.
     */
    default Stream<Call> calls() {
        return stmts()
                .filter(s -> s instanceof Call)
                .map(s -> (Call) s);
    }

    /**
     * @return iterator of Stmts in this IR.
     */
    @Override
    @Nonnull
    default Iterator<Stmt> iterator() {
        return getStmts().iterator();
    }
}
