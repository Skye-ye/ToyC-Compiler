package toyc.ir;

import toyc.ir.exp.Var;
import toyc.ir.stmt.*;
import toyc.util.AnalysisException;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * Mutable implementation of IR that supports insertion and removal of statements.
 */
public class MutableIR {

    private final IR ir; // Original immutable IR for reference
    private final List<Stmt> stmts; // Mutable

    private boolean modified = false;

    public MutableIR(IR ir) {
        this.ir = ir;
        this.stmts = new LinkedList<>(ir.getStmts());
    }

    public IR toImmutableIR() {
        if (!modified) {
            return ir; // If no modifications, return the original IR
        }
        reindexStmts();
        List<Var> vars = collectVars();
        Set<Var> returnVars = collectReturnVars();
        return new DefaultIR(ir.getFunction(), ir.getParams(), returnVars, vars, stmts);
    }

    /**
     * Insert a statement after the specified statement.
     * Efficient O(n) operation for LinkedList.
     *
     * @param afterStmt the statement after which to insert
     * @param newStmt   the statement to insert
     * @throws AnalysisException if the statement is not found
     */
    public void insertAfter(@Nonnull Stmt afterStmt, @Nonnull Stmt newStmt) {
        ListIterator<Stmt> iterator = stmts.listIterator();
        while (iterator.hasNext()) {
            if (iterator.next() == afterStmt) {
                iterator.add(newStmt);
                modified = true;
                return;
            }
        }
        throw new AnalysisException("insertAfter: Statement not found");
    }

    /**
     * Insert a statement before the specified statement.
     * Efficient O(n) operation for LinkedList.
     *
     * @param beforeStmt the statement before which to insert
     * @param newStmt    the statement to insert
     * @throws AnalysisException if the statement is not found
     */
    public void insertBefore(@Nonnull Stmt beforeStmt, @Nonnull Stmt newStmt) {
        ListIterator<Stmt> iterator = stmts.listIterator();
        while (iterator.hasNext()) {
            if (iterator.next() == beforeStmt) {
                iterator.previous(); // Go back to the position before beforeStmt
                iterator.add(newStmt);
                modified = true;
                return;
            }
        }
        throw new AnalysisException("insertBefore: Statement not found");
    }

    /**
     * Remove the specified statement.
     * O(n) operation but efficient for LinkedList.
     *
     * @param stmt the statement to remove
     * @throws AnalysisException if the statement is not found
     */
    public void removeStmt(@Nonnull Stmt stmt) {
        if (!stmts.remove(stmt)) {
            throw new AnalysisException("removeStmt: Statement not found");
        }
        modified = true;
    }

    /**
     * Replace a statement with another statement.
     * Efficient O(n) operation for LinkedList.
     *
     * @param oldStmt the statement to be replaced
     * @param newStmt the new statement to replace with
     * @throws AnalysisException if the old statement is not found
     */
    public void replaceStmt(@Nonnull Stmt oldStmt, @Nonnull Stmt newStmt) {
        ListIterator<Stmt> iterator = stmts.listIterator();
        while (iterator.hasNext()) {
            if (iterator.next() == oldStmt) {
                iterator.set(newStmt);
                modified = true;
                return;
            }
        }
        throw new AnalysisException("replaceStmt: Statement not found");
    }

    /**
     * Get the next statement after the specified statement.
     */
    public Stmt getNextStmt(Stmt currentStmt) {
        Iterator<Stmt> iterator = stmts.iterator();
        while (iterator.hasNext()) {
            Stmt stmt = iterator.next();
            if (stmt == currentStmt) {
                // Found current statement, return next if it exists
                return iterator.hasNext() ? iterator.next() : null;
            }
        }
        return null; // Current statement not found
    }

    /**
     * Get the predecessors that jump to the specified statement.
     */
    public Set<JumpStmt> getPredecessors(Stmt stmt) {
        Set<JumpStmt> predecessors = new HashSet<>();
        for (Stmt s : stmts) {
            if (s instanceof JumpStmt jumpStmt) {
                if (jumpStmt.getTarget() == stmt) {
                    predecessors.add(jumpStmt);
                }
            }
        }
        return predecessors;
    }

    /**
     * Get a read-only view of the stmts
     */
    public List<Stmt> getStmts() {
        return Collections.unmodifiableList(stmts);
    }

    /**
     * Re-index all statements
     */
    private void reindexStmts() {
        for (int i = 0; i < stmts.size(); i++) {
            stmts.get(i).setIndex(i);
        }
    }

    /**
     * Collect all variables used in the IR.
     */
    private List<Var> collectVars() {
        Set<Var> vars = new LinkedHashSet<>(ir.getParams());

        // Collect variables from all statements
        for (Stmt stmt : stmts) {
            // Add defined variables (lvalues)
            stmt.getDef().ifPresent(def -> {
                if (def instanceof Var var) {
                    vars.add(var);
                }
            });

            // Add used variables (rvalues)
            for (var use : stmt.getUses()) {
                if (use instanceof Var var) {
                    vars.add(var);
                }
            }
        }

        List<Var> allVar = new ArrayList<>(vars);
        for (int i = 0; i < allVar.size(); i++) {
            allVar.get(i).setIndex(i);
        }
        return allVar;
    }

    /**
     * Collect return variables used in the IR.
     */
    private Set<Var> collectReturnVars() {
        Set<Var> returnVars = new HashSet<>();
        for (Stmt stmt : stmts) {
            if (stmt instanceof Return returnStmt) {
                Var retVar = returnStmt.getValue();
                if (retVar != null) {
                    returnVars.add(retVar);
                }
            }
        }
        return returnVars;
    }
}