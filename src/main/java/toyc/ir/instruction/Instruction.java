package toyc.ir.instruction;

import toyc.ir.InstructionVisitor;

public abstract class Instruction {
    private static int nextId = 0;
    private final int id;
    
    public Instruction() {
        this.id = nextId++;
    }
    
    public int getId() {
        return id;
    }
    
    public abstract String toString();
    
    public abstract void accept(InstructionVisitor visitor);
}