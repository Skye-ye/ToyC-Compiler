package toyc.config;

import java.util.List;

/**
 * Contains information about analysis execution plan.
 *
 * @param analyses        list of analyses to be executed.
 */
public record Plan(
        List<PlanElement> analyses) implements PlanElement {

    private static final Plan EMPTY = new Plan(List.of());

    /**
     * @return an empty plan.
     */
    public static Plan emptyPlan() {
        return EMPTY;
    }

    /**
     * Convenience method to get all AlgorithmConfig objects recursively
     */
    public List<AlgorithmConfig> getAllAnalyses() {
        return analyses.stream()
                .flatMap(element -> switch (element) {
                    case AlgorithmConfig config -> java.util.stream.Stream.of(config);
                    case Plan nestedPlan -> nestedPlan.getAllAnalyses().stream();
                })
                .toList();
    }

    /**
     * Convenience method to get all nested Plans recursively
     */
    public List<Plan> getAllNestedPlans() {
        return analyses.stream()
                .flatMap(element -> switch (element) {
                    case AlgorithmConfig config -> java.util.stream.Stream.empty();
                    case Plan nestedPlan -> java.util.stream.Stream.concat(
                            java.util.stream.Stream.of(nestedPlan),
                            nestedPlan.getAllNestedPlans().stream()
                    );
                })
                .toList();
    }
}
