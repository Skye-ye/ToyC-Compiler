package toyc.algorithm.optimization;

import toyc.algorithm.analysis.defuse.DefUse;
import toyc.algorithm.analysis.defuse.DefUseAnalysis;
import toyc.algorithm.analysis.loop.Loop;
import toyc.algorithm.analysis.loop.LoopDetection;
import toyc.config.AlgorithmConfig;
import toyc.ir.IR;
import toyc.ir.exp.LValue;
import toyc.ir.exp.Var;
import toyc.ir.stmt.JumpStmt;
import toyc.ir.stmt.Stmt;
import toyc.ir.stmt.StmtListCopier;
import toyc.util.NumericSuffixNaming;
import toyc.util.collection.Maps;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class LoopUnrolling extends Optimization {
    public static final String ID = "loop-unroll";

    private IROperation operation;

    private IR ir;

    private NumericSuffixNaming nameManager;

    public LoopUnrolling(AlgorithmConfig config) {
        super(config);
    }

    @Override
    public IR optimize(IR ir) {
        operation = new IROperation(ir);
        this.ir = ir;
        nameManager = new NumericSuffixNaming(ir.getVars().stream()
                .map(Var::getName)
                .collect(Collectors.toSet()));
        Set<Loop> loops = ir.getResult(LoopDetection.ID);

        for (Loop loop : loops) {
            unrollLoop(loop);
        }

        return operation.getIR();
    }

    /**
     * Unroll the loop by duplicating its body
     */
    private void unrollLoop(Loop loop) {
        Stmt header = loop.header();
        Set<Stmt> body = loop.body();
        Set<Stmt> tails = loop.tails();
        Map<Var, Var> varMapping = createVarMapping(body, header.getIndex());

        // Create a new loop body by duplicating the original body
        List<Stmt> duplicatedBody =
                StmtListCopier.copy(body.stream().toList(), varMapping,
                        ir.getFunction());

        // Update tails' target
        for (Stmt tail : tails) {
            int index = tail.getIndex();
            for (Stmt stmt : duplicatedBody) {
                if (stmt.getIndex() == index) {
                    if (stmt instanceof JumpStmt jumpStmt) {
                        jumpStmt.setTarget(header);
                    } else {
                        throw new IllegalStateException(
                                "Unsupported tail statement type: " + stmt.getClass());
                    }
                    break;
                }
            }
        }

        operation.insertUnrolledLoop(header, duplicatedBody);
    }

    private Map<Var, Var> createVarMapping(Set<Stmt> stmts, int headerIndex) {
        DefUse defUse = ir.getResult(DefUseAnalysis.ID);
        Map<Var, Var> varMapping = Maps.newMap();

        for (Stmt stmt : stmts) {
            stmt.getDef().ifPresent(def -> {
                if (def instanceof Var defVar) {
                    if (!varMapping.containsKey(defVar) && !isVarDefinedBeforeLoop(defVar, headerIndex)) {
                        Set<Stmt> uses = defUse.getUses(stmt);
                        if (stmts.containsAll(uses)) {
                            // the definition is only used in loop body
                            String newVarName = nameManager.getNewVarName(defVar.getName());
                            Var clonedVar = new Var(
                                    ir.getFunction(),
                                    newVarName,
                                    defVar.getType(),
                                    -1,
                                    defVar.isConst() ? defVar.getConstValue() : null
                            );
                            varMapping.put(defVar, clonedVar);
                        }
                    }
                }
            });
        }
        return varMapping;
    }

    /**
     * Check if a variable is defined before the loop starts
     */
    private boolean isVarDefinedBeforeLoop(Var var, int loopHeaderIndex) {
        for (Var param : ir.getParams()) {
            if (param.equals(var)) {
                return true;
            }
        }
        for (Stmt stmt : ir.getStmts()) {
            if (stmt.getIndex() >= loopHeaderIndex) {
                break; // Stop checking after reaching the loop header
            }
            Optional<LValue> def = stmt.getDef();
            if (def.isPresent() && def.get().equals(var)) {
                return true;
            }
        }
        return false;
    }
}
