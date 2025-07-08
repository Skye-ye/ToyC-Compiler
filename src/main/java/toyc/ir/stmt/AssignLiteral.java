package toyc.ir.stmt;

import toyc.ir.exp.Literal;
import toyc.ir.exp.Var;

/**
 * Representation of statement that assigns literals, e.g., a = 10.
 */
public class AssignLiteral extends AssignStmt<Var, Literal> {

    public AssignLiteral(Var lvalue, Literal rvalue) {
        super(lvalue, rvalue);
    }

    @Override
    public <T> T accept(StmtVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
