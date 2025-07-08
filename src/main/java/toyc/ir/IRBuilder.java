package toyc.ir;

import toyc.language.Function;

import java.io.Serializable;

/**
 * Interface for builder of {@link IR}.
 */
public interface IRBuilder extends Serializable {

    /**
     * Builds IR for concrete functions.
     */
    IR buildIR(Function function);
}
