package toyc.util.dotgen;

import toyc.ir.BasicBlock;
import toyc.ir.ControlFlowGraph;
import toyc.ir.instruction.CallInstruction;
import toyc.ir.instruction.ConditionalJumpInstruction;
import toyc.ir.instruction.Instruction;
import toyc.ir.instruction.JumpInstruction;
import toyc.ir.instruction.ReturnInstruction;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ICFGGenerator {
    
    public static void generateICFGFile(Map<String, ControlFlowGraph> functions, String outputPath) throws IOException {
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(Path.of(outputPath)))) {
            generateICFG(functions, writer);
        }
    }
    
    private static void generateICFG(Map<String, ControlFlowGraph> functions, PrintWriter writer) {
        writer.println("digraph G {");
        writer.println("  node [shape=box, color=\".3 .2 1.0\", style=filled];");
        writer.println();
        
        int globalNodeId = 0;
        Map<String, FunctionInfo> functionInfoMap = new HashMap<>();
        
        // First pass: create all nodes and build mapping
        for (ControlFlowGraph cfg : functions.values()) {
            FunctionInfo funcInfo = new FunctionInfo();
            funcInfo.entryNodeId = globalNodeId++;
            funcInfo.exitNodeId = -1; // Will be set when we find the exit
            
            // Generate entry node
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
                        String label;
                        
                        if (instruction instanceof CallInstruction) {
                            // Special formatting for call instructions
                            String funcName = cfg.getFunctionName();
                            label = instructionIndex + ": <" + funcName + "> " + instruction;
                        } else {
                            label = instructionIndex + ": " + instruction.toString();
                        }
                        
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
            
            functionInfoMap.put(cfg.getFunctionName(), funcInfo);
        }
        
        writer.println();
        
        // Second pass: generate edges based on CFG structure
        for (ControlFlowGraph cfg : functions.values()) {
            generateICFGEdges(cfg, functionInfoMap, writer);
        }
        
        writer.println("}");
    }
    
    private static void generateICFGEdges(ControlFlowGraph cfg, Map<String, FunctionInfo> functionInfoMap, PrintWriter writer) {
        FunctionInfo funcInfo = functionInfoMap.get(cfg.getFunctionName());
        
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
                
                if (current.instruction instanceof CallInstruction callInstr) {
                    // Handle call instruction
                    String targetFunctionName = callInstr.getFunctionName();
                    FunctionInfo targetFuncInfo = functionInfoMap.get(targetFunctionName);
                    
                    if (targetFuncInfo != null) {
                        // Edge to called function (blue dashed)
                        writer.println("  \"" + current.nodeId + "\" -> \"" + targetFuncInfo.entryNodeId + "\" [color=blue, style=dashed];");
                        
                        // Return edge from called function exit (red dashed)
                        writer.println("  \"" + targetFuncInfo.exitNodeId + "\" -> \"" + next.nodeId + "\" [color=red, style=dashed];");
                        
                        // Also create a dashed edge to next instruction (call continues)
                        writer.println("  \"" + current.nodeId + "\" -> \"" + next.nodeId + "\" [style=dashed];");
                    } else {
                        // Regular sequential edge if function not found
                        writer.println("  \"" + current.nodeId + "\" -> \"" + next.nodeId + "\" [];");
                    }
                } else {
                    // Regular sequential edge
                    writer.println("  \"" + current.nodeId + "\" -> \"" + next.nodeId + "\" [];");
                }
            }
        }
        
        // Edges between blocks (control flow)
        for (BasicBlock block : cfg.getBlocks()) {
            InstructionNode lastNode = blockToLastNode.get(block);
            if (lastNode == null) continue;
            
            if (lastNode.instruction instanceof ReturnInstruction) {
                // Return instruction connects to exit
                writer.println("  \"" + lastNode.nodeId + "\" -> \"" + funcInfo.exitNodeId + "\" [];");
            } else {
                // Use CFG successors for branching
                for (BasicBlock successor : block.getSuccessors()) {
                    InstructionNode firstSuccessorNode = blockToFirstNode.get(successor);
                    if (firstSuccessorNode != null) {
                        writer.println("  \"" + lastNode.nodeId + "\" -> \"" + firstSuccessorNode.nodeId + "\" [];");
                    }
                }
            }
        }
    }
    
    private static String buildFunctionSignature(ControlFlowGraph cfg) {
        StringBuilder signature = new StringBuilder();
        
        // Add return type
        String returnType = cfg.getReturnType();
        if (returnType != null) {
            signature.append(returnType).append(" ");
        } else {
            signature.append("int "); // Default return type
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