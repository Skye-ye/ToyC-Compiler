package toyc.ir;

import toyc.ir.instruction.*;

import java.util.Map;

public class IRPrinter implements InstructionVisitor {
    private final StringBuilder output;
    private final int indentLevel;
    
    public IRPrinter() {
        this.output = new StringBuilder();
        this.indentLevel = 0;
    }
    
    public String printProgram(Map<String, ControlFlowGraph> functions) {
        output.setLength(0);
        
        output.append("=== ToyC Intermediate Representation ===\n\n");
        
        for (ControlFlowGraph cfg : functions.values()) {
            printFunction(cfg);
            output.append("\n");
        }
        
        return output.toString();
    }
    
    public String printFunction(ControlFlowGraph cfg) {
        // Use the blocks list directly to ensure we only print blocks that still exist
        for (BasicBlock block : cfg.getBlocks()) {
            // Skip empty exit blocks
            if (block == cfg.getExitBlock() && block.isEmpty()) {
                continue;
            }
            printBasicBlock(block);
        }
        
        return output.toString();
    }
    
    private void printBasicBlock(BasicBlock block) {
        output.append(block.getName()).append(":\n");
        
        for (Instruction instruction : block.getInstructions()) {
            indent();
            instruction.accept(this);
            output.append("\n");
        }
        
        output.append("\n");
    }
    
    private void indent() {
        output.append("  ".repeat(Math.max(0, indentLevel + 1)));
    }
    
    @Override
    public void visit(AssignInstruction instruction) {
        output.append(instruction.toString());
    }
    
    @Override
    public void visit(BinaryOpInstruction instruction) {
        output.append(instruction.toString());
    }
    
    @Override
    public void visit(UnaryOpInstruction instruction) {
        output.append(instruction.toString());
    }
    
    @Override
    public void visit(CallInstruction instruction) {
        output.append(instruction.toString());
    }
    
    @Override
    public void visit(ReturnInstruction instruction) {
        output.append(instruction.toString());
    }
    
    @Override
    public void visit(JumpInstruction instruction) {
        output.append(instruction.toString());
    }
    
    @Override
    public void visit(ConditionalJumpInstruction instruction) {
        output.append(instruction.toString());
    }
    
    @Override
    public void visit(LabelInstruction instruction) {
        output.append(instruction.toString());
    }
}