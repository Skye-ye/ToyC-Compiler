package toyc.frontend.semantic.symbol;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import toyc.language.type.Type;

public class SymbolTable {
    private final SymbolTable parent;
    private final HashMap<String, Type> symbols;
    private final List<Type> types;

    public SymbolTable(SymbolTable parent) {
        this.parent = parent;
        this.symbols = new HashMap<>();
        this.types = new ArrayList<>();
    }

    public SymbolTable getParent() {
        return parent;
    }

    public void define(String name, Type type) {
        symbols.put(name, type);
        types.add(type);
    }

    // Look for symbol in current scope and parent scopes
    public Type resolve(String name) {
        Type type = find(name);
        if (type != null) {
            return type;
        }
        if (parent != null) {
            return parent.resolve(name);
        }
        return null;
    }

    // Look for symbol in current scope
    public Type find(String name) {
        return symbols.get(name);
    }

    // Return the list of types
    public List<Type> getTypes() {
        return types;
    }
}
