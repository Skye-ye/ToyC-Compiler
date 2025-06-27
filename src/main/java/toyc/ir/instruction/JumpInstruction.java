package toyc.ir.instruction;

import toyc.ir.InstructionVisitor;
import toyc.ir.Label;

public class JumpInstruction extends Instruction {
    private final Label target;
    
    public JumpInstruction(Label target) {
        super();
        this.target = target;
    }
    
    public Label getTarget() {
        return target;
    }
    
    @Override
    public String toString() {
        return "goto " + target;
    }
    
    @Override
    public void accept(InstructionVisitor visitor) {
        visitor.visit(this);
    }
}