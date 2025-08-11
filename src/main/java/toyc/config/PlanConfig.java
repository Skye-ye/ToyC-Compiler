package toyc.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * Configuration for an analysis to be executed.
 * <p>
 * Different from {@link AlgorithmConfig} which is specified by configuration file,
 * {@link PlanConfig} is specified by either plan file or options.
 *
 * @see AlgorithmConfig
 */
public class PlanConfig {

    private static final Logger logger = LogManager.getLogger(PlanConfig.class);

    private static final String PLAN_FILE = "toyc-plan.yml";

    /**
     * Unique identifier of the analysis.
     */
    @JsonProperty
    private final String id;

    /**
     * Options for the analysis.
     */
    @JsonProperty
    private final AlgorithmOptions options;

    @JsonCreator
    public PlanConfig(
            @JsonProperty("id") String id,
            @JsonProperty("options") AlgorithmOptions options) {
        this.id = id;
        this.options = Objects.requireNonNullElse(options,
                AlgorithmOptions.emptyOptions());
    }

    public String getId() {
        return id;
    }

    public AlgorithmOptions getOptions() {
        return options;
    }

    @Override
    public String toString() {
        return "PlanConfig{" +
                "id='" + id + '\'' +
                ", options=" + options +
                '}';
    }

    /**
     * Read a list of PlanConfig from given file.
     */
    public static List<PlanConfig> readConfigs(File file) {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        JavaType type = mapper.getTypeFactory()
                .constructCollectionType(List.class, PlanConfig.class);
        try {
            return mapper.readValue(file, type);
        } catch (IOException e) {
            throw new ConfigException("Failed to read plan file " + file, e);
        }
    }

    /**
     * Reads a list of PlanConfig from options.
     */
    public static List<PlanConfig> readConfigs(Options options) {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        JavaType mapType = mapper.getTypeFactory()
                .constructMapType(Map.class, String.class, Object.class);
        return options.getAnalyses().entrySet()
                .stream()
                .map(entry -> {
                    String id = entry.getKey();
                    String optStr = toYAMLString(entry.getValue());
                    try {
                        Map<String, Object> optsMap = optStr.isBlank()
                                ? Map.of()
                                // Leverage Jackson to parse YAML string to Map
                                : mapper.readValue(optStr, mapType);
                        return new PlanConfig(id, new AlgorithmOptions(optsMap));
                    } catch (JsonProcessingException e) {
                        throw new ConfigException("Invalid analysis options: " +
                                entry.getKey() + ":" + entry.getValue(), e);
                    }
                })
                .toList();
    }

    /**
     * Converts option string to a valid YAML string.
     * The option string is of format "key1:value1;key2:value2;...".
     */
    private static String toYAMLString(String optValue) {
        StringJoiner joiner = new StringJoiner("\n");
        for (String keyValue : optValue.split(";")) {
            if (!keyValue.isBlank()) {
                int i = keyValue.indexOf(':'); // split keyValue
                joiner.add(keyValue.substring(0, i) + ": "
                        + keyValue.substring(i + 1));
            }
        }
        return joiner.toString();
    }

    /**
     * Writes a list of PlanConfigs to given file.
     */
    public static void writeConfigs(List<PlanConfig> planConfigs, File outputDir) {
        File outFile = new File(outputDir, PLAN_FILE);
        ObjectMapper mapper = new ObjectMapper(
                new YAMLFactory()
                        .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
                        .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES));
        try {
            logger.info("Writing analysis plan to {}", outFile.getAbsolutePath());
            mapper.writeValue(outFile, planConfigs);
        } catch (IOException e) {
            throw new ConfigException("Failed to write plan file to "
                    + outFile.getAbsolutePath(), e);
        }
    }
}
