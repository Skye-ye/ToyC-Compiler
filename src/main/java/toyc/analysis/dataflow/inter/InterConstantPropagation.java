package toyc.analysis.dataflow.inter;

import toyc.analysis.dataflow.analysis.constprop.CPFact;
import toyc.analysis.dataflow.analysis.constprop.ConstantPropagation;
import toyc.analysis.dataflow.analysis.constprop.Value;
import toyc.analysis.graph.icfg.CallEdge;
import toyc.analysis.graph.icfg.CallToReturnEdge;
import toyc.analysis.graph.icfg.NormalEdge;
import toyc.analysis.graph.icfg.ReturnEdge;
import toyc.config.AnalysisConfig;
import toyc.ir.IR;
import toyc.ir.exp.CallExp;
import toyc.ir.exp.Var;
import toyc.ir.stmt.*;
import toyc.language.Function;

import java.util.List;

/**
 * Implementation of interprocedural constant propagation for int values.
 */
public class InterConstantPropagation extends
        AbstractInterDataflowAnalysis<Function, Stmt, CPFact> {

    public static final String ID = "inter-const-prop";

    private final ConstantPropagation.Analysis cp;

    /**
     * Whether the constant propagation use control-flow edge information
     * to refine analysis results.
     */
    private final boolean edgeRefine;

    public InterConstantPropagation(AnalysisConfig config) {
        super(config);
        edgeRefine = getOptions().getBoolean("edge-refine");
        cp = new ConstantPropagation.Analysis(null, edgeRefine);
    }

    @Override
    public boolean isForward() {
        return cp.isForward();
    }

    @Override
    public CPFact newBoundaryFact(Stmt boundary) {
        IR ir = icfg.getContainingFunctionOf(boundary).getIR();
        return cp.newBoundaryFact(ir);
    }

    @Override
    public CPFact newInitialFact() {
        return new CPFact();
    }

    @Override
    public void meetInto(CPFact fact, CPFact target) {
        cp.meetInto(fact, target);
    }

    @Override
    protected boolean transferCallNode(Stmt stmt, CPFact in, CPFact out) {
        return out.copyFrom(in);
    }

    @Override
    protected boolean transferNonCallNode(Stmt stmt, CPFact in, CPFact out) {
        return cp.transferNode(stmt, in, out);
    }

    @Override
    protected CPFact transferNormalEdge(NormalEdge<Stmt> edge, CPFact out) {
        // Just apply edge transfer of intraprocedural constant propagation
        return edgeRefine ? cp.transferEdge(edge.getCFGEdge(), out) : out;
    }

    @Override
    protected CPFact transferCallToReturnEdge(CallToReturnEdge<Stmt> edge, CPFact out) {
        // Kill the value of LHS variable
        Call call = (Call) edge.source();
        Var lhs = call.getResult();
        if (lhs != null) {
            CPFact result = out.copy();
            result.remove(lhs);
            return result;
        } else {
            return out;
        }
    }

    @Override
    protected CPFact transferCallEdge(CallEdge<Stmt> edge, CPFact callSiteOut) {
        // Passing arguments at call site to parameters of the callee
        CallExp callExp = ((Call) edge.source()).getCallExp();
        Function callee = edge.getCallee();
        CPFact result = newInitialFact();
        List<Var> args = callExp.getArgs();
        List<Var> params = callee.getIR().getParams();
        for (int i = 0; i < args.size(); ++i) {
            Var arg = args.get(i);
            Var param = params.get(i);
            Value argValue = callSiteOut.get(arg);
            result.update(param, argValue);
        }
        return result;
    }

    @Override
    protected CPFact transferReturnEdge(ReturnEdge<Stmt> edge, CPFact returnOut) {
        // Passing return value to the LHS of the call statement
        Var lhs = ((Call) edge.getCallSite()).getResult();
        CPFact result = newInitialFact();
        if (lhs != null) {
            Value retValue = edge.getReturnVars()
                    .stream()
                    .map(returnOut::get)
                    .reduce(Value.getUndef(), cp::meetValue);
            result.update(lhs, retValue);
        }
        return result;
    }
}
