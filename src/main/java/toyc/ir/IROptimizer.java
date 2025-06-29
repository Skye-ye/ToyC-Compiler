package toyc.ir;

import toyc.ir.instruction.*;

import java.util.*;
import java.util.Queue;
import java.util.LinkedList;

public class IROptimizer {
    
    public static void optimizeControlFlow(ControlFlowGraph cfg) {
        boolean changed;
        int iterations = 0;
        do {
            // Multiple passes of different optimizations
            changed = eliminateJumpOnlyBlocks(cfg);
            changed |= removeEmptyBlocks(cfg);
            changed |= removeUnreachableBlocks(cfg);
            iterations++;
        } while (changed && iterations < 10);
        
        // Final pass: renumber blocks to have consecutive IDs
        renumberBlocks(cfg);
    }
    
    /**
     * Eliminate blocks that contain only unconditional jumps
     */
    private static boolean eliminateJumpOnlyBlocks(ControlFlowGraph cfg) {
        boolean changed = false;
        Map<Label, Label> redirectMap = new HashMap<>();
        Set<BasicBlock> blocksToRemove = new HashSet<>();
        
        // First pass: identify jump-only blocks and build redirect map
        for (BasicBlock block : cfg.getBlocks()) {
            if (block == cfg.getEntryBlock() || block == cfg.getExitBlock()) continue;
            
            if (isJumpOnlyBlock(block)) {
                JumpInstruction jump = (JumpInstruction) block.getInstructions().getFirst();
                redirectMap.put(block.getLabel(), jump.getTarget());
                blocksToRemove.add(block);
            }
        }
        
        // Resolve chains in redirect map (A->B->C becomes A->C)
        for (Map.Entry<Label, Label> entry : redirectMap.entrySet()) {
            Label finalTarget = resolveFinalTarget(entry.getValue(), redirectMap, new HashSet<>());
            if (!finalTarget.equals(entry.getValue())) {
                entry.setValue(finalTarget);
            }
        }
        
        // Second pass: update all jump instructions to use final targets
        for (BasicBlock block : cfg.getBlocks()) {
            if (blocksToRemove.contains(block)) continue;
            
            for (int i = 0; i < block.getInstructions().size(); i++) {
                Instruction instr = block.getInstructions().get(i);
                Instruction newInstr = redirectInstruction(instr, redirectMap);
                if (newInstr != instr) {
                    block.getInstructions().set(i, newInstr);
                    changed = true;
                }
            }
        }
        
        // Third pass: remove the jump-only blocks and clean up mappings
        for (BasicBlock block : blocksToRemove) {
            cfg.getBlocks().remove(block);
        }

        if (!blocksToRemove.isEmpty()) {
            changed = true;
        }
        
        return changed;
    }
    
    private static boolean isJumpOnlyBlock(BasicBlock block) {
        return block.getInstructions().size() == 1 && 
               block.getInstructions().getFirst() instanceof JumpInstruction;
    }
    
    private static Label resolveFinalTarget(Label target, Map<Label, Label> redirectMap, Set<Label> visited) {
        if (visited.contains(target)) {
            return target; // Cycle detected, return current target
        }
        
        Label redirect = redirectMap.get(target);
        if (redirect != null) {
            visited.add(target);
            return resolveFinalTarget(redirect, redirectMap, visited);
        }
        
        return target;
    }
    
    private static Instruction redirectInstruction(Instruction instr, Map<Label, Label> redirectMap) {
        if (instr instanceof JumpInstruction jump) {
            Label newTarget = redirectMap.get(jump.getTarget());
            if (newTarget != null) {
                return new JumpInstruction(newTarget);
            }
        } else if (instr instanceof ConditionalJumpInstruction condJump) {
            Label newTarget = redirectMap.get(condJump.getTarget());
            if (newTarget != null) {
                return new ConditionalJumpInstruction(condJump.getCondition(), newTarget);
            }
        }
        return instr;
    }
    
