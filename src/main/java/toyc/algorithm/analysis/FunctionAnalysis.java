package toyc.algorithm.analysis;

import toyc.algorithm.Algorithm;
import toyc.config.AlgorithmConfig;
import toyc.ir.IR;

/**
 * Abstract base class for all method analyses, or say, intra-procedural analyses.
 *
 * @param <R> result type
 */
public abstract class FunctionAnalysis<R> extends Algorithm {

    // private boolean isParallel;

    protected FunctionAnalysis(AlgorithmConfig config) {
        super(config);
    }

    /**
     * Runs this analysis for the given {@link IR}.
     * The result will be stored in {@link IR}. If the result is not used
     * by following analyses, then this method should return {@code null}.
     *
     * @param ir IR of the method to be analyzed
     * @return the analysis result for given ir.
     */
    public abstract R analyze(IR ir);
}
