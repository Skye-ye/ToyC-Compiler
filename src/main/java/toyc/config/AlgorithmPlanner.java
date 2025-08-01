package toyc.config;

import toyc.algorithm.analysis.graph.callgraph.CallGraphBuilder;
import toyc.util.collection.CollectionUtils;
import toyc.util.collection.Lists;
import toyc.util.graph.Graph;
import toyc.util.graph.SCC;
import toyc.util.graph.SimpleGraph;
import toyc.util.graph.TopologicalSorter;

import java.util.*;
import java.util.stream.Collectors;

import static toyc.util.collection.Sets.newSet;

/**
 * Makes analysis plan based on given plan configs and analysis configs.
 */
public class AlgorithmPlanner {

    private final ConfigManager manager;

    /**
     * Set of IDs for the analyses whose results are kept.
     */
    private final Set<String> keepResult;

    public AlgorithmPlanner(ConfigManager manager, Set<String> keepResult) {
        this.manager = manager;
        this.keepResult = keepResult;
    }

    /**
     * This method makes a plan by converting given list of PlanConfig
     * to AnalysisConfig. It will be used when analysis plan is specified
     * by configuration file.
     *
     * @return the analysis plan consists of a list of analysis config.
     * @throws ConfigException if the given planConfigs are invalid.
     */
    public Plan makePlan(List<PlanConfig> planConfigs,
                         boolean reachableScope) {
        List<AlgorithmConfig> analyses = covertConfigs(planConfigs);
        validateAnalyses(analyses, reachableScope);
        Graph<AlgorithmConfig> graph = buildDependenceGraph(analyses);
        validateDependenceGraph(graph);
        
        // Expand plan to handle IR modifications
        analyses = expandPlanWithIRModification(analyses, reachableScope);
        
        return new Plan(analyses, graph, keepResult);
    }

    /**
     * Converts a list of PlanConfigs to the list of corresponding AnalysisConfigs.
     */
    private List<AlgorithmConfig> covertConfigs(List<PlanConfig> planConfigs) {
        return planConfigs.stream()
                .map(pc -> manager.getConfig(pc.getId()))
                .collect(Collectors.toList());
    }

    /**
     * Checks if the given analysis sequence is valid.
     *
     * @param analyses       the given analysis sequence
     * @param reachableScope whether the analysis scope is set to reachable
     * @throws ConfigException if the given analyses is invalid
     */
    private void validateAnalyses(List<AlgorithmConfig> analyses, boolean reachableScope) {
        // check if all required analyses are placed in front of
        // their requiring analyses
        for (int i = 0; i < analyses.size(); ++i) {
            AlgorithmConfig config = analyses.get(i);
            for (AlgorithmConfig required : manager.getRequiredConfigs(config)) {
                int rindex = analyses.indexOf(required);
                if (rindex == -1) {
                    // required analysis is missing
                    throw new ConfigException(String.format(
                            "'%s' is required by '%s' but missing in analysis plan",
                            required, config));
                } else if (rindex >= i) {
                    // invalid analysis order: required analysis runs
                    // after current analysis
                    throw new ConfigException(String.format(
                            "'%s' is required by '%s' but it runs after '%s'",
                            required, config, config));
                }
            }
        }
        if (reachableScope) { // analysis scope is set to reachable
            // check if given analyses include call graph builder
            AlgorithmConfig cg = CollectionUtils.findFirst(analyses,
                    AlgorithmPlanner::isCG);
            if (cg == null) {
                throw new ConfigException(String.format("Scope is reachable" +
                                " but call graph builder (%s) is not given in analyses",
                        CallGraphBuilder.ID));
            }
            // check if call graph builder is executed as early as possible
            Set<AlgorithmConfig> cgRequired = manager.getAllRequiredConfigs(cg);
            for (AlgorithmConfig config : analyses) {
                if (config.equals(cg)) {
                    break;
                }
                if (!cgRequired.contains(config)) {
                    throw new ConfigException(String.format(
                            "Scope is reachable, thus '%s' " +
                                    "should be placed after call graph builder (%s)",
                            config, CallGraphBuilder.ID));
                }
            }
        }
    }

    private static boolean isCG(AlgorithmConfig config) {
        return config.getId().equals(CallGraphBuilder.ID);
    }

