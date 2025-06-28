package toyc.ir.instruction;

import toyc.ir.InstructionVisitor;
import toyc.ir.value.Value;
import toyc.ir.value.Variable;

public class AssignInstruction extends Instruction {
    private final Variable target;
    private final Value source;
    
    public AssignInstruction(Variable target, Value source) {
        super();
        this.target = target;
        this.source = source;
    }
    
    public Variable getTarget() {
        return target;
    }
    
    public Value getSource() {
        return source;
    }
    
    @Override
    public String toString() {
        return target + " = " + source;
    }
    
    @Override
    public void accept(InstructionVisitor visitor) {
        visitor.visit(this);
    }
}