package toyc.ir.instruction;

import toyc.ir.InstructionVisitor;
import toyc.ir.value.Value;
import toyc.ir.value.Variable;

import java.util.List;

public class CallInstruction extends Instruction {
    private final Variable result;
    private final String functionName;
    private final List<Value> arguments;
    
    public CallInstruction(Variable result, String functionName, List<Value> arguments) {
        super();
        this.result = result;
        this.functionName = functionName;
        this.arguments = arguments;
    }
    
    public Variable getResult() {
        return result;
    }
    
    public String getFunctionName() {
        return functionName;
    }
    
    public List<Value> getArguments() {
        return arguments;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (result != null) {
            sb.append(result).append(" = ");
        }
        sb.append("call ").append(functionName).append("(");
        for (int i = 0; i < arguments.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(arguments.get(i));
        }
        sb.append(")");
        return sb.toString();
    }
    
    @Override
    public void accept(InstructionVisitor visitor) {
        visitor.visit(this);
    }
}