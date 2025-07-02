package toyc.ir.stmt;

/**
 * Stmt visitor which may return a result after the visit.
 *
 * @param <T> type of the return value
 */
public interface StmtVisitor<T> {

    default T visit(AssignLiteral stmt) {
        return visitDefault(stmt);
    }

    default T visit(Copy stmt) {
        return visitDefault(stmt);
    }

    default T visit(Binary stmt) {
        return visitDefault(stmt);
    }

    default T visit(Unary stmt) {
        return visitDefault(stmt);
    }

    default T visit(Goto stmt) {
        return visitDefault(stmt);
    }

    default T visit(If stmt) {
        return visitDefault(stmt);
    }

    default T visit(Call stmt) {
        return visitDefault(stmt);
    }

    default T visit(Return stmt) {
        return visitDefault(stmt);
    }

    default T visit(Nop stmt) {
        return visitDefault(stmt);
    }

    default T visitDefault(Stmt stmt) {
        return null;
    }
}
