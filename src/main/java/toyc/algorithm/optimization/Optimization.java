package toyc.algorithm.optimization;

import toyc.algorithm.Algorithm;
import toyc.config.AlgorithmConfig;
import toyc.ir.IR;
import toyc.language.Function;

/**
 * Abstract base class for all optimizations.
 */
public abstract class Optimization extends Algorithm {
    protected Optimization(AlgorithmConfig config) {
        super(config);
    }

    public abstract IR optimize(IR ir);
}
