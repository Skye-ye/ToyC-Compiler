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
 * Configuration for an algorithm.
 */
public final class AlgorithmConfig implements PlanElement {

    /**
     * Description of the algorithm.
     * <p>
     * This information is only an explanation of the algorithm,
     * and not used by the system.
     */
    @JsonProperty
    private final String description;

    /**
     * Fully-qualified name of the algorithm class.
     * <p>
     * Here we use String (class name) instead of the Class itself
     * to represent the algorithm for fast startup speed. Our configuration
     * system will load all algorithm configs in the file at each startup.
     * If we use Class for this field, then it needs to load all
     * algorithm classes, including the ones that may not be used in this run,
     * which cost more time than merely reading class names.
     */
    @JsonProperty
    private final String algorithmClass;

    /**
     * Unique identifier of the algorithm.
     * <p>
     * ToyC compiler relies on algorithm id to identify each algorithm, so the
     * id of each algorithm must be unique. If an id is assigned to multiple
     * analyses, the configuration system will throw {@link ConfigException}.
     */
    @JsonProperty
    private final String id;

    /**
     * Indicates whether this algorithm modifies IR
     * <p>
     * This is used to determine whether the algorithm modifies the IR.
     * If it does, the Plan Builder will rerun required algorithms
     */
    @JsonProperty
    private final Boolean modification;

    /**
     * Require items of the algorithm.
     * <p>
     * Each require item contains two part:
     * 1. algorithm id (say A), whose result is required by this algorithm.
     * 2. require conditions, which are relevant to the options of this algorithm.
     * If the conditions are given, then this algorithm requires A
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
     * Options for the algorithm.
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
            @JsonProperty("modification") Boolean modification,
            @JsonProperty("requires") List<String> requires,
            @JsonProperty("options") AlgorithmOptions options) {
        this.description = description;
        this.algorithmClass = algorithmClass;
        this.id = id;
        this.modification = modification;
        this.requires = Objects.requireNonNullElse(requires, List.of());
        this.options = Objects.requireNonNullElse(options,
                AlgorithmOptions.emptyOptions());
    }

    /**
     * Convenient static factory for creating an algorithmConfig by merely
     * specifying id and options. The given options should be an array
     * of key-value pairs, e.g., [k1, v1, k2, v2, ...].
     */
    public static AlgorithmConfig of(String id, Object... options) {
        return new AlgorithmConfig(null, null, id, null, null,
                convertOptions(options));
    }

    /**
     * Converts an array of key-value pairs (e.g, [k1, v1, k2, v2, ...])
     * to algorithmOptions.
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

    public Boolean getModification() {
        return modification;
    }

    /**
     * Note that this API only returns unprocessed raw require information.
     * To obtain the real required analyses, you should call
     * {@link ConfigManager#getRequiredConfigs}.
     *
     * @return require information of this algorithm given in configuration files.
     */
    List<String> getRequires() {
        return requires;
    }

    public AlgorithmOptions getOptions() {
        return options;
    }

    public String toDetailedString() {
        return "algorithmConfig{" +
                "description='" + description + '\'' +
                ", algorithmClass='" + algorithmClass + '\'' +
                ", id='" + id + '\'' +
                ", modification='" + modification + '\'' +
                ", requires=" + requires +
                ", options=" + options +
                '}';
    }

    @Override
    public String toString() {
        return id;
    }

    /**
     * Parses a list of algorithmConfig from given input stream.
     */
    public static List<AlgorithmConfig> parseConfigs(InputStream content) {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        JavaType type = mapper.getTypeFactory()
                .constructCollectionType(List.class, AlgorithmConfig.class);
        try {
            return mapper.readValue(content, type);
        } catch (IOException e) {
            throw new ConfigException("Failed to read algorithm config file", e);
        }
    }
}
