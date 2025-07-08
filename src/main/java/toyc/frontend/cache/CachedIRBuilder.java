package toyc.frontend.cache;


import toyc.ir.IR;
import toyc.ir.IRBuilder;
import toyc.language.Function;
import toyc.language.Program;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * The {@link toyc.ir.IRBuilder} is for keeping the {@link IR}s of all methods to
 * prevent cyclic references with too long a path which may make
 * the serialization fail or {@link StackOverflowError}.
 */
public class CachedIRBuilder implements IRBuilder {

    private final Map<String, IR> functionName2IR;

    public CachedIRBuilder(IRBuilder irBuilder,  Program program) {
        irBuilder.buildAll(program);
        functionName2IR = program.allFunctions()
                        .collect(Collectors.toMap(Function::getName, Function::getIR));
    }

    /**
     * This method will be called by {@link Function#getIR()} only once,
     * so remove the IR from the map after returning it.
     */
    @Override
    public IR buildIR(Function function) {
        return functionName2IR.remove(function.getName());
    }

    @Override
    public void buildAll(Program program) {
        program.allFunctions().forEach(Function::getIR);
    }
}
