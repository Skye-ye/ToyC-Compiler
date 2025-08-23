package toyc.algorithm.analysis.deadcode;

import toyc.algorithm.analysis.FunctionAnalysis;
import toyc.algorithm.analysis.dataflow.analysis.LiveVariable;
import toyc.algorithm.analysis.dataflow.analysis.constprop.CPFact;
import toyc.algorithm.analysis.dataflow.analysis.constprop.ConstantPropagation;
import toyc.algorithm.analysis.dataflow.analysis.constprop.Evaluator;
import toyc.algorithm.analysis.dataflow.analysis.constprop.Value;
import toyc.algorithm.analysis.dataflow.fact.NodeResult;
import toyc.algorithm.analysis.dataflow.fact.SetFact;
import toyc.algorithm.analysis.graph.cfg.CFG;
import toyc.algorithm.analysis.graph.cfg.CFGBuilder;
import toyc.algorithm.analysis.graph.cfg.CFGEdge;
import toyc.config.AlgorithmConfig;
import toyc.ir.IR;
import toyc.ir.exp.*;
import toyc.ir.stmt.AssignStmt;
import toyc.ir.stmt.Call;
import toyc.ir.stmt.If;
import toyc.ir.stmt.Stmt;
import toyc.util.collection.Maps;
import toyc.util.collection.Sets;

import java.util.*;

/**
 * Detects dead code in an IR.
 */
public class DeadCodeDetection extends FunctionAnalysis<Map<Stmt,
        DeadCodeDetection.Kind>> {

    public static final String ID = "dead-code";

    public DeadCodeDetection(AlgorithmConfig config) {
        super(config);
    }

    @Override
    public Map<Stmt, DeadCodeDetection.Kind> analyze(IR ir) {
        // obtain results of pre-analyses
        CFG<Stmt> cfg = ir.getResult(CFGBuilder.ID);
        NodeResult<Stmt, CPFact> constants =
                ir.getResult(ConstantPropagation.ID);
        NodeResult<Stmt, SetFact<Var>> liveVars =
                ir.getResult(LiveVariable.ID);
        // keep statements (dead code) sorted in the resulting set
        Map<Stmt, DeadCodeDetection.Kind> deadCode =
                Maps.newOrderedMap(Comparator.comparing(Stmt::getIndex));
        // initialize graph traversal
        Set<Stmt> visited = Sets.newSet(cfg.getNumberOfNodes());
        Queue<Stmt> queue = new ArrayDeque<>();
        queue.add(cfg.getEntry());
        while (!queue.isEmpty()) {
            Stmt stmt = queue.remove();
            visited.add(stmt);
            if (isDeadAssignment(stmt, liveVars)) {
                // record dead assignment
                deadCode.put(stmt, Kind.NORMAL);
            } else if (isDeadCall(stmt, liveVars)) {
                // record dead call
                deadCode.put(stmt, Kind.NORMAL);
            }
            for (CFGEdge<Stmt> edge : cfg.getOutEdgesOf(stmt)) {
                if (isUnreachableBranch(edge, constants)) {
                    deadCode.put(stmt, edge.getKind() == CFGEdge.Kind.IF_TRUE
                            ? Kind.IF_TRUE : Kind.IF_FALSE);
                } else {
                    Stmt succ = edge.target();
                    if (!visited.contains(succ)) {
                        queue.add(succ);
                    }
                }
            }
        }
        if (visited.size() < cfg.getNumberOfNodes()) {
            // this means that some nodes are not reachable during traversal
            for (Stmt s : ir) {
                if (!visited.contains(s)) {
                    deadCode.put(s, Kind.NORMAL);
                }
            }
        }
        return deadCode.isEmpty() ? Collections.emptyMap() : deadCode;
    }

    private static boolean isDeadAssignment(
            Stmt stmt, NodeResult<Stmt, SetFact<Var>> liveVars) {
        if (stmt instanceof AssignStmt<?, ?> assign) {
            if (assign.getLValue() instanceof Var lhs) {
                return !liveVars.getOutFact(assign).contains(lhs) &&
                        hasNoSideEffect(assign.getRValue());
            }
        }
        return false;
    }

    private static boolean isDeadCall(
            Stmt stmt, NodeResult<Stmt, SetFact<Var>> liveVars) {
        if (stmt instanceof Call call) {
            Var result = call.getResult();
            if (result != null) {
                return !liveVars.getOutFact(call).contains(result) &&
                        hasNoSideEffect(call.getCallExp());
            } else {
                // if the call has no result, it is dead if it has no side effect
                return hasNoSideEffect(call.getCallExp());
            }
        }
        return false;
    }


    private static boolean isUnreachableBranch(
            CFGEdge<Stmt> edge, NodeResult<Stmt, CPFact> constants) {
        Stmt src = edge.source();
        if (src instanceof If ifStmt) {
            Value cond = Evaluator.evaluate(
                    ifStmt.getCondition(), constants.getInFact(ifStmt));
            if (cond.isConstant()) {
                int v = cond.getConstant();
                return v == 1 && edge.getKind() == CFGEdge.Kind.IF_FALSE ||
                        v == 0 && edge.getKind() == CFGEdge.Kind.IF_TRUE;
            }
        }
        return false;
    }

    /**
     * @return true if given RValue has no side effect, otherwise false.
     */
    private static boolean hasNoSideEffect(RValue rvalue) {
        if (rvalue instanceof ArithmeticExp) {
            ArithmeticExp.Op op = ((ArithmeticExp) rvalue).getOperator();
            // may trigger DivideByZeroException
            return op != ArithmeticExp.Op.DIV && op != ArithmeticExp.Op.REM;
        }
        return true;
    }

    public enum Kind {

        /**
         * Edge kind for fall-through to next statement.
         */
        NORMAL,

        /**
         * Edge kind for if statements when condition is true.
         */
        IF_TRUE,

        /**
         * Edge kind for if statements when condition is false.
         */
        IF_FALSE,
    }
}
