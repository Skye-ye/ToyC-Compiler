package toyc.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import toyc.util.collection.Maps;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Configuration for an analysis.
 */
public class AlgorithmConfig {

    /**
     * Description of the analysis.
     * <p>
     * This information is only an explanation of the analysis,
     * and not used by Tai-e.
     */
    @JsonProperty
    private final String description;

    /**
     * Fully-qualified name of the analysis class.
     * <p>
     * Here we use String (class name) instead of the Class itself
     * to represent the analysis for fast startup speed. Our configuration
     * system will load all analysis configs in the file at each startup.
     * If we use Class for this field, then it needs to load all
     * analysis classes, including the ones that may not be used in this run,
     * which cost more time than merely reading class names.
     */
    @JsonProperty
    private final String algorithmClass;

    /**
     * Unique identifier of the analysis.
     * <p>
     * Tai-e relies on analysis id to identify each analysis, so the id of
     * each analysis must be unique. If an id is assigned to multiple analyses,
     * the configuration system will throw {@link ConfigException}.
     */
    @JsonProperty
    private final String id;

    /**
     * Require items of the analysis.
     * <p>
     * Each require item contains two part:
     * 1. analysis id (say A), whose result is required by this analysis.
     * 2. require conditions, which are relevant to the options of this analysis.
     * If the conditions are given, then this analysis requires A
     * only when all conditions are satisfied.
     * <p>
     * We support simple compositions of conditions, and we give some examples
     * to illustrate require items.
     * requires: [A1,A2] # requires analyses A1 and A2
     * requires: [A(x=y)] # requires A when value of option x is y
     * requires: [A(x=y&amp;a=b)] # requires A when value of option x is y
     * and value of option a is b
     * requires: [A(x=a|b|c)] # requires A when value of option x is
     * a, b, or c.
     */
    @JsonProperty
    private final List<String> requires;

    /**
     * Options for the analysis.
     */
    @JsonProperty
    private final AlgorithmOptions options;

    /**
     * Used by deserialization from configuration file.
     */
    @JsonCreator
    public AlgorithmConfig(
            @JsonProperty("description") String description,
            @JsonProperty("algorithmClass") String algorithmClass,
            @JsonProperty("id") String id,
            @JsonProperty("requires") List<String> requires,
            @JsonProperty("options") AlgorithmOptions options) {
        this.description = description;
        this.algorithmClass = algorithmClass;
        this.id = id;
        this.requires = Objects.requireNonNullElse(requires, List.of());
        this.options = Objects.requireNonNullElse(options,
                AlgorithmOptions.emptyOptions());
    }

    /**
     * Convenient static factory for creating an AnalysisConfig by merely
     * specifying id and options. The given options should be an array
     * of key-value pairs, e.g., [k1, v1, k2, v2, ...].
     */
    public static AlgorithmConfig of(String id, Object... options) {
        return new AlgorithmConfig(null, null, id, null, convertOptions(options));
    }

    /**
     * Converts an array of key-value pairs (e.g, [k1, v1, k2, v2, ...])
     * to AnalysisOptions.
     */
    private static AlgorithmOptions convertOptions(Object[] options) {
        Map<String, Object> optionsMap = Maps.newLinkedHashMap();
        for (int i = 0; i < options.length; i += 2) {
            optionsMap.put((String) options[i], options[i + 1]);
        }
        return new AlgorithmOptions(optionsMap);
    }

    public String getDescription() {
        return description;
    }

    public String getAlgorithmClass() {
        return algorithmClass;
    }

    public String getId() {
        return id;
    }

    /**
     * Note that this API only returns unprocessed raw require information.
     * To obtain the real required analyses, you should call
     * {@link ConfigManager#getRequiredConfigs}.
     *
     * @return require information of this analysis given in configuration files.
     */
    List<String> getRequires() {
        return requires;
    }

    public AlgorithmOptions getOptions() {
        return options;
    }

    public String toDetailedString() {
        return "AnalysisConfig{" +
                "description='" + description + '\'' +
                ", algorithmClass='" + algorithmClass + '\'' +
                ", id='" + id + '\'' +
                ", requires=" + requires +
                ", options=" + options +
                '}';
    }

    @Override
    public String toString() {
        return id;
    }

    /**
     * Parses a list of AnalysisConfig from given input stream.
     */
    public static List<AlgorithmConfig> parseConfigs(InputStream content) {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        JavaType type = mapper.getTypeFactory()
                .constructCollectionType(List.class, AlgorithmConfig.class);
        try {
            return mapper.readValue(content, type);
        } catch (IOException e) {
            throw new ConfigException("Failed to read analysis config file", e);
        }
    }
}
