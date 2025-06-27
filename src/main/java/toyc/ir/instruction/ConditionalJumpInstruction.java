package toyc.ir.instruction;

import toyc.ir.InstructionVisitor;
import toyc.ir.Label;
import toyc.ir.value.Value;

public class ConditionalJumpInstruction extends Instruction {
    private final Value condition;
    private final Label target;
    
    public ConditionalJumpInstruction(Value condition, Label target) {
        super();
        this.condition = condition;
        this.target = target;
    }
    
    public Value getCondition() {
        return condition;
    }
    
    public Label getTarget() {
        return target;
    }
    
    @Override
    public String toString() {
        return "if " + condition + " goto " + target;
    }
    
    @Override
    public void accept(InstructionVisitor visitor) {
        visitor.visit(this);
    }
}