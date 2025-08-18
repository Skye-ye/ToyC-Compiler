package toyc.algorithm.optimization;

import toyc.ir.IR;
import toyc.ir.MutableIR;
import toyc.ir.stmt.Goto;
import toyc.ir.stmt.If;
import toyc.ir.stmt.JumpStmt;
import toyc.ir.stmt.Stmt;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Handles linear optimization operations on IR with proper index mapping.
 * <p>
 * Key features:
 * 1. Maintains mapping between original and current indices
 * 2. Provides safe operations for complex scenarios (e.g., removing jump targets)
 * 3. Supports both current-index and original-index based operations
 */
public class IROperation {
    private final MutableIR ir;

    public IROperation(IR ir) {
        this.ir = new MutableIR(ir);
    }

    /**
     * Insert a statement before the specified {@code stmt} in the IR.
     *
     * @param stmt    the statement before which to insert
     * @param newStmt the statement to insert
     */
    public void insertBefore(@Nonnull Stmt stmt, @Nonnull Stmt newStmt) {
        newStmt.setLineNumber(stmt.getLineNumber());
        ir.insertBefore(stmt, newStmt);
    }

    /**
     * Insert a statement after the specified {@code stmt} in the IR.
     *
     * @param stmt    the statement after which to insert
     * @param newStmt the statement to insert
     */
    public void insertAfter(@Nonnull Stmt stmt, @Nonnull Stmt newStmt) {
        newStmt.setLineNumber(stmt.getLineNumber());
        ir.insertAfter(stmt, newStmt);
    }

    /**
     * Remove the specified statement from the IR.
     * If the statement to be removed is a target of other statements (e.g., Goto, If),
     * it updates those statements to point to the next statement.
     *
     * @param stmt the statement to remove
     */
    public void remove(@Nonnull Stmt stmt) {
        // check whether the stmt is target of other stmts
        Stmt nextStmt = ir.getNextStmt(stmt);
        if (nextStmt != null) {
            updateTargets(stmt, nextStmt);
        }
        ir.removeStmt(stmt);
    }

    /**
     * Replace the specified statement with a new statement in the IR.
     * If the statement to be replaced is a target of other statements (e.g., Goto, If),
     * it updates those statements to point to the new statement.
     *
     * @param stmt    the statement to replace
     * @param newStmt the new statement to insert
     */
    public void replace(@Nonnull Stmt stmt, @Nonnull Stmt newStmt) {
        updateTargets(stmt, newStmt);
        newStmt.setLineNumber(stmt.getLineNumber());
        ir.replaceStmt(stmt, newStmt);
    }

    /**
     * Replace the specified statement with a list of new statements in the IR.
     * If the statement to be replaced is a target of other statements (e.g., Goto, If),
     * it updates those statements to point to the first new statement.
     *
     * @param stmt     the statement to replace
     * @param newStmts the list of new statements to insert
     */
    public void replace(@Nonnull Stmt stmt, @Nonnull List<Stmt> newStmts) {
        Stmt firstStmt = newStmts.getFirst();
        updateTargets(stmt, firstStmt);
        for (Stmt newStmt : newStmts) {
            newStmt.setLineNumber(stmt.getLineNumber());
            ir.insertBefore(stmt, newStmt);
        }
        ir.removeStmt(stmt);
    }

    public void insertUnrolledLoop(@Nonnull Stmt header, @Nonnull List<Stmt> body) {
        Stmt newHeader = body.getFirst();
        for (JumpStmt stmt : ir.getPredecessors(header)) {
            // Only update the jump stmts before old header
            if (stmt.getIndex() < header.getIndex()) {
                stmt.setTarget(newHeader);
            }
        }
        for (Stmt bodyStmt : body) {
            bodyStmt.setLineNumber(header.getLineNumber());
            ir.insertBefore(header, bodyStmt);
        }
    }

    /**
     * Get the next statement after the specified statement in the IR.
     *
     * @param stmt the statement for which to find the next statement
     * @return the next statement, or null if there is no next statement
     */
    public Stmt getNextStmt(@Nonnull Stmt stmt) {
        return ir.getNextStmt(stmt);
    }

    /**
     * Get the current IR as an immutable copy.
     */
    @Nonnull
    public IR getIR() {
        optimizeControlFlow();
        return ir.toImmutableIR();
    }

    /**
     * Update the targets of statements that reference the old statement.
     *
     * @param oldStmt the old statement to be replaced
     * @param newStmt the new statement to set as the target
     */
    private void updateTargets(@Nonnull Stmt oldStmt, @Nonnull Stmt newStmt) {
        Set<JumpStmt> sourceStmts = ir.getPredecessors(oldStmt);
        if (!sourceStmts.isEmpty()) {
            for (JumpStmt sourceStmt : sourceStmts) {
                if (sourceStmt.getTarget() == oldStmt) {
                    sourceStmt.setTarget(newStmt);
                }
            }
        }
    }

    /**
     * Optimize control flow by removing unnecessary statements.
     * Specifically, it removes If and Goto statements that directly lead to the next statement.
     */
    private void optimizeControlFlow() {
        List<Stmt> stmts = ir.getStmts();
        List<Stmt> toRemove = new ArrayList<>();

        for (Stmt stmt : stmts) {
            if (stmt instanceof JumpStmt jumpStmt) {
                Stmt nextStmt = ir.getNextStmt(stmt);
                Stmt target = jumpStmt.getTarget();
                assert target != null;
                assert nextStmt != null;
                if (target == nextStmt) {
                    toRemove.add(stmt);
                }
            }
        }

        // Remove all marked statements
        for (Stmt stmt : toRemove) {
            remove(stmt);
        }
    }
}
