package toyc.algorithm.optimization;

import toyc.ir.MutableIR;
import toyc.ir.IR;
import toyc.ir.stmt.*;

import java.util.Map;
import java.util.HashMap;

/**
 * Can handle all kinds of linear optimization operations
 * 1. If the optimization operation is linear, you could ignore the index change across the IR
 * 2. Just follow the Fact returned by the analysis, in other words, you couldn't easily remove the stmt you inserted before
 * 3. Side effects can affect the newly inserted stmt, but you can't manipulate it directly
 */

public class AbstractOperation implements Operation {
    private final MutableIR ir;
    private final Map<Integer, Integer> indexMapping;

    public AbstractOperation(IR ir) {
        this.ir = new MutableIR(ir);
        int originalSize = this.ir.getSize();
        this.indexMapping = new HashMap<>();
        for (int i = 0; i < originalSize; i++) {
            indexMapping.put(i, i);
        }
    }

    /* basic operation according to current index */
    @Override
    public boolean insert(Stmt stmt, int index){
        return ir.insertStmt(index, stmt);
    }

    @Override
    public boolean remove(int index) {
        Stmt stmt = ir.getStmt(index);
        if (stmt == null) {
            return false;
        }
        return ir.removeStmt(index);
    }

    @Override
    public boolean replaceWithNop(int index) {
        Stmt stmt = ir.getStmt(index);
        if (stmt == null) {
            return false;
        }
        Stmt nopStmt = new Nop(); // Assuming Nop is a valid statement type representing a no-operation
        return ir.replaceStmt(index, nopStmt);
    }
    
    /* insert only one statement before the index 
     * insert according to the original index, and adjust the index mapping
    (if index is n, then stmt will be the n_th in new ir) */
    public boolean insertByOrigin(Stmt stmt, int originalIndex) {
        Integer currentIndex = indexMapping.get(originalIndex);
        if (currentIndex == null) {
            return false;
        }
        boolean success = ir.insertStmt(currentIndex, stmt);
        if (success) {
            updateMappingAfterInsert(originalIndex);
        }
        return success;
    }

    /*remove only one statement
      remove by the original index, and adjust the index mapping
    */
    public boolean removeByOrigin(int originalIndex) {
        Integer currentIndex = indexMapping.get(originalIndex);
        if (currentIndex == null) {
            return false;
        }
        
        boolean success = ir.removeStmt(currentIndex);
        
        if (success) {
            indexMapping.remove(originalIndex);
            updateMappingAfterRemove(originalIndex);
        }
        
        return success;
    }

    /**
     * Update the index mapping after inserting a statement.
     * This method is called internally to maintain the integrity of the index mapping.
     */
    private void updateMappingAfterInsert(int insertedAt) {
        for (Map.Entry<Integer, Integer> entry : indexMapping.entrySet()) {
            if (entry.getValue()!=null && entry.getValue() >= insertedAt) {
                entry.setValue(entry.getValue() + 1);
            }
        }
    }

    /**
     * Update the index mapping after removing a statement.
     * This method is called internally to maintain the integrity of the index mapping.
     */
    private void updateMappingAfterRemove(int removedAt) {
        indexMapping.put(removedAt, null); //set the removed one as null
        for (Map.Entry<Integer, Integer> entry : indexMapping.entrySet()) {
            if (entry.getValue() != null && entry.getValue() > removedAt) {
                entry.setValue(entry.getValue() - 1);
            }
        }
    }

    /*
     * for complex situation, offers safe operation
     * For example, if you remove the statement which is the target of if or goto statement,
     * you should modify the corresponding statement too
     */
    public boolean removeTarget(int originalIndex) {
        Integer currentIndex = indexMapping.get(originalIndex);
        if (currentIndex == null) {
            return false;
        }

        Stmt targetStmt = ir.getStmt(getCurrentIndex(originalIndex));
        if (targetStmt == null) {
            return false;
        }

        int newTargetIndex = currentIndex;  // when this target is removed, the next one will get its index
        if (!removeByOrigin(originalIndex)) {
            return false;
        }

        for (Stmt stmt : ir) {
            if (stmt instanceof If ifStmt) {
                if (ifStmt.getTarget() == targetStmt) {
                    ifStmt.setTarget(ir.getStmt(newTargetIndex)); // set to a new target
                }
            }
            if (stmt instanceof Goto gotoStmt) {
                if (gotoStmt.getTarget() == targetStmt) {
                    gotoStmt.setTarget(ir.getStmt(newTargetIndex)); // set to a new target
                }
            }
        }

        return true;

    }

    /* other basic operations */
    public Integer getCurrentIndex(int originalIndex) {
        return indexMapping.get(originalIndex);
    }

    public boolean isValidOriginalIndex(int originalIndex) {
       return indexMapping.containsKey(originalIndex);
    }

    public IR getCurrentIR() {
        return ir.toImmutableIR();
    }

    // get statement by current index
    public Stmt getStmt(int currentIndex) {
        return ir.getStmt(currentIndex);
    }
}
