package toyc.ir.stmt;

import java.util.List;

/**
 * Representation of goto statement, e.g., goto L.
 */
public class Goto extends JumpStmt {

    private Stmt target;

    @Override
    public Stmt getTarget() {
        return target;
    }

    @Override
    public void setTarget(Stmt target) {
        this.target = target;
    }

    @Override
    public boolean canFallThrough() {
        return false;
    }

    @Override
    public <T> T accept(StmtVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return "goto " + toString(target);
    }
}
