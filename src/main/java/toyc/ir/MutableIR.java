package toyc.ir;

import toyc.ir.exp.Var;
import toyc.ir.stmt.Stmt;
import toyc.language.Function;
import toyc.util.AbstractResultHolder;
import toyc.util.Indexer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Mutable implementation of IR that supports insertion and removal of statements.
 */
public class MutableIR extends AbstractResultHolder implements IR {

    private final Function function;
    private final List<Var> params;
    private final List<Var> vars;
    private final List<Var> returnVars;
    private final Indexer<Var> varIndexer;
    private final List<Stmt> stmts; // 可变的语句列表

    public MutableIR(IR originalIR) {
        this.function = originalIR.getFunction();
        this.params = List.copyOf(originalIR.getParams());
        this.returnVars = List.copyOf(originalIR.getReturnVars());
        this.vars = List.copyOf(originalIR.getVars());
        this.varIndexer = new VarIndexer();
        this.stmts = new ArrayList<>(originalIR.getStmts()); // 创建可变副本
    }

    public MutableIR(Function function, List<Var> params, Set<Var> returnVars, 
                     List<Var> vars, List<Stmt> stmts) {
        this.function = function;
        this.params = List.copyOf(params);
        this.returnVars = List.copyOf(returnVars);
        this.vars = List.copyOf(vars);
        this.varIndexer = new VarIndexer();
        this.stmts = new ArrayList<>(stmts);
    }

    public IR toImmutableIR() {
        return new DefaultIR(function, params, Set.copyOf(returnVars), vars, stmts);
    }

    /**
     * Insert a statement at the specified index.
     */
    public boolean insertStmt(int index, Stmt stmt) {
        if (index < 0 || index > stmts.size()) {
            return false;
        }
        
        stmts.add(index, stmt);
        // 重新设置所有语句的索引
        reindexStmts();
        return true;
    }

    /**
     * Remove a statement at the specified index.
     */
    public boolean removeStmt(int index) {
        if (index < 0 || index >= stmts.size()) {
            return false;
        }
        
        stmts.remove(index);
        // 重新设置所有语句的索引
        reindexStmts();
        return true;
    }

    /**
     * Replace a statement at the specified index.
     */
    public boolean replaceStmt(int index, Stmt newStmt) {
        if (index < 0 || index >= stmts.size()) {
            return false;
        }
        
        stmts.set(index, newStmt);
        newStmt.setIndex(index);
        return true;
    }

    /**
     * Re-index all statements after modification.
     */
    private void reindexStmts() {
        for (int i = 0; i < stmts.size(); i++) {
            stmts.get(i).setIndex(i);
        }
    }

    // 实现 IR 接口的其他方法
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
}