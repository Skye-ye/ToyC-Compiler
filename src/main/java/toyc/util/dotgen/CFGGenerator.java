package toyc.util.dotgen;

import toyc.ir.BasicBlock;
import toyc.ir.ControlFlowGraph;
import toyc.ir.instruction.Instruction;
import toyc.ir.instruction.JumpInstruction;
import toyc.ir.instruction.ConditionalJumpInstruction;
import toyc.ir.instruction.ReturnInstruction;
import toyc.ir.instruction.CallInstruction;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

public class CFGGenerator {
    
    public static void generateCFGFile(ControlFlowGraph cfg,
                                       String outputPath) throws IOException {
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(Path.of(outputPath)))) {
            generateCFGDOT(cfg, writer);
        }
    }
    
    private static void generateCFGDOT(ControlFlowGraph cfg,
                                       PrintWriter writer) {
        writer.println("digraph \"" + escapeDOTString(cfg.getFunctionName()) + "\" {");
        writer.println("  node [shape=box, style=filled, color=\".3 .2 1.0\"];");
        writer.println();
        
        int globalNodeId = 0;
        FunctionInfo funcInfo = new FunctionInfo();
        funcInfo.entryNodeId = globalNodeId++;
        funcInfo.exitNodeId = -1; // Will be set when we find the exit
        
        // Generate Entry node with function signature
        String functionSignature = buildFunctionSignature(cfg);
        writer.println("  \"" + funcInfo.entryNodeId + "\" [label=\"Entry<" + escapeDOTString(functionSignature) + ">\"];");
        
        // Generate nodes for each instruction (excluding control flow instructions)
        int instructionIndex = 0;
        for (BasicBlock block : cfg.getBlocks()) {
            for (Instruction instruction : block.getInstructions()) {
                // Skip control flow instructions (goto, if goto) - they become edges
                if (instruction instanceof JumpInstruction || instruction instanceof ConditionalJumpInstruction) {
                    instructionIndex++;
                    continue;
                }
                
                if (instruction instanceof ReturnInstruction) {
                    // Create exit node if this is a return
                    if (funcInfo.exitNodeId == -1) {
                        funcInfo.exitNodeId = globalNodeId++;
                        writer.println("  \"" + funcInfo.exitNodeId + "\" [label=\"Exit<" + escapeDOTString(functionSignature) + ">\"];");
                    }
                    
                    // Create return instruction node
                    int returnNodeId = globalNodeId++;
                    String returnLabel = instructionIndex + ": " + instruction;
                    writer.println("  \"" + returnNodeId + "\" [label=\"" + escapeDOTString(returnLabel) + "\"];");
                    
                    funcInfo.instructionNodes.add(new InstructionNode(returnNodeId, instruction, instructionIndex, block));
                } else {
                    int nodeId = globalNodeId++;
                    String label = instructionIndex + ": " + instruction.toString();
                    
                    writer.println("  \"" + nodeId + "\" [label=\"" + escapeDOTString(label) + "\"];");
                    funcInfo.instructionNodes.add(new InstructionNode(nodeId, instruction, instructionIndex, block));
                }
                instructionIndex++;
            }
        }
        
        // If no return instruction found, create exit node
        if (funcInfo.exitNodeId == -1) {
            funcInfo.exitNodeId = globalNodeId++;
            writer.println("  \"" + funcInfo.exitNodeId + "\" [label=\"Exit<" + escapeDOTString(functionSignature) + ">\"];");
        }
        
        writer.println();
        
        // Generate edges based on CFG structure
        generateCFGEdges(cfg, funcInfo, writer);
        
        writer.println("}");
    }
    
    private static void generateCFGEdges(ControlFlowGraph cfg, FunctionInfo funcInfo, PrintWriter writer) {
        // Build mapping from blocks to their first instruction nodes
        Map<BasicBlock, InstructionNode> blockToFirstNode = new HashMap<>();
        Map<BasicBlock, InstructionNode> blockToLastNode = new HashMap<>();
        
        for (InstructionNode node : funcInfo.instructionNodes) {
            BasicBlock block = node.block;
            if (!blockToFirstNode.containsKey(block)) {
                blockToFirstNode.put(block, node);
            }
            blockToLastNode.put(block, node);
        }
        
        // Entry to first instruction of entry block
        BasicBlock entryBlock = cfg.getEntryBlock();
        if (entryBlock != null && blockToFirstNode.containsKey(entryBlock)) {
            writer.println("  \"" + funcInfo.entryNodeId + "\" -> \"" + blockToFirstNode.get(entryBlock).nodeId + "\" [];");
        }
        
        // Edges within blocks (sequential flow)
        for (BasicBlock block : cfg.getBlocks()) {
            List<InstructionNode> blockNodes = new ArrayList<>();
            for (InstructionNode node : funcInfo.instructionNodes) {
                if (node.block == block) {
                    blockNodes.add(node);
                }
            }
            
            // Sort by instruction index
            blockNodes.sort((a, b) -> Integer.compare(a.instructionIndex, b.instructionIndex));
            
            // Connect sequential instructions within the block
            for (int i = 0; i < blockNodes.size() - 1; i++) {
                InstructionNode current = blockNodes.get(i);
                InstructionNode next = blockNodes.get(i + 1);
                writer.println("  \"" + current.nodeId + "\" -> \"" + next.nodeId + "\" [];");
            }
        }
        
        // Edges between blocks (control flow)
        for (BasicBlock block : cfg.getBlocks()) {
            InstructionNode lastNode = blockToLastNode.get(block);
            if (lastNode == null) continue;
            
            if (lastNode.instruction instanceof ReturnInstruction) {
                // Return instruction connects to exit
                writer.println("  \"" + lastNode.nodeId + "\" -> \"" + funcInfo.exitNodeId + "\" [label=\"RETURN\"];");
            } else {
                // Analyze the block's control flow instructions to determine edge labels
                generateControlFlowEdges(block, lastNode, blockToFirstNode, cfg, writer);
            }
        }
    }
    
    private static void generateControlFlowEdges(BasicBlock block, InstructionNode lastNode, 
                                                Map<BasicBlock, InstructionNode> blockToFirstNode, 
                                                ControlFlowGraph cfg, PrintWriter writer) {
        List<Instruction> instructions = block.getInstructions();
        
        // Look for control flow instructions in this block
        for (int i = 0; i < instructions.size(); i++) {
            Instruction instr = instructions.get(i);
            
            if (instr instanceof ConditionalJumpInstruction condJump) {
                // Handle conditional jump - true branch
                BasicBlock trueTarget = cfg.getBlockByLabel(condJump.getTarget());
                if (trueTarget != null && blockToFirstNode.containsKey(trueTarget)) {
                    InstructionNode firstTrueNode = blockToFirstNode.get(trueTarget);
                    writer.println("  \"" + lastNode.nodeId + "\" -> \"" + firstTrueNode.nodeId + "\" [label=\"IF_TRUE\"];");
                }
                
                // Look for the immediately following goto instruction for false branch
                if (i + 1 < instructions.size() && instructions.get(i + 1) instanceof JumpInstruction nextJump) {
                    BasicBlock falseTarget = cfg.getBlockByLabel(nextJump.getTarget());
                    if (falseTarget != null && blockToFirstNode.containsKey(falseTarget)) {
                        InstructionNode firstFalseNode = blockToFirstNode.get(falseTarget);
                        writer.println("  \"" + lastNode.nodeId + "\" -> \"" + firstFalseNode.nodeId + "\" [label=\"IF_FALSE\"];");
                    }
                    return; // We've handled both branches
                }
            } else if (instr instanceof JumpInstruction jump) {
                // Handle unconditional jump
                BasicBlock target = cfg.getBlockByLabel(jump.getTarget());
                if (target != null && blockToFirstNode.containsKey(target)) {
                    InstructionNode firstTargetNode = blockToFirstNode.get(target);
                    writer.println("  \"" + lastNode.nodeId + "\" -> \"" + firstTargetNode.nodeId + "\" [label=\"GOTO\"];");
                }
                return; // We've handled the jump
            }
        }
        
        // If no control flow instructions found, use fall-through edges without labels
        for (BasicBlock successor : block.getSuccessors()) {
            InstructionNode firstSuccessorNode = blockToFirstNode.get(successor);
            if (firstSuccessorNode != null) {
                writer.println("  \"" + lastNode.nodeId + "\" -> \"" + firstSuccessorNode.nodeId + "\" [];");
            }
        }
    }
    
    private static String buildFunctionSignature(ControlFlowGraph cfg) {
        StringBuilder signature = new StringBuilder();
        
        // Add return type
        String returnType = cfg.getReturnType();
        if (returnType != null) {
            signature.append(returnType).append(" ");
        }
        
        // Add function name
        signature.append(cfg.getFunctionName());
        
        // Add parameters
        signature.append("(");
        List<String> params = cfg.getParameterNames();
        for (int i = 0; i < params.size(); i++) {
            if (i > 0) {
                signature.append(", ");
            }
            signature.append("int ").append(params.get(i)); // ToyC only has int parameters
        }
        signature.append(")");
        
        return signature.toString();
    }
    
    private static String escapeDOTString(String str) {
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
    
    private static class FunctionInfo {
        int entryNodeId;
        int exitNodeId;
        List<InstructionNode> instructionNodes = new ArrayList<>();
    }
    
    private static class InstructionNode {
        int nodeId;
        Instruction instruction;
        int instructionIndex;
        BasicBlock block;
        
        InstructionNode(int nodeId, Instruction instruction, int instructionIndex, BasicBlock block) {
            this.nodeId = nodeId;
            this.instruction = instruction;
            this.instructionIndex = instructionIndex;
            this.block = block;
        }
    }
}