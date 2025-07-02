package toyc.ir.stmt;

import toyc.ir.exp.LValue;
import toyc.ir.exp.RValue;

import javax.annotation.Nullable;

/**
 * Representation of all definition statements, i.e., exp1 = exp2.
 *
 * @param <L> type of left-hand side expression
 * @param <R> type of right-hand side expression
 */
public abstract class DefinitionStmt<L extends LValue, R extends RValue>
        extends AbstractStmt {

    /**
     * @return the left-hand side expression. If this Stmt is an {@link Call}
     * which does not have a left-hand side expression, e.g., f(...), then
     * this method returns null; otherwise, it must return a non-null value.
     */
    @Nullable
    public abstract L getLValue();

    /**
     * @return the right-hand side expression.
     */
    public abstract R getRValue();
}
