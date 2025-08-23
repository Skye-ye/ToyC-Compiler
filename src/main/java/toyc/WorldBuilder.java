package toyc;

import toyc.config.AlgorithmConfig;
import toyc.config.Options;

import java.util.List;

/**
 * Interface for {@link World} builder.
 */
public interface WorldBuilder {

    /**
     * Builds a new instance of {@link World} and make it globally accessible
     * through static methods of {@link World}.
     *
     * @param options  the options
     */
    void build(Options options);
}
