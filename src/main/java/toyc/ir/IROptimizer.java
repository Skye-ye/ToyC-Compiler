package toyc.ir;

import toyc.ir.stmt.*;
import toyc.language.Function;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Optimizes IR by removing redundant instructions.
 */
public class IROptimizer {

    /**
     * Optimizes the given IR by removing redundant NOP instructions.
     * 
     * @param ir the IR to optimize
     * @return a new optimized IR
     */
    public static IR removeRedundantNops(IR ir) {
        List<Stmt> originalStmts = ir.getStmts();
        if (originalStmts.isEmpty()) {
            return ir;
        }

        // Build a map of statements that are targets of jumps
        Set<Stmt> jumpTargets = findJumpTargets(originalStmts);
        // Remove redundant NOPs while maintaining jump target integrity
        List<Stmt> optimizedStmts = new ArrayList<>();
        Map<Stmt, Stmt> stmtRedirections = new HashMap<>();
        
        for (int i = 0; i < originalStmts.size(); i++) {
            Stmt stmt = originalStmts.get(i);
            
            if (stmt instanceof Nop && !jumpTargets.contains(stmt)) {
                // This NOP is not a jump target, so it can be removed
                // Find the next non-NOP statement to redirect jumps to
                Stmt redirectTarget = findNextNonNopOrNull(originalStmts, i, jumpTargets);
                if (redirectTarget != null) {
                    stmtRedirections.put(stmt, redirectTarget);
                }
                // Don't add this NOP to optimized statements
                continue;
            }
            
            optimizedStmts.add(stmt);
        }
        
        // Update jump targets in the optimized statements
        updateJumpTargets(optimizedStmts, stmtRedirections);
        
        // Create a mapping from old statements to new statements for proper re-indexing
        List<Stmt> reindexedStmts = createReindexedStatements(optimizedStmts);
        
        // Create new optimized IR
        return new DefaultIR(
            ir.getFunction(),
            ir.getParams(),
            new HashSet<>(ir.getReturnVars()),
            ir.getVars(),
            reindexedStmts
        );
    }

    /**
     * Finds all statements that are targets of jump instructions.
     */
    private static Set<Stmt> findJumpTargets(List<Stmt> stmts) {
        Set<Stmt> targets = new HashSet<>();
        
        for (Stmt stmt : stmts) {
            if (stmt instanceof Goto gotoStmt) {
                Stmt target = gotoStmt.getTarget();
                if (target != null) {
                    targets.add(target);
                }
            } else if (stmt instanceof If ifStmt) {
                Stmt target = ifStmt.getTarget();
                if (target != null) {
                    targets.add(target);
                }
            }
        }
        
        return targets;
    }

    /**
     * Finds the next non-NOP statement starting from the given index,
     * or returns null if only NOPs remain.
     */
    private static Stmt findNextNonNopOrNull(List<Stmt> stmts, int startIndex, Set<Stmt> jumpTargets) {
        for (int i = startIndex + 1; i < stmts.size(); i++) {
            Stmt stmt = stmts.get(i);
            if (!(stmt instanceof Nop) || jumpTargets.contains(stmt)) {
                return stmt;
            }
        }
        return null; // No more statements or only NOPs remain
    }

    /**
     * Updates jump targets based on statement redirections.
     */
    private static void updateJumpTargets(List<Stmt> stmts, Map<Stmt, Stmt> redirections) {
        for (Stmt stmt : stmts) {
            if (stmt instanceof Goto gotoStmt) {
                Stmt originalTarget = gotoStmt.getTarget();
                Stmt newTarget = redirections.get(originalTarget);
                if (newTarget != null) {
                    gotoStmt.setTarget(newTarget);
                }
            } else if (stmt instanceof If ifStmt) {
                Stmt originalTarget = ifStmt.getTarget();
                Stmt newTarget = redirections.get(originalTarget);
                if (newTarget != null) {
                    ifStmt.setTarget(newTarget);
                }
            }
        }
    }

    // Old method removed - now using createReindexedStatements instead

