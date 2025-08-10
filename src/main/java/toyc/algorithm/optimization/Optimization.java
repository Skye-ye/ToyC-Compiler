package toyc.algorithm.optimization;

import toyc.algorithm.Algorithm;
import toyc.config.AlgorithmConfig;
import toyc.ir.IR;
import toyc.language.Function;

/**
 * Driver for performing a specific kind of optimization
 */
public abstract class Optimization extends Algorithm {
    protected Optimization(AlgorithmConfig config) {
        super(config);
    }

    public void optimize(IR ir) {
        Function function = ir.getFunction();
        function.setIR(makeOptimization(ir));
    }

    protected abstract IR makeOptimization(IR ir);
}