    /**
     * This method makes an analysis plan based on given plan configs,
     * and it will automatically add required analyses (which are not in
     * the given plan) to the resulting plan.
     * It will be used when analysis plan is specified by command line options.
     *
     * @return the analysis plan consisting of a list of analysis config.
     * @throws ConfigException if the specified planConfigs is invalid.
     */
    public Plan expandPlan(List<PlanConfig> planConfigs,
                           boolean reachableScope) {
        List<AlgorithmConfig> configs = covertConfigs(planConfigs);
        if (reachableScope) { // complete call graph builder
            AlgorithmConfig cg = CollectionUtils.findFirst(configs,
                    AlgorithmPlanner::isCG);
            if (cg == null) {
                // if analysis scope is reachable and call graph builder is
                // not given, then we automatically add it
                configs.add(manager.getConfig(CallGraphBuilder.ID));
            }
        }
        Graph<AlgorithmConfig> graph = buildDependenceGraph(configs);
        validateDependenceGraph(graph);
        List<AlgorithmConfig> analyses = new TopologicalSorter<>(graph, configs).get();
        if (reachableScope) {
            analyses = shiftCG(analyses);
        }
        
        // Expand plan to handle IR modifications
        analyses = expandPlanWithIRModification(analyses, reachableScope);
        
        return new Plan(analyses, graph, keepResult);
    }

    /**
     * Shifts call graph builder (cg) in given sequence to ensure that
     * it will run before all the analyses that it does not require.
     */
    private List<AlgorithmConfig> shiftCG(List<AlgorithmConfig> analyses) {
        AlgorithmConfig cg = CollectionUtils.findFirst(analyses,
                AlgorithmPlanner::isCG);
        Set<AlgorithmConfig> required = manager.getAllRequiredConfigs(cg);
        List<AlgorithmConfig> notRequired = new ArrayList<>();
        // obtain the analyses that run before cg but not required by cg
        for (AlgorithmConfig c : analyses) {
            if (c.equals(cg)) {
                break;
            }
            if (!required.contains(c)) {
                notRequired.add(c);
            }
        }
        List<AlgorithmConfig> result = new ArrayList<>(analyses.size());
        // add analyses that are required by cg
        for (AlgorithmConfig c : analyses) {
            if (required.contains(c)) {
                result.add(c);
            }
            if (c.equals(cg)) { // found cg, break
                break;
            }
        }
        result.add(cg); // add cg
        // add analyses that are not required by cg but placed before cg
        // in the original sequence
        result.addAll(notRequired);
        // add remaining analyses
        for (int i = analyses.indexOf(cg) + 1; i < analyses.size(); ++i) {
            result.add(analyses.get(i));
        }
        return result;
    }

    /**
     * Builds a dependence graph for AnalysisConfigs.
     * This method traverses relevant AnalysisConfigs starting from the ones
     * specified by given configs. During the traversal, if it finds that
     * analysis A1 is required by A2, then it adds an edge A1 -> A2 and
     * nodes A1 and A2 to the resulting graph.
     * <p>
     * The resulting graph contains the given analyses (planConfigs) and
     * all their (directly and indirectly) required analyses.
     */
    private Graph<AlgorithmConfig> buildDependenceGraph(List<AlgorithmConfig> configs) {
        SimpleGraph<AlgorithmConfig> graph = new SimpleGraph<>();
        Set<AlgorithmConfig> visited = newSet();
        Queue<AlgorithmConfig> workList = new ArrayDeque<>(configs);
        while (!workList.isEmpty()) {
            AlgorithmConfig config = workList.poll();
            graph.addNode(config);
            visited.add(config);
            manager.getRequiredConfigs(config).forEach(required -> {
                graph.addEdge(required, config);
                if (!visited.contains(required)) {
                    workList.add(required);
                }
            });
        }
        return graph;
    }

    /**
     * Checks if the given dependence graph is valid.
     *
     * @throws ConfigException if the given plan is invalid
     */
    private void validateDependenceGraph(Graph<AlgorithmConfig> graph) {
        // Check if the dependence graph is self-contained, i.e.,
        // every required analysis is included in the graph
        graph.forEach(config -> {
            List<AlgorithmConfig> missing = Lists.filter(
                    manager.getRequiredConfigs(config),
                    c -> !graph.hasNode(c));
            if (!missing.isEmpty()) {
                throw new ConfigException("Invalid analysis plan: " +
                        missing + " are missing");
            }
        });
        // Check if the dependence graph contains cycles
        SCC<AlgorithmConfig> scc = new SCC<>(graph);
        if (!scc.getTrueComponents().isEmpty()) {
            throw new ConfigException("Invalid analysis plan: " +
                    scc.getTrueComponents() + " are mutually dependent");
        }
    }

