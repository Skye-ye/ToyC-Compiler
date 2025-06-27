package toyc.ir.instruction;

import toyc.ir.InstructionVisitor;
import toyc.ir.value.Value;

public class ReturnInstruction extends Instruction {
    private final Value value;
    
    public ReturnInstruction(Value value) {
        super();
        this.value = value;
    }
    
    public Value getValue() {
        return value;
    }
    
    @Override
    public String toString() {
        return value != null ? "ret " + value : "ret";
    }
    
    @Override
    public void accept(InstructionVisitor visitor) {
        visitor.visit(this);
    }
}