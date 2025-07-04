package toyc.ir;

import java.util.*;

public class ControlFlowGraph {
    private final String functionName;
    private final List<BasicBlock> blocks;
    private BasicBlock entryBlock;
    private BasicBlock exitBlock;
    private final Map<Label, BasicBlock> labelToBlock;
    private final List<String> parameterNames;
    private String returnType;
    
    public ControlFlowGraph(String functionName) {
        this.functionName = functionName;
        this.blocks = new ArrayList<>();
        this.labelToBlock = new HashMap<>();
        this.parameterNames = new ArrayList<>();
    }
    
    public String getFunctionName() {
        return functionName;
    }
    
    public List<String> getParameterNames() {
        return parameterNames;
    }
    
    public void addParameterName(String paramName) {
        parameterNames.add(paramName);
    }
    
    public String getReturnType() {
        return returnType;
    }
    
    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }
    
    public List<BasicBlock> getBlocks() {
        return blocks;
    }
    
    public BasicBlock getEntryBlock() {
        return entryBlock;
    }
    
    public void setEntryBlock(BasicBlock entryBlock) {
        this.entryBlock = entryBlock;
        if (!blocks.contains(entryBlock)) {
            blocks.add(entryBlock);
        }
    }
    
    public BasicBlock getExitBlock() {
        return exitBlock;
    }
    
    public void setExitBlock(BasicBlock exitBlock) {
        this.exitBlock = exitBlock;
        if (!blocks.contains(exitBlock)) {
            blocks.add(exitBlock);
        }
    }
    
    public void addBlock(BasicBlock block) {
        if (!blocks.contains(block)) {
            blocks.add(block);
        }
        if (block.getLabel() != null) {
            labelToBlock.put(block.getLabel(), block);
        }
    }
    
    public BasicBlock getBlockByLabel(Label label) {
        return labelToBlock.get(label);
    }
    
    public void mapLabelToBlock(Label label, BasicBlock block) {
        labelToBlock.put(label, block);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("CFG for function: ").append(functionName).append("\n");
        sb.append("Entry: ").append(entryBlock != null ? entryBlock.getName() : "null").append("\n");
        sb.append("Exit: ").append(exitBlock != null ? exitBlock.getName() : "null").append("\n\n");
        
        for (BasicBlock block : blocks) {
            sb.append(block.toString());
            if (!block.getSuccessors().isEmpty()) {
                sb.append("  Successors: ");
                for (BasicBlock successor : block.getSuccessors()) {
                    sb.append(successor.getName()).append(" ");
                }
                sb.append("\n");
            }
            sb.append("\n");
        }
        
        return sb.toString();
    }
}