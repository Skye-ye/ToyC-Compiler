package toyc.ir.instruction;

import toyc.ir.InstructionVisitor;
import toyc.ir.value.Value;
public class UnaryOpInstruction extends Instruction {
    public enum UnaryOp {
        PLUS, MINUS, NOT
    }
    
    private final Value result;
    private final UnaryOp operator;
    private final Value operand;
    
    public UnaryOpInstruction(Value result, UnaryOp operator, Value operand) {
        super();
        this.result = result;
        this.operator = operator;
        this.operand = operand;
    }
    
    public Value getResult() {
        return result;
    }
    
    public UnaryOp getOperator() {
        return operator;
    }
    
    public Value getOperand() {
        return operand;
    }
    
    @Override
    public String toString() {
        return result + " = " + operatorToString() + operand;
    }
    
    private String operatorToString() {
        return switch (operator) {
            case PLUS -> "+";
            case MINUS -> "-";
            case NOT -> "!";
        };
    }
    
    @Override
    public void accept(InstructionVisitor visitor) {
        visitor.visit(this);
    }
}