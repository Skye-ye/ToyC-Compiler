package toyc.algorithm.optimization.constfold;

import toyc.World;
import toyc.algorithm.analysis.dataflow.analysis.constprop.CPFact;
import toyc.algorithm.analysis.dataflow.analysis.constprop.Value;
import toyc.algorithm.analysis.dataflow.fact.NodeResult;
import toyc.algorithm.analysis.dataflow.inter.InterConstantPropagation;
import toyc.algorithm.optimization.IROperation;
import toyc.algorithm.optimization.Optimization;
import toyc.config.AlgorithmConfig;
import toyc.ir.IR;
import toyc.ir.exp.*;
import toyc.ir.stmt.*;

import java.util.List;

public class ConstantFolding extends Optimization {
    public static final String ID = "const-fold";

    private IROperation operation;

    public ConstantFolding(AlgorithmConfig config) {
        super(config);
    }

    @Override
    public IR optimize(IR ir) {
        operation = new IROperation(ir);
        NodeResult<Stmt, CPFact> constants = World.get().getResult(InterConstantPropagation.ID);

        List<Stmt> stmts = List.copyOf(operation.getIR().getStmts());

        for (int i = 0; i < stmts.size(); i++) {
            Stmt stmt = stmts.get(i);
            CPFact fact = constants.getResult(stmt);
            foldStatement(stmt, fact, i);
        }

        return operation.getIR();
    }

    private void foldStatement(Stmt stmt, CPFact fact, int index) {
        if (stmt instanceof Return returnStmt) {
            foldReturn(returnStmt, fact, index);
        }
    }

    private void foldReturn(Return returnStmt, CPFact fact, int index) {
        Var retVar = returnStmt.getValue();
        if (retVar != null) {
            Value retValue = fact.get(retVar);
            if (retValue.isConstant()) {
                // Constant return value, replace Var with intconst
                int constValue = retValue.getConstant();
                Var constVar = new Var(retVar.getFunction(), "__const_ret__",
                        retVar.getType(), index, IntLiteral.get(constValue));
                Return newReturnStmt = new Return(constVar);;
                AssignLiteral assignStmt = new AssignLiteral(constVar, IntLiteral.get(constValue));
                operation.replace(returnStmt, newReturnStmt);
                operation.insertBefore(newReturnStmt, assignStmt);
            }
        }
    }
}
