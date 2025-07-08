package toyc.analysis.dataflow.analysis;

import toyc.analysis.FunctionAnalysis;
import toyc.analysis.dataflow.fact.DataflowResult;
import toyc.analysis.dataflow.solver.Solver;
import toyc.analysis.graph.cfg.CFG;
import toyc.analysis.graph.cfg.CFGBuilder;
import toyc.config.AnalysisConfig;
import toyc.ir.IR;

/**
 * Driver for performing a specific kind of data-flow analysis for a method.
 */
public abstract class AnalysisDriver<Node, Fact>
        extends FunctionAnalysis<DataflowResult<Node, Fact>> {

    protected AnalysisDriver(AnalysisConfig config) {
        super(config);
    }

    @Override
    public DataflowResult<Node, Fact> analyze(IR ir) {
        CFG<Node> cfg = ir.getResult(CFGBuilder.ID);
        DataflowAnalysis<Node, Fact> analysis = makeAnalysis(cfg);
        Solver<Node, Fact> solver = Solver.getSolver();
        return solver.solve(analysis);
    }

    /**
     * Creates an analysis object for given cfg.
     */
    protected abstract DataflowAnalysis<Node, Fact> makeAnalysis(CFG<Node> cfg);
}
