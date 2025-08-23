package toyc.algorithm.optimization;

import toyc.World;
import toyc.algorithm.analysis.dataflow.analysis.constprop.CPFact;
import toyc.algorithm.analysis.dataflow.analysis.constprop.Value;
import toyc.algorithm.analysis.dataflow.fact.NodeResult;
import toyc.algorithm.analysis.dataflow.inter.InterConstantPropagation;
import toyc.config.AlgorithmConfig;
import toyc.ir.IR;
import toyc.ir.exp.*;
import toyc.ir.stmt.*;
import toyc.util.NumericSuffixNaming;

import java.util.List;
import java.util.stream.Collectors;

public class ConstantFolding extends Optimization {
    public static final String ID = "const-fold";

    private static final String INT_CONST = "%intconst";

    private IROperation operation;

    private NumericSuffixNaming nameManager;

    public ConstantFolding(AlgorithmConfig config) {
        super(config);
    }

    @Override
    public IR optimize(IR ir) {
        operation = new IROperation(ir);
        nameManager = new NumericSuffixNaming(ir.getVars().stream()
                .map(Var::getName)
                .collect(Collectors.toSet()));
        NodeResult<Stmt, CPFact> constants = World.get().getResult(InterConstantPropagation.ID);

        List<Stmt> stmts = ir.getStmts();

        for (Stmt stmt : stmts) {
            CPFact fact = constants.getResult(stmt);
            foldStatement(stmt, fact);
        }

        return operation.getIR();
    }

    private void foldStatement(Stmt stmt, CPFact fact) {
        if (stmt instanceof Return returnStmt) {
            foldReturn(returnStmt, fact);
        } else if (stmt instanceof AssignStmt<?, ?> assignStmt) {
            foldAssign(assignStmt, fact);
        }
    }

    private void foldAssign(AssignStmt<?, ?> assignStmt, CPFact fact) {
        if (assignStmt instanceof AssignLiteral) {
            return;
        }
        if (assignStmt.getLValue() instanceof Var var) {
            Value value = fact.get(var);
            if (value.isConstant()) {
                // Constant assignment, replace the statement with AssignLiteral
                int constValue = value.getConstant();
                AssignLiteral assignLiteral = new AssignLiteral(var,
                        IntLiteral.get(constValue));
                operation.replace(assignStmt, assignLiteral);
            }
        }
    }

    private void foldReturn(Return returnStmt, CPFact fact) {
        Var retVar = returnStmt.getValue();
        if (retVar != null && !retVar.isConst()) {
            Value retValue = fact.get(retVar);
            if (retValue.isConstant()) {
                // Constant return value, replace Var with intconst
                int constValue = retValue.getConstant();
                Var constVar = new Var(retVar.getFunction(),
                        nameManager.getNewVarName(INT_CONST),
                        retVar.getType(), -1, IntLiteral.get(constValue));
                Return newReturnStmt = new Return(constVar);
                AssignLiteral assignStmt = new AssignLiteral(constVar, IntLiteral.get(constValue));
                operation.replace(returnStmt, assignStmt);
                operation.insertAfter(assignStmt, newReturnStmt);
            }
        }
    }
}
