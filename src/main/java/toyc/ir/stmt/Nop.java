package toyc.ir.stmt;

/**
 * Representation of no-operation statement.
 * Used as placeholders for jump targets.
 */
public class Nop extends AbstractStmt {

    @Override
    public <T> T accept(StmtVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return "nop";
    }
}