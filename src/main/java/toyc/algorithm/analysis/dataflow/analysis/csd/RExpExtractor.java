package toyc.algorithm.analysis.dataflow.analysis.csd;

import toyc.ir.exp.*;
import toyc.ir.stmt.*;
import toyc.util.collection.ArraySet;

import java.util.Set;

/**
 * Extractor for extracting all sub-expressions from statements. (only useful subexpression rvalue)
 * This class provides methods to extract all expressions (usually RValues) 
 * contained within a given statement.
 */
public class RExpExtractor implements StmtVisitor<Set<Exp>> {

    private static final RExpExtractor INSTANCE = new RExpExtractor();
    /**
     * Extracts all sub-expressions from the given statement.
     * 
     * @param stmt the statement to extract expressions from
     * @return a set containing all sub-expressions found in the statement
     */
    public static Set<Exp> extract(Stmt stmt) {
        return stmt.accept(INSTANCE);
    }

    @Override
    public Set<Exp> visit(Binary stmt) {
        Set<Exp> exps = new ArraySet<>();
        // Add the binary expression
        BinaryExp binaryExp = stmt.getRValue();
        exps.add(binaryExp);
        return exps;
    }

    @Override
    public Set<Exp> visit(Unary stmt) {
        Set<Exp> exps = new ArraySet<>();
        // Add the unary expression
        UnaryExp unaryExp = stmt.getRValue();
        exps.add(unaryExp);
        return exps;
    }

    // call statements need to discrete f() or a = f()
    // there is no need to do cse on f()
    @Override
    public Set<Exp> visit(Call stmt) {
        Set<Exp> exps = new ArraySet<>();
        // Add the result variable if exists
        if (stmt.getLValue() == null) {
            return exps;
        }
        else{
            // Add the call expression
            CallExp callExp = stmt.getCallExp();
            exps.add(callExp);
            return exps;
        }
    }

    @Override
    public Set<Exp> visit(If stmt) {
        Set<Exp> exps = new ArraySet<>();
        // Add the condition expression
        ConditionExp condition = stmt.getCondition();
        exps.add(condition);
        return exps;
    }

    // statements like assignliteral, copy don't contain meaningful subexpressions
    @Override
    public Set<Exp> visit(AssignLiteral stmt) {
        return Set.of(); 
    }

    @Override
    public Set<Exp> visit(Copy stmt) {
        return Set.of();
    }

    // return only have one var as its subexpression (useless)
    @Override
    public Set<Exp> visit(Return stmt) {
        return Set.of();
    }

    @Override
    public Set<Exp> visit(Goto stmt) {
        // Goto statements don't contain expressions
        return Set.of();
    }

    @Override
    public Set<Exp> visit(Nop stmt) {
        // Nop statements don't contain expressions
        return Set.of();
    }

    @Override
    public Set<Exp> visitDefault(Stmt stmt) {
        // Default case returns empty set
        return Set.of();
    }
}