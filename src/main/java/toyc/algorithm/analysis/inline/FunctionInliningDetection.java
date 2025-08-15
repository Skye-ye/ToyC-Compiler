package toyc.algorithm.analysis.inline;

import toyc.algorithm.analysis.FunctionAnalysis;
import toyc.config.AlgorithmConfig;
import toyc.ir.IR;
import toyc.ir.stmt.Call;
import toyc.language.Function;

import java.util.Set;

import static java.util.stream.Collectors.toSet;

public class FunctionInliningDetection extends FunctionAnalysis<Set<Call>>
{
    public static final String ID = "func-inline";

    public FunctionInliningDetection(AlgorithmConfig config) {
        super(config);
    }

    @Override
    public Set<Call> analyze(IR ir) {
        Function function = ir.getFunction();
        // For now, we regard all calls that are not self-recursive as candidates for inlining.
        return ir.calls()
                .filter(call -> !call.getCallExp().getFunction().equals(function))
                .collect(toSet());
    }
}
