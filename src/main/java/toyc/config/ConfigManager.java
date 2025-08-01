package toyc.config;

import toyc.util.collection.Maps;
import toyc.util.collection.Sets;

import java.util.*;

import static toyc.util.collection.Maps.newMap;

/**
 * Manages a collection of {@link AlgorithmConfig}.
 */
public class ConfigManager {

    /**
     * Map from analysis id to corresponding AlgorithmConfig.
     */
    private final Map<String, AlgorithmConfig> configs = Maps.newLinkedHashMap();

    /**
     * Map from AlgorithmConfig to its required AlgorithmConfigs.
     */
    private final Map<AlgorithmConfig, List<AlgorithmConfig>> requires = newMap();

    public ConfigManager(List<AlgorithmConfig> configs) {
        configs.forEach(this::addConfig);
    }

    private void addConfig(AlgorithmConfig config) {
        if (configs.containsKey(config.getId())) {
            throw new ConfigException("There are multiple analyses for the same id " +
                    config.getId() + " in " + Configs.getAlgorithmConfigURL());
        }
        configs.put(config.getId(), config);
    }

    /**
     * Given an analysis id, returns the corresponding AlgorithmConfig.
     *
     * @throws ConfigException when the manager does not contain
     *                         the AlgorithmConfig for the given id.
     */
    AlgorithmConfig getConfig(String id) {
        AlgorithmConfig config = configs.get(id);
        if (config == null) {
            throw new ConfigException("Analysis \"" + id + "\" is not found in " +
                    Configs.getAlgorithmConfigURL());
        }
        return config;
    }

    /**
     * Overwrites the AlgorithmConfig.options by corresponding PlanConfig.options.
     */
    public void overwriteOptions(List<PlanConfig> planConfigs) {
        planConfigs.forEach(pc ->
                getConfig(pc.getId()).getOptions()
                        .update(pc.getOptions()));
    }

    /**
     * Obtains the required analyses of given analysis (represented by AlgorithmConfig).
     * This computation is based on the options given in PlanConfig,
     * thus this method should be called after invoking {@link #overwriteOptions}.
     * NOTE: we should obtain required configs by this method, instead of
     * {@link AlgorithmConfig#getRequires()}.
     */
    List<AlgorithmConfig> getRequiredConfigs(AlgorithmConfig config) {
        return requires.computeIfAbsent(config, c ->
                c.getRequires()
                        .stream()
                        .filter(required -> {
                            String conditions = Configs.extractConditions(required);
                            return Configs.satisfyConditions(conditions, c.getOptions());
                        })
                        .map(required -> getConfig(Configs.extractId(required)))
                        .toList());
    }

    /**
     * @return all configs (directly and indirectly) required by the given config
     */
    Set<AlgorithmConfig> getAllRequiredConfigs(AlgorithmConfig config) {
        Set<AlgorithmConfig> visited = Sets.newHybridSet();
        Deque<AlgorithmConfig> queue = new ArrayDeque<>(
                getRequiredConfigs(config));
        while (!queue.isEmpty()) {
            AlgorithmConfig curr = queue.pop();
            visited.add(curr);
            getRequiredConfigs(curr)
                    .stream()
                    .filter(c -> !visited.contains(c))
                    .forEach(queue::add);
        }
        return Collections.unmodifiableSet(visited);
    }
}
