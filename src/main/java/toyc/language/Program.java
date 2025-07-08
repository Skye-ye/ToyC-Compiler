package toyc.language;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.stream.Stream;

public class Program {
    private final List<Function> functions;
    private final Map<String, Function> functionMap;
    private final Function mainFunction;

    public Program(List<Function> functions) {
        this.functions = List.copyOf(functions);
        this.functionMap = new LinkedHashMap<>();
        Function main = null;
        
        for (Function function : functions) {
            functionMap.put(function.getName(), function);
            if ("main".equals(function.getName())) {
                main = function;
            }
        }
        
        this.mainFunction = main;
    }

    public Stream<Function> allFunctions() {
        return functions.stream();
    }

    public Optional<Function> getFunction(String name) {
        return Optional.ofNullable(functionMap.get(name));
    }

    public boolean hasFunction(String name) {
        return functionMap.containsKey(name);
    }

    public Function getMainFunction() {
        return mainFunction;
    }

    public boolean hasMainFunction() {
        return mainFunction != null;
    }

    public int getFunctionCount() {
        return functions.size();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Program with ").append(functions.size()).append(" functions:\n");
        for (Function function : functions) {
            sb.append("  ").append(function.toString()).append("\n");
        }
        return sb.toString();
    }
}
