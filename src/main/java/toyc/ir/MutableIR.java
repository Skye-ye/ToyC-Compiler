package toyc.ir;

import toyc.ir.exp.Var;
import toyc.ir.stmt.Return;
import toyc.ir.stmt.Stmt;
import toyc.language.Function;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * Mutable implementation of IR that supports insertion and removal of statements.
 */
public class MutableIR {

    private final Function function;
    private final List<Var> params; // Immutable
    private final List<Var> vars; // Mutable
    private Set<Var> returnVars; // Mutable
    private final List<Stmt> stmts; // Mutable

    public MutableIR(IR ir) {
        this.function = ir.getFunction();
        this.params = List.copyOf(ir.getParams());
        this.vars = List.copyOf(ir.getVars());
        this.stmts = new LinkedList<>(ir.getStmts());
    }

    public IR toImmutableIR() {
        reindexStmts();
        collectReturnVars();
        return new DefaultIR(function, params, returnVars, vars, stmts);
    }

    /**
     * Insert a statement after the specified statement.
     * Efficient O(n) operation for LinkedList.
     */
    public boolean insertAfter(@Nonnull Stmt afterStmt, @Nonnull Stmt newStmt) {
        ListIterator<Stmt> iterator = stmts.listIterator();
        while (iterator.hasNext()) {
            if (iterator.next() == afterStmt) {
                iterator.add(newStmt);
                return true;
            }
        }
        return false; // Statement not found
    }

    /**
     * Insert a statement before the specified statement.
     * Efficient O(n) operation for LinkedList.
     */
    public boolean insertBefore(@Nonnull Stmt beforeStmt, @Nonnull Stmt newStmt) {
        ListIterator<Stmt> iterator = stmts.listIterator();
        while (iterator.hasNext()) {
            if (iterator.next() == beforeStmt) {
                iterator.previous(); // Go back to the position before beforeStmt
                iterator.add(newStmt);
                return true;
            }
        }
        return false; // Statement not found
    }

    /**
     * Remove the specified statement.
     * O(n) operation but efficient for LinkedList.
     */
    public boolean removeStmt(@Nonnull Stmt stmt) {
        return stmts.remove(stmt);
    }

    /**
     * Replace a statement with another statement.
     * Efficient O(n) operation for LinkedList.
     */
    public boolean replaceStmt(@Nonnull Stmt oldStmt, @Nonnull Stmt newStmt) {
        ListIterator<Stmt> iterator = stmts.listIterator();
        while (iterator.hasNext()) {
            if (iterator.next() == oldStmt) {
                iterator.set(newStmt);
                return true;
            }
        }
        return false; // Statement not found
    }

    /**
     * Re-index all statements
     */
    private void reindexStmts() {
        for (int i = 0; i < stmts.size(); i++) {
            stmts.get(i).setIndex(i);
        }
    }

    private void collectReturnVars() {
        returnVars = new HashSet<>();
        for (Stmt stmt : stmts) {
            if (stmt instanceof Return returnStmt) {
                Var retVar = returnStmt.getValue();
                if (retVar != null) {
                    returnVars.add(retVar);
                }
            }
        }
    }
}