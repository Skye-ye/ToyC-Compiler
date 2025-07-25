package toyc.analysis.dataflow.analysis.cse;

import toyc.ir.exp.*;
import toyc.ir.stmt.*;

public class LExpExtractor implements StmtVisitor<Var> {
    private static final LExpExtractor INSTANCE = new LExpExtractor();

    /**
     * Extracts the left-hand side variable from the given statement.
     *
     * @param stmt the statement to extract the left-hand side variable from
     * @return the left-hand side variable, or null if not applicable
     */
    public static Var extract(Stmt stmt) {
        return stmt.accept(INSTANCE);
    }

    @Override
    public Var visit(AssignLiteral stmt) {
        return stmt.getLValue();
    }

    @Override
    public Var visit(Binary stmt) {
        return stmt.getLValue();
    }

    @Override
    public Var visit(Copy stmt) {
        return stmt.getLValue();
    }

    @Override
    public Var visit(Unary stmt) {
        return stmt.getLValue();
    }

    @Override
    public Var visit(Call stmt) {
        return stmt.getLValue();
    }
}
