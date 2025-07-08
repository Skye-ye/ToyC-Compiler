package toyc;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import toyc.config.Options;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Common functionality for {@link WorldBuilder} implementations.
 */
public abstract class AbstractWorldBuilder implements WorldBuilder {

    private static final Logger logger = LogManager.getLogger(AbstractWorldBuilder.class);

    protected static String getInputs(Options options) {
            return String.join(File.pathSeparator, options.getInputFiles());
    }

    /**
     * Obtains all input files specified in {@code options}.
     */
    protected static List<String> getInputFiles(Options options) {
        List<String> inputs = new ArrayList<>();
        options.getInputFiles().forEach(value -> {
            if (value.endsWith(".txt")) {
                // value is a path to a file that contains class names
                try (Stream<String> lines = Files.lines(Path.of(value))) {
                    lines.forEach(inputs::add);
                } catch (IOException e) {
                    logger.warn("Failed to read input class file {} due to {}",
                            value, e);
                }
            } else {
                // value is a class name
                inputs.add(value);
            }
        });
        return inputs;
    }
}
