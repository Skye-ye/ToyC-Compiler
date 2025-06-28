package toyc.ir;

import toyc.ir.instruction.Instruction;

import java.util.*;

public class BasicBlock {
    private static int nextId = 0;
    private final int id;
    private final String name;
    private final List<Instruction> instructions;
    private final Set<BasicBlock> predecessors;
    private final Set<BasicBlock> successors;
    private Label label;
    
    public BasicBlock(String name) {
        this.id = nextId++;
        this.name = name;
        this.instructions = new ArrayList<>();
        this.predecessors = new HashSet<>();
        this.successors = new HashSet<>();
    }
    
    public BasicBlock() {
        this("BB" + nextId);
    }
    
    public int getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public List<Instruction> getInstructions() {
        return instructions;
    }
    
    public void addInstruction(Instruction instruction) {
        instructions.add(instruction);
    }
    
    public void addInstructionAt(int index, Instruction instruction) {
        instructions.add(index, instruction);
    }
    
    public Set<BasicBlock> getPredecessors() {
        return predecessors;
    }
    
    public Set<BasicBlock> getSuccessors() {
        return successors;
    }
    
    public void addPredecessor(BasicBlock block) {
        predecessors.add(block);
    }
    
    public void addSuccessor(BasicBlock block) {
        successors.add(block);
        block.addPredecessor(this);
    }
    
    public Label getLabel() {
        return label;
    }
    
    public void setLabel(Label label) {
        this.label = label;
    }
    
    public boolean isEmpty() {
        return instructions.isEmpty();
    }
    
    public Instruction getLastInstruction() {
        return instructions.isEmpty() ? null : instructions.getLast();
    }
    
    public Instruction getFirstInstruction() {
        return instructions.isEmpty() ? null : instructions.getFirst();
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(":\n");
        for (Instruction instruction : instructions) {
            sb.append("  ").append(instruction).append("\n");
        }
        return sb.toString();
    }
    
    public static void resetCounter() {
        nextId = 0;
    }
}