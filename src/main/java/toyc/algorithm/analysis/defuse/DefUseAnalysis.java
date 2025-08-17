package toyc.algorithm.analysis.defuse;

import toyc.algorithm.analysis.FunctionAnalysis;
import toyc.algorithm.analysis.dataflow.analysis.ReachingDefinition;
import toyc.algorithm.analysis.dataflow.fact.DataflowResult;
import toyc.algorithm.analysis.dataflow.fact.SetFact;
import toyc.config.AlgorithmConfig;
import toyc.ir.IR;
import toyc.ir.exp.RValue;
import toyc.ir.exp.Var;
import toyc.ir.stmt.Stmt;
import toyc.util.collection.IndexMap;
import toyc.util.collection.Maps;
import toyc.util.collection.MultiMap;
import toyc.util.collection.Sets;
import toyc.util.collection.TwoKeyMultiMap;

/**
 * Computes intra-procedural def-use and use-def chains
 * based on reaching definition analysis.
 */
public class DefUseAnalysis extends FunctionAnalysis<DefUse> {

    public static final String ID = "def-use";

    /**
     * Whether compute definitions, i.e., use-def chains.
     */
    private final boolean computeDefs;

    /**
     * Whether compute uses, i.e., def-use chains.
     */
    private final boolean computeUses;

    public DefUseAnalysis(AlgorithmConfig config) {
        super(config);
        computeDefs = getOptions().getBoolean("compute-defs");
        computeUses = getOptions().getBoolean("compute-uses");
    }

    @Override
    public DefUse analyze(IR ir) {
        DataflowResult<Stmt, SetFact<Stmt>> rdResult = ir.getResult(ReachingDefinition.ID);
        TwoKeyMultiMap<Stmt, Var, Stmt> defs = computeDefs ?
                Maps.newTwoKeyMultiMap(new IndexMap<>(ir, ir.getStmts().size()),
                        () -> Maps.newMultiMap(Maps.newHybridMap()))
                : null;
        MultiMap<Stmt, Stmt> uses = computeUses ?
                Maps.newMultiMap(new IndexMap<>(ir, ir.getStmts().size()),
                        Sets::newHybridSet)
                : null;
        for (Stmt stmt : ir) {
            SetFact<Stmt> reachDefs = rdResult.getInFact(stmt);
            for (RValue use : stmt.getUses()) {
                if (use instanceof Var useVar) {
                    for (Stmt reachDef : reachDefs) {
                        reachDef.getDef().ifPresent(lhs -> {
                            if (lhs.equals(use)) {
                                if (computeDefs) {
                                    defs.put(stmt, useVar, reachDef);
                                }
                                if (computeUses) {
                                    uses.put(reachDef, stmt);
                                }
                            }
                        });
                    }
                }
            }
        }
        return new DefUse(defs, uses);
    }
}