package toyc.algorithm.optimization;

import toyc.ir.MutableIR;
import toyc.ir.IR;
import toyc.ir.stmt.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.HashMap;

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
    private final Map<Integer, Integer> indexMapping;

    public IROperation(IR ir) {
        this.ir = new MutableIR(ir);
        int originalSize = this.ir.getStmts().size();
        this.indexMapping = new HashMap<>();
        for (int i = 0; i < originalSize; i++) {
            indexMapping.put(i, i);
        }
    }

    /**
     * Insert a statement at the specified index in the IR.
     * @param stmt the statement to insert
     * @param index the index at which to insert the statement
     * @return true if the insertion was successful, false otherwise
     */
    public boolean insert(@Nonnull Stmt stmt, int index) {
        if (index < 0 || index > ir.getStmts().size()) {
            return false;
        }
        return ir.insertStmt(index, stmt);
    }

    /**
     * Remove a statement at the specified index in the IR.
     * @param index the index of the statement to remove
     * @return true if the removal was successful, false otherwise
     */
    public boolean remove(int index) {
        if (index < 0 || index >= ir.getStmts().size()) {
            return false;
        }
        return ir.removeStmt(index);
    }

    /**
     * Replace a statement at the specified index with a no-operation (NOP).
     * @param index the index of the statement to replace
     * @return true if the replacement was successful, false otherwise
     */
    public boolean replaceWithNop(int index) {
        if (index < 0 || index >= ir.getStmts().size()) {
            return false;
        }
        Stmt nopStmt = new Nop();
        return ir.replaceStmt(index, nopStmt);
    }

    /**
     * Insert a statement before the given original index position.
     * @param stmt the statement to insert
     * @param originalIndex the original index position
     * @return true if successful, false otherwise
     */
    public boolean insertByOrigin(@Nonnull Stmt stmt, int originalIndex) {
        Integer currentIndex = indexMapping.get(originalIndex);
        if (currentIndex == null) {
            return false;
        }

        boolean success = ir.insertStmt(currentIndex, stmt);
        if (success) {
            updateMappingAfterInsert(currentIndex);
        }
        return success;
    }

    /**
     * Remove a statement by its original index.
     * @param originalIndex the original index of the statement to remove
     * @return true if successful, false otherwise
     */
    public boolean removeByOrigin(int originalIndex) {
        Integer currentIndex = indexMapping.get(originalIndex);
        if (currentIndex == null) {
            return false;
        }

        boolean success = ir.removeStmt(currentIndex);
        if (success) {
            updateMappingAfterRemove(originalIndex, currentIndex);
        }
        return success;
    }

    /**
     * Replace a statement with NOP by its original index.
     * @param originalIndex the original index of the statement to replace
     * @return true if successful, false otherwise
     */
    public boolean replaceWithNopByOrigin(int originalIndex) {
        Integer currentIndex = indexMapping.get(originalIndex);
        if (currentIndex == null) {
            return false;
        }

        Stmt nopStmt = new Nop();
        return ir.replaceStmt(currentIndex, nopStmt);
    }

    /**
     * Safely remove a statement that might be a target of control flow statements.
     * Updates all references to point to the next statement.
     * @param originalIndex the original index of the target statement
     * @return true if successful, false otherwise
     */
    public boolean removeTarget(int originalIndex) {
        Integer currentIndex = indexMapping.get(originalIndex);
        if (currentIndex == null) {
            return false;
        }

        Stmt targetStmt = ir.getStmt(currentIndex);
        if (targetStmt == null) {
            return false;
        }

        // Determine the new target (next statement or null if at end)
        Stmt newTarget = null;
        if (currentIndex + 1 < ir.getStmts().size()) {
            newTarget = ir.getStmt(currentIndex + 1);
        }

        // Update all references to this target before removing
        updateControlFlowReferences(targetStmt, newTarget);

        // Remove the statement
        return removeByOrigin(originalIndex);
    }

    /**
     * Update all control flow statements that reference the old target.
     */
    private void updateControlFlowReferences(Stmt oldTarget, Stmt newTarget) {
        for (Stmt stmt : ir) {
            if (stmt instanceof If ifStmt && ifStmt.getTarget() == oldTarget) {
                ifStmt.setTarget(newTarget);
            } else if (stmt instanceof Goto gotoStmt && gotoStmt.getTarget() == oldTarget) {
                gotoStmt.setTarget(newTarget);
            }
        }
    }

    /**
     * Update mappings after inserting a statement at the given current index.
     */
    private void updateMappingAfterInsert(int insertedAtCurrentIndex) {
        for (Map.Entry<Integer, Integer> entry : indexMapping.entrySet()) {
            Integer currentIdx = entry.getValue();
            if (currentIdx != null && currentIdx >= insertedAtCurrentIndex) {
                entry.setValue(currentIdx + 1);
            }
        }
    }

    /**
     * Update mappings after removing a statement.
     */
    private void updateMappingAfterRemove(int originalIndex, int removedAtCurrentIndex) {
        // Mark the removed index as invalid
        indexMapping.put(originalIndex, null);

        // Shift all indices after the removed position
        for (Map.Entry<Integer, Integer> entry : indexMapping.entrySet()) {
            Integer currentIdx = entry.getValue();
            if (currentIdx != null && currentIdx > removedAtCurrentIndex) {
                entry.setValue(currentIdx - 1);
            }
        }
    }

    /**
     * Get the current index for an original index.
     * @param originalIndex the original index
     * @return the current index, or null if the statement was removed
     */
    @Nullable
    public Integer getCurrentIndex(int originalIndex) {
        return indexMapping.get(originalIndex);
    }

    /**
     * Check if an original index is still valid (not removed).
     */
    public boolean isValidOriginalIndex(int originalIndex) {
        Integer currentIndex = indexMapping.get(originalIndex);
        return currentIndex != null;
    }

    /**
     * Get the current IR as an immutable copy.
     */
    @Nonnull
    public IR getCurrentIR() {
        return ir.toImmutableIR();
    }

    /**
     * Get a statement by current index.
     */
    @Nullable
    public Stmt getStmt(int currentIndex) {
        if (currentIndex < 0 || currentIndex >= ir.getStmts().size()) {
            return null;
        }
        return ir.getStmt(currentIndex);
    }
}
