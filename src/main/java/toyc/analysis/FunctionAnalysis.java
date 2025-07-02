package toyc.analysis;

import toyc.ir.IR;

/**
 * Abstract base class for all method analyses, or say, intra-procedural analyses.
 *
 * @param <R> result type
 */
public abstract class FunctionAnalysis<R> extends Analysis {

    // private boolean isParallel;

    protected FunctionAnalysis(String id) {
        super(id);
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
