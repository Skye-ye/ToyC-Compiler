package toyc.frontend.ir;

import toyc.ir.exp.Var;

import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

public class Scope {
    private final Scope parent;
    private final Map<String, Var> variables;

    public Scope(Scope parent) {
        this.parent = parent;
        this.variables = new HashMap<>();
    }

    public Scope getParent() {
        return parent;
    }

    public void define(String name, Var var) {
        // Check for redefinition in the current scope
        if (variables.containsKey(name)) {
            throw new IllegalArgumentException("Variable " + name + " is already defined in this scope.");
        }
        variables.put(name, var);
    }

    public Var resolve(String name) {
        Var var = variables.get(name);
        if (var != null) {
            return var;
        }
        if (parent != null) {
            return parent.resolve(name);
        }
        return null; // Not found in this scope or any parent scope
    }

    public Set<Var> getAllVariables() {
        Set<Var> allVars = new HashSet<>(variables.values());
        if (parent != null) {
            allVars.addAll(parent.getAllVariables());
        }
        return allVars;
    }
}
