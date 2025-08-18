package toyc.ir.stmt;

public abstract class JumpStmt extends AbstractStmt {

    /**
     * @return possible jump targets of this statement.
     */
    public abstract Stmt getTarget();

    /**
     * Set the jump target of this statement.
     *
     * @param target the target statement to jump to.
     */
    public abstract void setTarget(Stmt target);

    /**
     * Convert a target statement to its String representation.
     */
    public String toString(Stmt target) {
        return target == null ?
                "[unknown]" : Integer.toString(target.getIndex());
    }
}
