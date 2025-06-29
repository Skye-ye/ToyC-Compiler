package toyc.util;

import toyc.ir.BasicBlock;
import toyc.ir.ControlFlowGraph;
import toyc.ir.instruction.CallInstruction;
import toyc.ir.instruction.Instruction;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CGGenerator {
    
    public static void generateCGFile(Map<String, ControlFlowGraph> functions, String outputPath) throws IOException {
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(Path.of(outputPath)))) {
            generateCGDOT(functions, writer);
        }
    }
    
    private static void generateCGDOT(Map<String, ControlFlowGraph> functions,
                                      PrintWriter writer) {
        writer.println("digraph G {");
        writer.println("  node [shape=box, color=\".3 .2 1.0\", style=filled];");
        writer.println();
        
        // Generate function nodes
        int nodeId = 0;
        for (ControlFlowGraph cfg : functions.values()) {
            generateFunctionNode(cfg, nodeId, writer);
            nodeId++;
        }

        writer.println();

        // Generate call edges
        nodeId = 0;
        for (ControlFlowGraph cfg : functions.values()) {
            generateCallEdges(cfg, nodeId, functions, writer);
            nodeId++;
        }
        
        writer.println("}");
    }
    
    private static void generateFunctionNode(ControlFlowGraph cfg, int nodeId, PrintWriter writer) {
        String functionSignature = buildFunctionSignature(cfg);
        writer.println("  \"" + nodeId + "\" [label=\"<" + escapeDOTString(functionSignature) + ">\"];");
    }
    
    private static void generateCallEdges(ControlFlowGraph cfg, int sourceNodeId, Map<String, ControlFlowGraph> functions, PrintWriter writer) {
        // Find all call instructions in this function
        List<CallSite> callSites = findCallSites(cfg);
        
        // Group call sites by target function to avoid overlapping edges
        Map<String, List<CallSite>> callsByTarget = new HashMap<>();
        for (CallSite callSite : callSites) {
            callsByTarget.computeIfAbsent(callSite.targetFunction, k -> new ArrayList<>()).add(callSite);
        }
        
        for (Map.Entry<String, List<CallSite>> entry : callsByTarget.entrySet()) {
            String targetFunction = entry.getKey();
            List<CallSite> calls = entry.getValue();
            
            // Find target function node ID
            int targetNodeId = findFunctionNodeId(targetFunction, functions);
            if (targetNodeId != -1) {
                if (calls.size() == 1) {
                    // Single call - use simple label
                    String edgeLabel = formatCallSiteLabel(calls.getFirst());
                    String edgeAttributes = getEdgeAttributes(sourceNodeId, targetNodeId, edgeLabel);
                    writer.println("  \"" + sourceNodeId + "\" -> \"" + targetNodeId + "\" [" + edgeAttributes + "];");
                } else {
                    // Multiple calls to same function - combine them
                    String combinedLabel = formatMultipleCallsLabel(calls);
                    String edgeAttributes = getEdgeAttributes(sourceNodeId, targetNodeId, combinedLabel);
                    writer.println("  \"" + sourceNodeId + "\" -> \"" + targetNodeId + "\" [" + edgeAttributes + "];");
                }
            }
        }
    }
    
    private static List<CallSite> findCallSites(ControlFlowGraph cfg) {
        List<CallSite> callSites = new ArrayList<>();
        int instructionIndex = 0;
        
        for (BasicBlock block : cfg.getBlocks()) {
            for (Instruction instruction : block.getInstructions()) {
                if (instruction instanceof CallInstruction callInstr) {
                    CallSite callSite = new CallSite();
                    callSite.targetFunction = callInstr.getFunctionName();
                    callSite.instructionIndex = instructionIndex;
                    callSite.blockName = block.getName();
                    callSite.instruction = callInstr;
                    callSites.add(callSite);
                }
                instructionIndex++;
            }
        }
        
        return callSites;
    }
    
    private static int findFunctionNodeId(String functionName, Map<String, ControlFlowGraph> functions) {
        int nodeId = 0;
        for (ControlFlowGraph cfg : functions.values()) {
            if (cfg.getFunctionName().equals(functionName)) {
                return nodeId;
            }
            nodeId++;
        }
        return -1; // Function not found
    }
    
    private static String formatCallSiteLabel(CallSite callSite) {
        StringBuilder label = new StringBuilder();
        
        // Add the call instruction details without line/block info
        if (callSite.instruction.getResult() != null) {
            label.append(callSite.instruction.getResult()).append(" = ");
        }
        label.append("call ").append(callSite.targetFunction).append("(");
        
        for (int i = 0; i < callSite.instruction.getArguments().size(); i++) {
            if (i > 0) label.append(", ");
            label.append(callSite.instruction.getArguments().get(i));
        }
        label.append(")");
        
        return label.toString();
    }
    
    private static String formatMultipleCallsLabel(List<CallSite> calls) {
        if (calls.size() == 1) {
            return formatCallSiteLabel(calls.getFirst());
        }
        
        StringBuilder label = new StringBuilder();
        label.append("(").append(calls.size()).append(" calls)\\n");
        
        for (int i = 0; i < Math.min(calls.size(), 3); i++) { // Show max 3 calls
            if (i > 0) label.append("\\n");
            label.append(formatCallSiteLabel(calls.get(i)));
        }
        
        if (calls.size() > 3) {
            label.append("\\n...");
        }
        
        return label.toString();
    }
    
    private static String getEdgeAttributes(int sourceNodeId, int targetNodeId, String label) {
        StringBuilder attributes = new StringBuilder();
        attributes.append("label=\"").append(label).append("\"");
        
        // Special styling for recursive calls (self-loops)
        if (sourceNodeId == targetNodeId) {
            attributes.append(", style=dashed, color=red, penwidth=2");
        }
        
        return attributes.toString();
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
    
    private static class CallSite {
        String targetFunction;
        int instructionIndex;
        String blockName;
        CallInstruction instruction;
    }
}