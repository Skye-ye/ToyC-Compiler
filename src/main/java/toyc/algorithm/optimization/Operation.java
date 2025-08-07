package toyc.algorithm.optimization;

import toyc.ir.stmt.Stmt;

public interface Operation {
    /**
     * Insert a statement at the specified index in the IR.
     * @param stmt the statement to insert
     * @param index the index at which to insert the statement
     */
    boolean insert(Stmt stmt, int index);

    /**
     * Remove a statement at the specified index in the IR.
     * @param index the index of the statement to remove
     */
    boolean remove(int index);

    boolean replaceWithNop(int index);
}
