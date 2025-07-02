package toyc.ir.stmt;

import toyc.ir.exp.UnaryExp;
import toyc.ir.exp.Var;

/**
 * Representation of following kinds of unary assign statements:
 * <ul>
 *     <li>negation: x = -y
 * </ul>
 */
public class Unary extends AssignStmt<Var, UnaryExp> {

    public Unary(Var lvalue, UnaryExp rvalue) {
        super(lvalue, rvalue);
    }

    @Override
    public <T> T accept(StmtVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