    private static boolean removeEmptyBlocks(ControlFlowGraph cfg) {
        boolean changed = false;
        List<BasicBlock> blocksToRemove = new ArrayList<>();
        
        for (BasicBlock block : cfg.getBlocks()) {
            // Don't remove entry or exit blocks, even if empty
            if (block == cfg.getEntryBlock() || block == cfg.getExitBlock()) {
                continue;
            }
            
            // Remove completely empty blocks
            if (block.isEmpty()) {
                blocksToRemove.add(block);
            }
        }
        
        for (BasicBlock block : blocksToRemove) {
            cfg.getBlocks().remove(block);
            changed = true;
        }
        
        return changed;
    }
    
    private static boolean removeUnreachableBlocks(ControlFlowGraph cfg) {
        Set<BasicBlock> reachable = new HashSet<>();
        Set<BasicBlock> visited = new HashSet<>();
        
        if (cfg.getEntryBlock() != null) {
            markReachableBlocks(cfg.getEntryBlock(), cfg, reachable, visited);
        }
        
        List<BasicBlock> unreachable = new ArrayList<>();
        for (BasicBlock block : cfg.getBlocks()) {
            if (!reachable.contains(block)) {
                unreachable.add(block);
            }
        }
        
        cfg.getBlocks().removeAll(unreachable);
        return !unreachable.isEmpty();
    }
    
    private static void markReachableBlocks(BasicBlock block, ControlFlowGraph cfg, 
                                           Set<BasicBlock> reachable, Set<BasicBlock> visited) {
        if (visited.contains(block)) return;
        
        visited.add(block);
        reachable.add(block);
        
        // Follow all jump targets
        for (Instruction instr : block.getInstructions()) {
            if (instr instanceof JumpInstruction jump) {
                BasicBlock target = cfg.getBlockByLabel(jump.getTarget());
                if (target != null) {
                    markReachableBlocks(target, cfg, reachable, visited);
                }
            } else if (instr instanceof ConditionalJumpInstruction condJump) {
                BasicBlock target = cfg.getBlockByLabel(condJump.getTarget());
                if (target != null) {
                    markReachableBlocks(target, cfg, reachable, visited);
                }
            }
        }
    }
    
    /**
     * Renumber blocks to have consecutive IDs starting from B0 in BFS order
     */
    private static void renumberBlocks(ControlFlowGraph cfg) {
        if (cfg.getEntryBlock() == null) {
            return;
        }
        
        // Perform BFS traversal from entry block
        List<BasicBlock> orderedBlocks = new ArrayList<>();
        Set<BasicBlock> visited = new HashSet<>();
        Queue<BasicBlock> queue = new LinkedList<>();
        
        // Start BFS from entry block
        queue.offer(cfg.getEntryBlock());
        visited.add(cfg.getEntryBlock());
        
        while (!queue.isEmpty()) {
            BasicBlock current = queue.poll();
            orderedBlocks.add(current);
            
            // Add successors based on control flow instructions
            addSuccessorsInOrder(current, cfg, queue, visited);
        }
        
        // Add any remaining blocks that weren't reachable from entry
        for (BasicBlock block : cfg.getBlocks()) {
            if (!visited.contains(block)) {
                orderedBlocks.add(block);
            }
        }
        
        // Renumber blocks consecutively in BFS order
        for (int i = 0; i < orderedBlocks.size(); i++) {
            BasicBlock block = orderedBlocks.get(i);
            block.setName("B" + i);
        }
    }
    
    /**
     * Add successors to BFS queue in a deterministic order
     */
    private static void addSuccessorsInOrder(BasicBlock block, ControlFlowGraph cfg, 
                                           Queue<BasicBlock> queue, Set<BasicBlock> visited) {
        List<BasicBlock> successors = new ArrayList<>();
        
        // Follow control flow instructions to determine successors
        for (Instruction instr : block.getInstructions()) {
            if (instr instanceof JumpInstruction jump) {
                BasicBlock target = cfg.getBlockByLabel(jump.getTarget());
                if (target != null && !visited.contains(target)) {
                    successors.add(target);
                }
            } else if (instr instanceof ConditionalJumpInstruction condJump) {
                BasicBlock target = cfg.getBlockByLabel(condJump.getTarget());
                if (target != null && !visited.contains(target)) {
                    successors.add(target);
                }
            }
        }
        
        // Add successors to queue and mark as visited
        for (BasicBlock successor : successors) {
            if (!visited.contains(successor)) {
                queue.offer(successor);
                visited.add(successor);
            }
        }
    }
}