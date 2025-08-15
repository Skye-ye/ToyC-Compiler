package toyc.algorithm.optimization;

import toyc.algorithm.analysis.inline.FunctionInliningDetection;
import toyc.config.AlgorithmConfig;
import toyc.ir.IR;
import toyc.ir.exp.Var;
import toyc.ir.stmt.*;

import java.util.*;
import java.util.stream.Collectors;

public class FunctionInlining extends Optimization {
    public static final String ID = "func-inline-opt";

    private static final String VAR_SUFFIX = "$";

    private IROperation operation;

    private IR callerIR;

    private Set<String> varNames;

    public FunctionInlining(AlgorithmConfig config) {
        super(config);
    }

    @Override
    public IR optimize(IR ir) {
        operation = new IROperation(ir);
        callerIR = ir;
        varNames = callerIR.getVars().stream()
                .map(Var::getName)
                .collect(Collectors.toSet());
        Set<Call> callsToInline = ir.getResult(FunctionInliningDetection.ID);
        for (Call call : callsToInline) {
            inlineCall(call);
        }
        return operation.getIR();
    }

    private void inlineCall(Call call) {
        Map<Var, Var> varMapping = createVarMapping(call);
        List<Stmt> inlinedStmts = new LinkedList<>(
                StmtListCopier.copy(
                        call.getCallExp().getFunction().getIR().getStmts(),
                        varMapping,
                        callerIR.getFunction()));

        // Assign constant to callee params if caller args are intconst
        List<Var> callerArgs = call.getCallExp().getArgs();
        List<Var> calleeParams = call.getCallExp().getFunction().getIR().getParams();
        for (int i = 0; i < calleeParams.size(); i++) {
            Var param = calleeParams.get(i);
            Var arg = callerArgs.get(i);
            if (arg.isConst()) {
                inlinedStmts.addFirst(new AssignLiteral(param,
                        arg.getConstValue()));
            }
        }

        Var callerReturnVar = call.getResult();
        Stmt stmtAfterCall = operation.getNextStmt(call);
        assert stmtAfterCall != null; // For now, we only deal with non-tail calls

        operation.replace(call, inlinedStmts);

        for (Stmt stmt : inlinedStmts) {
            if (stmt instanceof Return returnStmt) {
                if (callerReturnVar != null) {
                    Var calleeReturnVar = returnStmt.getValue();
                    assert calleeReturnVar != null; // When callerReturnVar is not null, calleeReturnVar must also be non-null
                    Stmt copy = new Copy(callerReturnVar, calleeReturnVar);
                    operation.replace(stmt, copy);
                    Goto gotoStmt = new Goto();
                    gotoStmt.setTarget(stmtAfterCall);
                    operation.insertAfter(copy, gotoStmt);
                } else {
                    Goto gotoStmt = new Goto();
                    gotoStmt.setTarget(stmtAfterCall);
                    operation.insertAfter(stmt, gotoStmt);
                }
            }
        }
    }

    private Map<Var, Var> createVarMapping(Call call) {
        IR calleeIR = call.getCallExp().getFunction().getIR();
        List<Var> calleeParams = calleeIR.getParams();
        List<Var> callerArgs = call.getCallExp().getArgs();
        List<Var> calleeVars = calleeIR.getVars();
        Map<Var, Var> varMapping = new HashMap<>();

        for (int i = 0; i < calleeParams.size(); i++) {
            Var param = calleeParams.get(i);
            Var arg = callerArgs.get(i);
            if (arg.isConst()) {
                // Arg is constant, so we simply assign callee's param with
                // constant later
                varMapping.put(param, param); // Identical
            } else {
                varMapping.put(param, arg);
            }
        }

        for (Var calleeVar : calleeVars) {
            if (!varMapping.containsKey(calleeVar)) {
                String newVarName = getNewVarName(calleeVar);
                varNames.add(newVarName);
                Var clonedVar = new Var(
                        callerIR.getFunction(),
                        newVarName,
                        calleeVar.getType(),
                        -1,
                        calleeVar.isConst() ? calleeVar.getConstValue() : null);
                varMapping.put(calleeVar, clonedVar);
            }
        }

        return varMapping;
    }

    private String getNewVarName(Var var) {
        String name = var.getName();
        while (varNames.contains(name)) {
            name += VAR_SUFFIX;
        }
        return name;
    }
}
