package toyc.ir.instruction;

import toyc.ir.InstructionVisitor;
import toyc.ir.value.Value;
import toyc.ir.util.OperatorUtils;

public class BinaryOpInstruction extends Instruction {
    public enum BinaryOp {
        ADD, SUB, MUL, DIV, MOD,
        LT, GT, LE, GE, EQ, NEQ,
        AND, OR
    }
    
    private final Value result;
    private final Value left;
    private final BinaryOp operator;
    private final Value right;
    
    public BinaryOpInstruction(Value result, Value left, BinaryOp operator, Value right) {
        super();
        this.result = result;
        this.left = left;
        this.operator = operator;
        this.right = right;
    }
    
    public Value getResult() {
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
        return result + " = " + left + " " + OperatorUtils.operatorToString(operator) + " " + right;
    }
    
    @Override
    public void accept(InstructionVisitor visitor) {
        visitor.visit(this);
    }
}