package toyc.ir.exp;

public interface ExpVisitor<T> {

    // var
    default T visit(Var var) {
        return visitDefault(var);
    }

    default T visit(IntLiteral literal) {
        return visitDefault(literal);
    }

    default T visit(CallExp invoke) {
        return visitDefault(invoke);
    }

    // unary
    default T visit(NegExp exp) {
        return visitDefault(exp);
    }

    default T visit(NotExp exp) {
        return visitDefault(exp);
    }

    // binary
    default T visit(ArithmeticExp exp) {
        return visitDefault(exp);
    }

    default T visit(ComparisonExp exp) {
        return visitDefault(exp);
    }

    default T visit(ConditionExp exp) {
        return visitDefault(exp);
    }

    // default
    default T visitDefault(Exp exp) {
        return null;
    }
}
