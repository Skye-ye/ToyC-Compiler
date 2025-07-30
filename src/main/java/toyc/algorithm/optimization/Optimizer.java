package toyc.algorithm.optimization;

import toyc.ir.IR;
import toyc.ir.stmt.Stmt;

public interface Optimizer {
    /**
     * Insert a statement at the specified index in the IR.
     * @param ir the intermediate representation to modify
     * @param stmt the statement to insert
     * @param index the index at which to insert the statement
     */
    void insert(IR ir, Stmt stmt, int index);

    /**
     * Remove a statement at the specified index in the IR.
     * @param ir the intermediate representation to modify
     * @param index the index of the statement to remove
     */
    void remove(IR ir, int index);
}