    /**
     * Expands the given analysis plan to handle IR modifications.
     * When an algorithm modifies IR, all previously executed analyses become invalid
     * since their results are stored in the IR. This method re-inserts required
     * analyses after each IR-modifying algorithm.
     *
     * @param originalPlan the original analysis plan
     * @param reachableScope whether the analysis scope is set to reachable
     * @return expanded plan with re-inserted analyses after IR modifications
     */
    private List<AlgorithmConfig> expandPlanWithIRModification(List<AlgorithmConfig> originalPlan, boolean reachableScope) {
        List<AlgorithmConfig> expandedPlan = new ArrayList<>();
        List<AlgorithmConfig> executedNonModifying = new ArrayList<>();
        
        for (int i = 0; i < originalPlan.size(); i++) {
            AlgorithmConfig config = originalPlan.get(i);
            expandedPlan.add(config);
            
            if (isIRModifying(config)) {
                // Find what analyses future algorithms need
                Set<AlgorithmConfig> neededAnalyses = findNeededAnalysesAfterModification(
                    executedNonModifying, originalPlan, i + 1, reachableScope);
                
                // Add needed analyses in dependency order    
                List<AlgorithmConfig> orderedNeeded = orderByDependencies(neededAnalyses);
                expandedPlan.addAll(orderedNeeded);
                
                // Update executed list - only keep the re-inserted analyses
                executedNonModifying.clear();
                executedNonModifying.addAll(orderedNeeded);
            } else {
                // Non-modifying algorithm - add to executed list
                executedNonModifying.add(config);
            }
        }
        
        return expandedPlan;
    }

    /**
     * Checks if the given algorithm modifies IR.
     */
    private boolean isIRModifying(AlgorithmConfig config) {
        return config.getModification() != null && config.getModification();
    }

    /**
     * Finds analyses that need to be re-run after IR modification.
     * This method looks at future algorithms in the plan and determines which
     * previously executed analyses they depend on.
     *
     * @param executedNonModifying analyses executed before the IR modification
     * @param originalPlan the complete original plan
     * @param startIndex index to start looking for future algorithms
     * @param reachableScope whether the analysis scope is set to reachable
     * @return set of analyses that need to be re-run
     */
    private Set<AlgorithmConfig> findNeededAnalysesAfterModification(
            List<AlgorithmConfig> executedNonModifying,
            List<AlgorithmConfig> originalPlan,
            int startIndex,
            boolean reachableScope) {
        
        Set<AlgorithmConfig> needed = newSet();
        
        // If using reachable scope, always need to rebuild call graph after IR modification
        // because function calls might have changed, affecting reachability
        if (reachableScope) {
            AlgorithmConfig cgConfig = CollectionUtils.findFirst(executedNonModifying,
                    AlgorithmPlanner::isCG);
            if (cgConfig != null) {
                needed.add(cgConfig);
                // Also add its dependencies
                needed.addAll(manager.getAllRequiredConfigs(cgConfig));
            }
        }
        
        // Look at all future algorithms in the plan
        for (int i = startIndex; i < originalPlan.size(); i++) {
            AlgorithmConfig futureConfig = originalPlan.get(i);
            
            // Find all dependencies of this future algorithm
            Set<AlgorithmConfig> allRequired = manager.getAllRequiredConfigs(futureConfig);
            
            // Add any required analyses that were executed before the IR modification
            for (AlgorithmConfig required : allRequired) {
                if (executedNonModifying.contains(required)) {
                    needed.add(required);
                    // Also add its dependencies recursively
                    needed.addAll(manager.getAllRequiredConfigs(required));
                }
            }
        }
        
        // Filter to only include analyses from the executed list
        needed.retainAll(executedNonModifying);
        
        return needed;
    }

    /**
     * Orders the given analyses by their dependencies using topological sort.
     */
    private List<AlgorithmConfig> orderByDependencies(Set<AlgorithmConfig> analyses) {
        if (analyses.isEmpty()) {
            return List.of();
        }
        
        // Build dependency graph for these analyses
        Graph<AlgorithmConfig> subGraph = buildDependenceGraph(new ArrayList<>(analyses));
        
        // Return topologically sorted order
        return new TopologicalSorter<>(subGraph).get();
    }
}
