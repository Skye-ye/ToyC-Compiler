package toyc.ir.instruction;

import toyc.ir.InstructionVisitor;

public abstract class Instruction {
    
    public Instruction() {
        // Base constructor for all instructions
    }
    
    public abstract String toString();
    
    public abstract void accept(InstructionVisitor visitor);
}