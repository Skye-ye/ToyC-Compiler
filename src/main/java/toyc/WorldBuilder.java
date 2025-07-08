package toyc;

import toyc.config.AnalysisConfig;
import toyc.config.Options;

import java.util.List;

/**
 * Interface for {@link World} builder.
 */
public interface WorldBuilder {

    /**
     * Builds a new instance of {@link World} and make it globally accessible
     * through static methods of {@link World}.
     * TODO: remove {@code analyses}.
     *
     * @param options  the options
     * @param analyses the analyses to be executed
     */
    void build(Options options, List<AnalysisConfig> analyses);
}
