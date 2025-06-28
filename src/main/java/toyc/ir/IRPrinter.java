package toyc.ir;

import toyc.ir.instruction.*;
import toyc.ir.value.Temporary;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

public class IRPrinter implements InstructionVisitor {
    private final StringBuilder output;
    private final int indentLevel;
    private int tempCounter;
    private final Map<String, String> tempRenameMap;
    
    public IRPrinter() {
        this.output = new StringBuilder();
        this.indentLevel = 0;
        this.tempCounter = 0;
        this.tempRenameMap = new HashMap<>();
    }
    
    public String printProgram(Map<String, ControlFlowGraph> functions) {
        output.setLength(0);
        
        // Reset counters at the beginning of program printing
        Temporary.resetCounter();
        BasicBlock.resetCounter();
        
        output.append("=== ToyC Intermediate Representation ===\n\n");
        
        for (ControlFlowGraph cfg : functions.values()) {
            printFunction(cfg);
            output.append("\n");
        }
        
        return output.toString();
    }
    
    public String printFunction(ControlFlowGraph cfg) {
        // Reset temporary counter and rename map for each function
        tempCounter = 0;
        tempRenameMap.clear();
        Temporary.resetCounter();
        
        // Print LLVM-style function definition
        String functionName = cfg.getFunctionName();
        String returnType = "i32"; // Default to i32
        String parameters = buildParameters(cfg);
        
        output.append("define ").append(returnType).append(" @")
              .append(functionName).append("(").append(parameters).append(") {\n");
        
        // Use the blocks list directly to ensure we only print blocks that still exist
        List<BasicBlock> blocksToProcess = new ArrayList<>();
        for (BasicBlock block : cfg.getBlocks()) {
            // Skip empty exit blocks
            if (block == cfg.getExitBlock() && block.isEmpty()) {
                continue;
            }
            blocksToProcess.add(block);
        }
        
        for (int i = 0; i < blocksToProcess.size(); i++) {
            boolean isLastBlock = (i == blocksToProcess.size() - 1);
            printBasicBlock(blocksToProcess.get(i), functionName, isLastBlock);
        }
        
        output.append("}\n");
        return output.toString();
    }
    
    private String buildParameters(ControlFlowGraph cfg) {
        List<String> paramNames = cfg.getParameterNames();
        if (paramNames.isEmpty()) {
            return "";
        }
        
        StringBuilder params = new StringBuilder();
        for (int i = 0; i < paramNames.size(); i++) {
            if (i > 0) {
                params.append(", ");
            }
            params.append("i32 ").append(paramNames.get(i));
        }
        return params.toString();
    }
    
    private void printBasicBlock(BasicBlock block, String functionName, boolean isLastBlock) {
        String blockName = block.getName();
        
        // Convert first block to functionEntry format
        if (blockName.equals("BB0") || blockName.equals(functionName)) {
            blockName = functionName + "Entry";
        }
        
        output.append(blockName).append(":\n");
        
        for (Instruction instruction : block.getInstructions()) {
            indent();
            instruction.accept(this);
            output.append("\n");
        }
        
        // Only add newline if not the last block
        if (!isLastBlock) {
            output.append("\n");
        }
    }
    
    private void indent() {
        output.append("  ".repeat(Math.max(0, indentLevel + 1)));
    }
    
    private String renameTemporary(String originalName) {
        // If it's already a temporary variable (starts with 't' and followed by numbers)
        if (originalName.matches("t\\d+")) {
            return tempRenameMap.computeIfAbsent(originalName, k -> "t" + tempCounter++);
        }
        return originalName;
    }
    
    private String getOperatorString(BinaryOpInstruction.BinaryOp operator) {
        return switch (operator) {
            case ADD -> "+";
            case SUB -> "-";
            case MUL -> "*";
            case DIV -> "/";
            case MOD -> "%";
            case LT -> "<";
            case GT -> ">";
            case LE -> "<=";
            case GE -> ">=";
            case EQ -> "==";
            case NEQ -> "!=";
            case AND -> "&&";
            case OR -> "||";
        };
    }
    
    @Override
    public void visit(AssignInstruction instruction) {
        String result = renameTemporary(instruction.getTarget().getName());
        String source = renameTemporary(instruction.getSource().toString());
        output.append(result).append(" = ").append(source);
    }
    
    @Override
    public void visit(BinaryOpInstruction instruction) {
        String result = renameTemporary(instruction.getResult().getName());
        String left = renameTemporary(instruction.getLeft().toString());
        String right = renameTemporary(instruction.getRight().toString());
        String op = getOperatorString(instruction.getOperator());
        output.append(result).append(" = ").append(left).append(" ").append(op).append(" ").append(right);
    }
    
    @Override
    public void visit(UnaryOpInstruction instruction) {
        output.append(instruction.toString());
    }
    
    @Override
    public void visit(CallInstruction instruction) {
        if (instruction.getResult() != null) {
            String result = renameTemporary(instruction.getResult().getName());
            output.append(result).append(" = ");
        }
        output.append("call ").append(instruction.getFunctionName()).append("(");
        for (int i = 0; i < instruction.getArguments().size(); i++) {
            if (i > 0) output.append(", ");
            String arg = renameTemporary(instruction.getArguments().get(i).toString());
            output.append(arg);
        }
        output.append(")");
    }
    
    @Override
    public void visit(ReturnInstruction instruction) {
        output.append("ret");
        if (instruction.getValue() != null) {
            String value = renameTemporary(instruction.getValue().toString());
            output.append(" ").append(value);
        }
    }
    
    @Override
    public void visit(JumpInstruction instruction) {
        output.append("goto ").append(instruction.getTarget().getName());
    }
    
    @Override
    public void visit(ConditionalJumpInstruction instruction) {
        String condition = renameTemporary(instruction.getCondition().toString());
        output.append("if ").append(condition).append(" goto ").append(instruction.getTarget().getName());
    }
    
    @Override
    public void visit(LabelInstruction instruction) {
        output.append(instruction.toString());
    }
}