package toyc.analysis.dataflow.analysis.cse;

import toyc.analysis.dataflow.fact.MapFact;
import toyc.ir.exp.*;

import java.util.Collections;
import java.util.Map;
import java.util.Iterator;
import java.util.List;
/**
 * Represents data facts of common subexpression elimination, 
 * which contains a set of available expressions.
 */
public class CSEFact extends MapFact<Exp, Integer> {

    public CSEFact() {
        this(Collections.emptyMap());
    }
    
    /**
     * Copy constructor
     */
    public CSEFact(Map<Exp, Integer> map) {
        super(map);
    }

    @Override
    public CSEFact copy() {
        return new CSEFact(this.map);
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public boolean contains(Exp exp) {
        return map.containsKey(exp);
    }

    public boolean eliminate(Var lvalue) {
        boolean changed = false;
        Iterator<Map.Entry<Exp, Integer>> iterator = map.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Exp, Integer> entry = iterator.next();
            Exp exp = entry.getKey();
            if (exp instanceof BinaryExp) {
                BinaryExp binaryExp = (BinaryExp) exp;
                if (binaryExp.getOperand1().equals(lvalue) || binaryExp.getOperand2().equals(lvalue)) {
                    iterator.remove();  // Remove the expression if it contains the lvalue
                    changed = true;
                }
            }
            else if (exp instanceof UnaryExp) {
                UnaryExp unaryExp = (UnaryExp) exp;
                if (unaryExp.getOperand().equals(lvalue)) {
                    iterator.remove();  // Remove the expression if it contains the lvalue
                    changed = true;
                }
            }
            else if (exp instanceof CallExp) {
                CallExp callExp = (CallExp) exp;
                List<Var> args = callExp.getArgs();
                if (args.contains(lvalue)) {
                    iterator.remove();
                    changed = true;  // Remove the expression if it contains the lvalue
                }
            }
        }
        return changed;
    }
}