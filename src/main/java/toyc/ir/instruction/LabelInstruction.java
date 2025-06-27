package toyc.ir.instruction;

import toyc.ir.InstructionVisitor;
import toyc.ir.Label;

public class LabelInstruction extends Instruction {
    private final Label label;
    
    public LabelInstruction(Label label) {
        super();
        this.label = label;
    }
    
    public Label getLabel() {
        return label;
    }
    
    @Override
    public String toString() {
        return label + ":";
    }
    
    @Override
    public void accept(InstructionVisitor visitor) {
        visitor.visit(this);
    }
}