    /**
     * Creates new statement objects with proper sequential indices.
     */
    private static List<Stmt> createReindexedStatements(List<Stmt> optimizedStmts) {
        List<Stmt> reindexedStmts = new ArrayList<>();
        Map<Stmt, Stmt> stmtMapping = new HashMap<>();
        
        // First pass: create new statement objects with correct indices
        for (int i = 0; i < optimizedStmts.size(); i++) {
            Stmt originalStmt = optimizedStmts.get(i);
            Stmt newStmt = cloneStatement(originalStmt);
            newStmt.setIndex(i);
            newStmt.setLineNumber(originalStmt.getLineNumber());
            reindexedStmts.add(newStmt);
            stmtMapping.put(originalStmt, newStmt);
        }
        
        // Second pass: update jump targets to point to new statement objects
        updateJumpTargetsToNewStatements(reindexedStmts, stmtMapping);
        
        return reindexedStmts;
    }

    /**
     * Creates a deep clone of a statement without the index.
     */
    private static Stmt cloneStatement(Stmt stmt) {
        switch (stmt) {
            case Nop ignored -> {
                return new Nop();
            }
            case Goto ignored -> {
                // Target will be updated in second pass
                return new Goto();
                // Target will be updated in second pass
            }
            case If ifStmt -> {
                // Target will be updated in second pass
                return new If(ifStmt.getCondition());
                // Target will be updated in second pass
            }
            case Return returnStmt -> {
                return new Return(returnStmt.getValue());
            }
            case Call call -> {
                return new Call(call.getContainer(), call.getCallExp(), call.getResult());
            }
            case AssignLiteral assignLit -> {
                return new AssignLiteral(assignLit.getLValue(), assignLit.getRValue());
            }
            case Binary binary -> {
                return new Binary(binary.getLValue(), binary.getRValue());
            }
            case Copy copy -> {
                return new Copy(copy.getLValue(), copy.getRValue());
            }
            case Unary unary -> {
                return new Unary(unary.getLValue(), unary.getRValue());
            }
            case null, default -> {
                // For any other statement types, return the original (fallback)
                return stmt;
            }
        }
    }

    /**
     * Updates jump targets in the new statements to point to the correct new statement objects.
     */
    private static void updateJumpTargetsToNewStatements(List<Stmt> newStmts, Map<Stmt, Stmt> stmtMapping) {
        for (Stmt stmt : newStmts) {
            if (stmt instanceof Goto gotoStmt) {
                // Find the original goto statement to get its target
                Stmt originalTarget = findOriginalTarget(stmt, stmtMapping, "goto");
                if (originalTarget != null) {
                    Stmt newTarget = stmtMapping.get(originalTarget);
                    if (newTarget != null) {
                        gotoStmt.setTarget(newTarget);
                    }
                }
            } else if (stmt instanceof If ifStmt) {
                // Find the original if statement to get its target
                Stmt originalTarget = findOriginalTarget(stmt, stmtMapping, "if");
                if (originalTarget != null) {
                    Stmt newTarget = stmtMapping.get(originalTarget);
                    if (newTarget != null) {
                        ifStmt.setTarget(newTarget);
                    }
                }
            }
        }
    }

    /**
     * Helper method to find the original target of a jump statement.
     */
    private static Stmt findOriginalTarget(Stmt newStmt, Map<Stmt, Stmt> stmtMapping, String stmtType) {
        // Find the original statement that corresponds to this new statement
        for (Map.Entry<Stmt, Stmt> entry : stmtMapping.entrySet()) {
            if (entry.getValue() == newStmt) {
                Stmt originalStmt = entry.getKey();
                if (originalStmt instanceof Goto gotoStmt && "goto".equals(stmtType)) {
                    return gotoStmt.getTarget();
                } else if (originalStmt instanceof If ifStmt && "if".equals(stmtType)) {
                    return ifStmt.getTarget();
                }
                break;
            }
        }
        return null;
    }

    /**
     * Optimizes all IRs by removing redundant NOPs.
     */
    public static Map<String, IR> optimizeAll(Map<String, IR> irMap) {
        return irMap.entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> removeRedundantNops(entry.getValue()),
                    (existing, replacement) -> replacement,
                    LinkedHashMap::new
                ));
    }
}