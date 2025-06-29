package toyc.ir;

import toyc.ir.instruction.Instruction;
import toyc.ir.util.CounterManager;

import java.util.*;

public class BasicBlock {
    private final int id;
    private String name;
    private String descriptiveLabel;
    private final List<Instruction> instructions;
    private final Set<BasicBlock> predecessors;
    private final Set<BasicBlock> successors;
    private Label label;
    
    public BasicBlock(String descriptiveLabel) {
        this.id = CounterManager.nextBlockId();
        this.name = "B" + id;
        this.descriptiveLabel = descriptiveLabel;
        this.instructions = new ArrayList<>();
        this.predecessors = new HashSet<>();
        this.successors = new HashSet<>();
    }
    
    public BasicBlock() {
        this(null);
    }
    
    public int getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
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
    
    public String getDescriptiveLabel() {
        return descriptiveLabel;
    }
    
    public void setDescriptiveLabel(String descriptiveLabel) {
        this.descriptiveLabel = descriptiveLabel;
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
}