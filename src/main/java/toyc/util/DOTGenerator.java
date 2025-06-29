package toyc.util;

import toyc.ir.BasicBlock;
import toyc.ir.ControlFlowGraph;
import toyc.ir.instruction.Instruction;
import toyc.ir.instruction.JumpInstruction;
import toyc.ir.instruction.ConditionalJumpInstruction;
import toyc.ir.instruction.ReturnInstruction;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class DOTGenerator {
    
    public static void generateDOTFile(ControlFlowGraph cfg, String outputPath) throws IOException {
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(Path.of(outputPath)))) {
            generateDOT(cfg, writer);
        }
    }
    
    private static void generateDOT(ControlFlowGraph cfg, PrintWriter writer) {
        writer.println("digraph \"" + escapeDOTString(cfg.getFunctionName()) + "\" {");
        writer.println("  node [shape=box, style=filled, color=\".3 .2 1.0\"];");
        writer.println("  edge [fontsize=8];");
        writer.println();
        
        // Generate Entry node with function signature
        String functionSignature = buildFunctionSignature(cfg);
        writer.println("  \"Entry\" [label=\"Entry<" + escapeDOTString(functionSignature) + ">\"];");
        
        // Generate all basic blocks that are in the optimized CFG
        for (BasicBlock block : cfg.getBlocks()) {
            generateBlockNode(block, writer);
        }
        
        // Generate Exit node with function signature
        writer.println("  \"Exit\" [label=\"Exit<" + escapeDOTString(functionSignature) + ">\"];");
        
        writer.println();
        
        // Generate entry edge
        if (cfg.getEntryBlock() != null) {
            writer.println("  \"Entry\" -> \"" + escapeDOTString(cfg.getEntryBlock().getName()) + "\" [label=\"ENTRY\"];");
        }
        
        // Generate edges based on actual jump instructions  
        for (BasicBlock block : cfg.getBlocks()) {
            generateBlockEdgesFromInstructions(block, writer, cfg);
        }
        
        writer.println("}");
    }
    
    private static void generateBlockNode(BasicBlock block, PrintWriter writer) {
        StringBuilder label = new StringBuilder();
        label.append(block.getName());
        
        if (!block.getInstructions().isEmpty()) {
            label.append("\\n\\n");
            boolean firstInstruction = true;
            for (Instruction inst : block.getInstructions()) {
                // Skip goto and conditional jump instructions since control flow is shown by edges
                String instStr = inst.toString();
                if (instStr.startsWith("goto ") || instStr.startsWith("if ")) {
                    continue;
                }
                
                if (!firstInstruction) {
                    label.append("\\n");
                }
                label.append(escapeDOTString(instStr));
                firstInstruction = false;
            }
        }
        
        writer.println("  \"" + escapeDOTString(block.getName()) + "\" [label=\"" + label + "\"];");
    }
    
    private static void generateBlockEdgesFromInstructions(BasicBlock block, PrintWriter writer, ControlFlowGraph cfg) {
        String blockName = escapeDOTString(block.getName());
        List<BasicBlock> blockList = cfg.getBlocks();
        List<Instruction> instructions = block.getInstructions();
        
        for (int i = 0; i < instructions.size(); i++) {
            Instruction instr = instructions.get(i);
            
            if (instr instanceof ConditionalJumpInstruction condJump) {
                // Handle conditional jump - true branch
                BasicBlock target = cfg.getBlockByLabel(condJump.getTarget());
                if (target != null && blockList.contains(target)) {
                    String targetName = escapeDOTString(target.getName());
                    String edgeAttributes = getEdgeAttributes(block, target, "IF_TRUE");
                    writer.println("  \"" + blockName + "\" -> \"" + targetName + "\" [" + edgeAttributes + "];");
                }
                
                // Look for the immediately following goto instruction for false branch
                if (i + 1 < instructions.size() && instructions.get(i + 1) instanceof JumpInstruction nextJump) {
                    BasicBlock fallTarget = cfg.getBlockByLabel(nextJump.getTarget());
                    if (fallTarget != null && blockList.contains(fallTarget)) {
                        String fallTargetName = escapeDOTString(fallTarget.getName());
                        String edgeAttributes = getEdgeAttributes(block, fallTarget, "IF_FALSE");
                        writer.println("  \"" + blockName + "\" -> \"" + fallTargetName + "\" [" + edgeAttributes + "];");
                    }
                    i++; // Skip the next goto instruction since we processed it
                }
            } else if (instr instanceof JumpInstruction jump) {
                // Only process standalone goto instructions (not those following conditional jumps)
                BasicBlock target = cfg.getBlockByLabel(jump.getTarget());
                if (target != null && blockList.contains(target)) {
                    String targetName = escapeDOTString(target.getName());
                    String edgeAttributes = getEdgeAttributes(block, target, "GOTO");
                    writer.println("  \"" + blockName + "\" -> \"" + targetName + "\" [" + edgeAttributes + "];");
                }
            } else if (instr instanceof ReturnInstruction) {
                writer.println("  \"" + blockName + "\" -> \"Exit\" [label=\"RETURN\"];");
            }
        }
    }
    
    private static String getEdgeAttributes(BasicBlock sourceBlock, BasicBlock targetBlock, String label) {
        StringBuilder attributes = new StringBuilder();
        attributes.append("label=\"").append(label).append("\"");
        
        // Check if this is a backward edge (source block ID > target block ID)
        int sourceId = extractBlockId(sourceBlock.getName());
        int targetId = extractBlockId(targetBlock.getName());
        
        if (sourceId > targetId) {
            // Backward edge: use different styling for cleaner routing
            attributes.append(", constraint=false, style=dashed, color=blue, tailport=s, headport=n");
        }
        
        return attributes.toString();
    }
    
    private static int extractBlockId(String blockName) {
        // Extract numeric ID from block name (e.g., "B2" -> 2)
        if (blockName.startsWith("B")) {
            try {
                return Integer.parseInt(blockName.substring(1));
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
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
}