package toyc.ir;

import toyc.ir.exp.Var;
import toyc.ir.stmt.Stmt;
import toyc.language.Function;
import toyc.util.AbstractResultHolder;
import toyc.util.Hashes;
import toyc.util.Indexer;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Default implementation of IR.
 * The data structures in this class are immutable.
 */
public class DefaultIR extends AbstractResultHolder implements IR {

    private final Function function;

    private final List<Var> params;

    private final List<Var> vars;

    private final List<Var> returnVars;

    private final Indexer<Var> varIndexer;

    private final List<Stmt> stmts;

    public DefaultIR(
            Function function,
            List<Var> params, Set<Var> returnVars, List<Var> vars,
            List<Stmt> stmts) {
        this.function = function;
        this.params = List.copyOf(params);
        this.returnVars = List.copyOf(returnVars);
        this.vars = List.copyOf(vars);
        this.varIndexer = new VarIndexer();
        this.stmts = List.copyOf(stmts);
    }

    @Override
    public Function getFunction() {
        return function;
    }

    @Override
    public List<Var> getParams() {
        return params;
    }

    @Override
    public Var getParam(int i) {
        return params.get(i);
    }

    @Override
    public boolean isParam(Var var) {
        return params.contains(var);
    }

    @Override
    public List<Var> getReturnVars() {
        return returnVars;
    }

    @Override
    public Var getVar(int i) {
        return vars.get(i);
    }

    @Override
    public List<Var> getVars() {
        return vars;
    }

    @Override
    public Indexer<Var> getVarIndexer() {
        return varIndexer;
    }

    private class VarIndexer implements Indexer<Var>, Serializable {

        @Override
        public int getIndex(Var v) {
            return v.getIndex();
        }

        @Override
        public Var getObject(int i) {
            return getVar(i);
        }
    }

    @Override
    public Stmt getStmt(int i) {
        return stmts.get(i);
    }

    @Override
    public List<Stmt> getStmts() {
        return stmts;
    }

    @Override
    public int getIndex(Stmt s) {
        return s.getIndex();
    }

    @Override
    public Stmt getObject(int i) {
        return getStmt(i);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        DefaultIR other = (DefaultIR) obj;

        return Objects.equals(function, other.function) &&
                Objects.equals(params, other.params) &&
                Objects.equals(vars, other.vars) &&
                Objects.equals(returnVars, other.returnVars) &&
                Objects.equals(stmts, other.stmts);
    }

    @Override
    public int hashCode() {
        return Hashes.hash(function, params, vars, returnVars, stmts);
    }
}
