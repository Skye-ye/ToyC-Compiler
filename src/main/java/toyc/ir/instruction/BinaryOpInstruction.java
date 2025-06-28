package toyc.ir.instruction;

import toyc.ir.InstructionVisitor;
import toyc.ir.value.Value;
import toyc.ir.value.Variable;

public class BinaryOpInstruction extends Instruction {
    public enum BinaryOp {
        ADD, SUB, MUL, DIV, MOD,
        LT, GT, LE, GE, EQ, NEQ,
        AND, OR
    }
    
    private final Variable result;
    private final Value left;
    private final BinaryOp operator;
    private final Value right;
    
    public BinaryOpInstruction(Variable result, Value left, BinaryOp operator, Value right) {
        super();
        this.result = result;
        this.left = left;
        this.operator = operator;
        this.right = right;
    }
    
    public Variable getResult() {
        return result;
    }
    
    public Value getLeft() {
        return left;
    }
    
    public BinaryOp getOperator() {
        return operator;
    }
    
    public Value getRight() {
        return right;
    }
    
    @Override
    public String toString() {
        return result + " = " + left + " " + operatorToString() + " " + right;
    }
    
    private String operatorToString() {
        return switch (operator) {
            case ADD -> "+";
            case SUB -> "-";
            case MUL -> "*";
            case DIV -> "/";
            case MOD -> "%";
            case LT -> "<";
            case GT -> ">";
            case LE -> "<=";
            case GE -> ">=";
            case EQ -> "==";
            case NEQ -> "!=";
            case AND -> "&&";
            case OR -> "||";
        };
    }
    
    @Override
    public void accept(InstructionVisitor visitor) {
        visitor.visit(this);
    }
}