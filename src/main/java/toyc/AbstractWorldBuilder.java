package toyc;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import toyc.config.Options;

/**
 * Common functionality for {@link WorldBuilder} implementations.
 */
public abstract class AbstractWorldBuilder implements WorldBuilder {

    private static final Logger logger = LogManager.getLogger(AbstractWorldBuilder.class);

    /**
     * Obtains all input files specified in {@code options}.
     */
    protected static String getInputFile(Options options) {
        return options.getInputFile();
    }
}
