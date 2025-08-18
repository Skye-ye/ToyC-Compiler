package toyc.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Objects;

/**
 * Represents a nested plan configuration.
 * This extends PlanConfig to handle nested plan structures.
 */
class NestedPlanConfig extends PlanConfig {

    /**
     * Name of the nested plan.
     */
    @JsonProperty
    private final String name;

    /**
     * List of analyses in this nested plan.
     */
    @JsonProperty
    private final List<PlanConfig> analyses;

    @JsonCreator
    public NestedPlanConfig(
            @JsonProperty("name") String name,
            @JsonProperty("analyses") List<PlanConfig> analyses) {
        super(null, null); // No id or options for nested plans
        this.name = name;
        this.analyses = Objects.requireNonNullElse(analyses, List.of());
    }

    public String getName() {
        return name;
    }

    public List<PlanConfig> getAnalyses() {
        return analyses;
    }

    @Override
    public String toString() {
        return "NestedPlanConfig{" +
                "name='" + name + '\'' +
                ", analyses=" + analyses.size() +
                '}';
    }
}