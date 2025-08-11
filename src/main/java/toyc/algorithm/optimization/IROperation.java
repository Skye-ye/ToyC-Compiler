package toyc.algorithm.optimization;

import toyc.ir.MutableIR;
import toyc.ir.IR;
import toyc.ir.stmt.*;

import javax.annotation.Nonnull;
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
     * @param stmt the statement before which to insert
     * @param newStmt the statement to insert
     * @return true if the insertion was successful, false otherwise
     */
    public boolean insertBefore(@Nonnull Stmt stmt, @Nonnull Stmt newStmt) {
        newStmt.setLineNumber(stmt.getLineNumber());
        return ir.insertBefore(stmt, newStmt);
    }

    /**
     * Insert a statement after the specified {@code stmt} in the IR.
     * @param stmt the statement after which to insert
     * @param newStmt the statement to insert
     * @return true if the insertion was successful, false otherwise
     */
    public boolean insertAfter(@Nonnull Stmt stmt, @Nonnull Stmt newStmt) {
        return ir.insertAfter(stmt, newStmt);
    }

    /**
     * Remove the specified statement from the IR.
     * @param stmt the statement to remove
     * @return true if the removal was successful, false otherwise
     */
    public boolean remove(@Nonnull Stmt stmt) {
        // check whether the stmt is target of other stmts
        Set<Stmt> sourceStmts = ir.getSourceStmts(stmt);
        if(!sourceStmts.isEmpty()){
            Stmt nextStmt = ir.getNextStmt(stmt);
            if (nextStmt != null) {
                for (Stmt sourceStmt : sourceStmts) {
                    if (sourceStmt instanceof Goto gotoStmt) {
                        if (gotoStmt.getTarget() == stmt) {
                            gotoStmt.setTarget(nextStmt);
                        }
                    }
                    else if (sourceStmt instanceof If ifStmt) {
                        if (ifStmt.getTarget() == stmt) {
                            ifStmt.setTarget(nextStmt);
                        }
                    }
                }
            }
        }
        return ir.removeStmt(stmt);
    }

    /**
     * Replace the specified statement with a new statement in the IR.
     * @param stmt the statement to replace
     * @param newStmt the new statement to insert
     * @return true if the replacement was successful, false otherwise
     */
    public boolean replace(@Nonnull Stmt stmt, @Nonnull Stmt newStmt) {
        Set<Stmt> sourceStmts = ir.getSourceStmts(stmt);
        if(!sourceStmts.isEmpty()){
            if (newStmt != null) {
                for (Stmt sourceStmt : sourceStmts) {
                    if (sourceStmt instanceof Goto gotoStmt) {
                        if (gotoStmt.getTarget() == stmt) {
                            gotoStmt.setTarget(newStmt);
                        }
                    }
                    else if (sourceStmt instanceof If ifStmt) {
                        if (ifStmt.getTarget() == stmt) {
                            ifStmt.setTarget(newStmt);
                        }
                    }
                }
            }
        }
        newStmt.setLineNumber(stmt.getLineNumber());
        return ir.replaceStmt(stmt, newStmt);
    }

    /**
     * Get the current IR as an immutable copy.
     */
    @Nonnull
    public IR getIR() {
        return ir.toImmutableIR();
    }
}
