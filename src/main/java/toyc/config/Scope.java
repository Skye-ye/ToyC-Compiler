package toyc.config;

public enum Scope {

    /**
     * Only analyzes the code reachable in the call graph.
     * This scope requires to perform a call graph construction in advance.
     */
    REACHABLE,

    /**
     * Analyzes all code.
     */
    ALL,
}
