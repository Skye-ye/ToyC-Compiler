package toyc.frontend.ir;

import toyc.ir.exp.IntLiteral;
import toyc.ir.exp.Var;
import toyc.language.Function;
import toyc.language.type.IntType;

import java.util.*;

/**
 * Manages variable creation, scope handling, and naming for IR generation.
 * Handles temporary variables, constants, parameters, and local variables.
 */
public class VarManager {

    private static final String TEMP_PREFIX = "temp$";
    private static final String CONST_PREFIX = "%intconst";
    private static final String VAR_SUFFIX_SEPARATOR = "_";

    private final Function function;

    private int varCounter = 0;
    private int tempCounter = 0;
    private int constCounter = 0;
    private int varIndex = 0;

    // Scope management
    private Scope scope;
    private final Map<String, Integer> variableCounters; // For handling shadowing

    public VarManager(Function function) {
        this.function = function;
        this.scope = new Scope(null);
        this.variableCounters = new HashMap<>();
    }

    /**
     * Create a new temporary variable.
     * @return A new temporary variable with a unique name.
     */
    public Var createTemp() {
        return new Var(function, TEMP_PREFIX + tempCounter++, IntType.INT, varIndex++);
    }

    /**
     * Create a constant variable with int literal.
     * @param literal the integer literal for the constant variable.
     * @return A new constant variable with a unique name.
     */
    public Var createConstVar(IntLiteral literal) {
        return new Var(function, CONST_PREFIX + constCounter++, IntType.INT, varIndex++, literal);
    }

    /**
     * Create a local variable with the given name.
     * @param name the name of the local variable.
     * @return A new local variable with the specified name.
     */
    public Var createLocalVariable(String name) {
        return new Var(function, name, IntType.INT, varCounter++);
    }

    /**
     * Enter a new scope.
     */
    public void enterScope() {
        scope = new Scope(scope);
    }

    /**
     * Exit the current scope.
     */
    public void exitScope() {
        scope = scope.getParent();
    }

    /**
     * Define a variable in the current scope.
     */
    public void defineVariable(String name, Var var) {
        scope.define(name, var);
    }

    /**
     * Look up a variable by name, searching from innermost to outermost scope.
     * @param name the name of the variable to look up.
     * @return The variable if found, or null if not found.
     */
    public Var lookupVariable(String name) {
        return scope.resolve(name);
    }

    /**
     * Handle variable shadowing by generating unique names if needed.
     */
    public String handleVariableShadowing(String originalName) {
        if (lookupVariable(originalName) != null) {
            return generateUniqueVariableName(originalName);
        }
        return originalName;
    }

    /**
     * Generate a unique variable name by appending a counter.
     */
    private String generateUniqueVariableName(String originalName) {
        int counter = variableCounters.getOrDefault(originalName, 0) + 1;
        variableCounters.put(originalName, counter);
        return originalName + VAR_SUFFIX_SEPARATOR + counter;
    }

    /**
     * Collect all variables from all scopes, excluding parameters.
     */
    public List<Var> collectAllVariables(List<Var> params) {
        List<Var> allVars = new ArrayList<>(params);

        // Build set of parameter names for exclusion
        Set<String> paramNames = new HashSet<>();
        for (Var param : params) {
            paramNames.add(param.getName());
        }

        // Add local variables (excluding parameters)
        Set<Var> allScopeVars = scope.getAllVariables();
        for (Var var : allScopeVars) {
            if (!paramNames.contains(var.getName())) {
                allVars.add(var);
            }
        }

        return allVars;
    }
}