package toyc.ir.stmt;

import toyc.ir.exp.LValue;
import toyc.ir.exp.RValue;
import toyc.util.Indexable;

import java.util.Optional;
import java.util.Set;

/**
 * Representation of statements in IR.
 */
public interface Stmt extends Indexable {

    /**
     * @return the index of this Stmt in the container IR.
     */
    @Override
    int getIndex();

    void setIndex(int index);

    /**
     * @return the line number of this Stmt in the original source file.
     * If the line number is unavailable, return -1.
     */
    int getLineNumber();

    void setLineNumber(int lineNumber);

    /**
     * @return the (optional) left-value expression defined in this Stmt.
     */
    Optional<LValue> getDef();

    /**
     * @return a set of right-value expressions used in this Stmt.
     */
    Set<RValue> getUses();

    /**
     * @return true if execution after this statement could continue at
     * the following statement, otherwise false.
     */
    boolean canFallThrough();

    <T> T accept(StmtVisitor<T> visitor);
}
