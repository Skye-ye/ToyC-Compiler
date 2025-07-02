package toyc.analysis.graph.callgraph;

import toyc.util.collection.Maps;
import toyc.util.collection.MultiMap;
import toyc.util.collection.Sets;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Common functionality for {@link CallGraph} implementations.
 * This class contains the data structures and functions for storing and
 * accessing information of a call graph. Simplified for languages without
 * polymorphism where each call site maps to exactly one function.
 *
 * @param <CallSite> type of call sites
 * @param <Function> type of Functions
 */
public abstract class AbstractCallGraph<CallSite, Function>
        implements CallGraph<CallSite, Function> {

    // Direct mapping: one call site -> one function
    protected final Map<CallSite, Function> callSiteToCallee = Maps.newMap();

    // Reverse mapping: function -> all call sites that call it
    protected final MultiMap<Function, CallSite> calleeToCallSites = Maps.newMultiMap();

    // Call site container mapping
    protected final Map<CallSite, Function> callSiteToContainer = Maps.newMap();

    // Function -> call sites contained within it
    protected final MultiMap<Function, CallSite> callSitesIn = Maps.newMultiMap(Sets::newHybridOrderedSet);

    protected final Set<Function> entryFunctions = Sets.newSet();

    /**
     * Set of reachable Functions. This field is not final so that
     * it allows subclasses choose more efficient data structure.
     */
    protected Set<Function> reachableFunctions = Sets.newSet();

    @Override
    public Set<CallSite> getCallersOf(Function callee) {
        return calleeToCallSites.get(callee);
    }

    @Override
    public Function getCalleeOf(CallSite callSite) {
        return callSiteToCallee.get(callSite);
    }

    @Override
    public Set<Function> getCalleesOfF(Function caller) {
        return callSitesIn.get(caller)
                .stream()
                .map(callSiteToCallee::get)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Function getContainerOf(CallSite callSite) {
        return callSiteToContainer.get(callSite);
    }

    @Override
    public Set<CallSite> getCallSitesIn(Function function) {
        return callSitesIn.get(function);
    }

    @Override
    public Edge<CallSite, Function> edgeOutOf(CallSite callSite) {
        Function callee = callSiteToCallee.get(callSite);
        return callee != null ? new Edge<>(callSite, callee) : null;
    }

    @Override
    public Stream<Edge<CallSite, Function>> edgesInTo(Function function) {
        return calleeToCallSites.get(function)
                .stream()
                .map(this::edgeOutOf);
    }

    // Deprecated methods for backward compatibility
    @Deprecated
    public Edge<CallSite, Function> getEdgeFrom(CallSite callSite) {
        return edgeOutOf(callSite);
    }

    @Deprecated
    public Stream<Edge<CallSite, Function>> getEdgesTo(Function function) {
        return edgesInTo(function);
    }

    @Override
    public Stream<Edge<CallSite, Function>> edges() {
        return callSiteToCallee.entrySet()
                .stream()
                .map(entry -> new Edge<>(entry.getKey(), entry.getValue()));
    }

    @Override
    public int getNumberOfEdges() {
        return callSiteToCallee.size();
    }

    @Override
    public Function entryFunction() {
        return entryFunctions.isEmpty() ? null : entryFunctions.iterator().next();
    }

    /**
     * @return all entry functions as a stream (for languages that support multiple entry points).
     */
    public Stream<Function> entryFunctions() {
        return entryFunctions.stream();
    }

    @Override
    public Stream<Function> reachableFunctions() {
        return reachableFunctions.stream();
    }

    @Override
    public int getNumberOfFunctions() {
        return reachableFunctions.size();
    }

    @Override
    public boolean contains(Function function) {
        return reachableFunctions.contains(function);
    }

    // Implementation for Graph interface.

    @Override
    public Set<FunctionEdge<CallSite, Function>> getInEdgesOf(Function function) {
        return getCallersOf(function)
                .stream()
                .map(cs -> new FunctionEdge<>(getContainerOf(cs), function, cs))
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Set<FunctionEdge<CallSite, Function>> getOutEdgesOf(Function function) {
        return callSitesIn.get(function)
                .stream()
                .map(cs -> new FunctionEdge<>(function, callSiteToCallee.get(cs), cs))
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Set<Function> getPredsOf(Function node) {
        return getCallersOf(node)
                .stream()
                .map(this::getContainerOf)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Set<Function> getSuccsOf(Function node) {
        return getCalleesOfF(node);
    }

    @Override
    public Set<Function> getNodes() {
        return Collections.unmodifiableSet(reachableFunctions);
    }

    /**
     * Helper method for subclasses to add a call edge.
     * This maintains all the internal data structures consistently.
     */
    protected void addCallEdge(CallSite callSite, Function container, Function callee) {
        callSiteToCallee.put(callSite, callee);
        calleeToCallSites.put(callee, callSite);
        callSiteToContainer.put(callSite, container);
        callSitesIn.put(container, callSite);
        reachableFunctions.add(container);
        reachableFunctions.add(callee);
    }

    /**
     * Helper method for subclasses to remove a call edge.
     */
    protected void removeCallEdge(CallSite callSite) {
        Function callee = callSiteToCallee.remove(callSite);
        Function container = callSiteToContainer.remove(callSite);

        if (callee != null) {
            calleeToCallSites.remove(callee, callSite);
        }
        if (container != null) {
            callSitesIn.remove(container, callSite);
        }
    }

    /**
     * Helper method for subclasses to add an entry function.
     */
    protected void addEntryFunction(Function function) {
        entryFunctions.add(function);
        reachableFunctions.add(function);
    }

    /**
     * Helper method for subclasses to remove an entry function.
     */
    protected void removeEntryFunction(Function function) {
        entryFunctions.remove(function);
    }
}