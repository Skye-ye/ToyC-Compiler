package toyc.analysis.deadcode;

import toyc.analysis.FunctionAnalysis;
import toyc.analysis.dataflow.analysis.LiveVariable;
import toyc.analysis.dataflow.analysis.constprop.CPFact;
import toyc.analysis.dataflow.analysis.constprop.ConstantPropagation;
import toyc.analysis.dataflow.analysis.constprop.Evaluator;
import toyc.analysis.dataflow.analysis.constprop.Value;
import toyc.analysis.dataflow.fact.NodeResult;
import toyc.analysis.dataflow.fact.SetFact;
import toyc.analysis.graph.cfg.CFG;
import toyc.analysis.graph.cfg.CFGBuilder;
import toyc.analysis.graph.cfg.CFGEdge;
import toyc.config.AnalysisConfig;
import toyc.ir.IR;
import toyc.ir.exp.ArithmeticExp;
import toyc.ir.exp.RValue;
import toyc.ir.exp.Var;
import toyc.ir.stmt.AssignStmt;
import toyc.ir.stmt.If;
import toyc.ir.stmt.Stmt;
import toyc.util.collection.Sets;

import java.util.*;

public class DeadCodeDetection extends FunctionAnalysis <Set<Stmt>>{
    public static final String ID ="dead-code";

    public DeadCodeDetection(AnalysisConfig config) {
        super(config);
    }
    @Override
    public Set<Stmt> analyze(IR ir){
        CFG<Stmt> cfg= ir.getResult(CFGBuilder.ID);
        NodeResult<Stmt, CPFact> constants = ir.getResult(ConstantPropagation.ID);
        NodeResult<Stmt, SetFact<Var>> liveVars = ir.getResult(LiveVariable.ID);

        Set<Stmt> deadCode = Sets.newOrderedSet(Comparator.comparing(Stmt::getIndex));

        Set<Stmt> visited = Sets.newSet(cfg.getNumberOfNodes());
        Queue<Stmt> queue= new ArrayDeque<>();
        queue.add(cfg.getEntry());

        while(!queue.isEmpty()){
            Stmt stmt = queue.remove();
            visited.add(stmt);


        if(isDeadAssignment(stmt,liveVars)){
            deadCode.add(stmt);
        }

        cfg.getOutEdgesOf(stmt)
                .stream()
                .filter(edge->!isUnreachableBranch(edge,constants))
                .map(CFGEdge::target)
                .forEach(succ -> {
                    if(!visited.contains(succ)){
                        queue.add(succ);
                    }
                });
    }
        if(visited.size()<cfg.getNumberOfNodes()){
            for(Stmt s : ir){
                if(!visited.contains(s)){
                    deadCode.add(s);
                }
            }
        }
    return deadCode.isEmpty() ? Collections.emptySet() : deadCode;

    }

    private static boolean isDeadAssignment(
            Stmt stmt, NodeResult<Stmt, SetFact<Var>> liveVars ){
        if(stmt instanceof AssignStmt<?,?> assign){
            if (assign.getLValue() instanceof Var lhs) {
                return !liveVars.getOutFact(assign).contains(lhs) &&
                        hasNoSideEffect(assign.getRValue());
            }
        }
        return false;
    }

    private static boolean isUnreachableBranch(
            CFGEdge<Stmt> edge, NodeResult<Stmt, CPFact> constants){
        Stmt src = edge.source();

        if (src instanceof If ifStmt){
            Value cond = Evaluator.evaluate(
                    ifStmt.getCondition(), constants.getInFact(ifStmt));
            if(cond.isConstant()){
                int v = cond.getConstant();
                return v == 1 && edge.getKind() == CFGEdge.Kind.IF_FALSE ||
                        v == 0 && edge.getKind() == CFGEdge.Kind.IF_TRUE;
            }
        }
        return false;
    }

    private static boolean hasNoSideEffect(RValue rvalue){
        if (rvalue instanceof ArithmeticExp) {
            ArithmeticExp.Op op = ((ArithmeticExp) rvalue).getOperator();
            return op != ArithmeticExp.Op.DIV && op != ArithmeticExp.Op.REM;
        }
        return true;
    }
}
