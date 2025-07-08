package toyc.ir;

import toyc.language.Function;
import toyc.language.Program;

import java.io.Serializable;

/**
 * Interface for builder of {@link IR}.
 */
public interface IRBuilder extends Serializable {

    /**
     * Builds IR for concrete functions.
     */
    IR buildIR(Function function);

    /**
     * Builds IR for all functions in the given Program.
     */
    void buildAll(Program program);
}
