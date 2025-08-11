package toyc.algorithm.analysis.dataflow.analysis;

import toyc.algorithm.analysis.FunctionAnalysis;
import toyc.algorithm.analysis.dataflow.fact.DataflowResult;
import toyc.algorithm.analysis.dataflow.solver.Solver;
import toyc.algorithm.analysis.graph.cfg.CFG;
import toyc.algorithm.analysis.graph.cfg.CFGBuilder;
import toyc.config.AlgorithmConfig;
import toyc.ir.IR;

/**
 * Driver for performing a specific kind of data-flow analysis for a method.
 */
public abstract class AnalysisDriver<Node, Fact>
        extends FunctionAnalysis<DataflowResult<Node, Fact>> {

    protected AnalysisDriver(AlgorithmConfig config) {
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
