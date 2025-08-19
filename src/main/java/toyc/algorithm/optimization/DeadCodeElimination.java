package toyc.algorithm.optimization;

import toyc.algorithm.analysis.deadcode.DeadCodeDetection;
import toyc.config.AlgorithmConfig;
import toyc.ir.IR;
import toyc.ir.stmt.Goto;
import toyc.ir.stmt.If;
import toyc.ir.stmt.Stmt;

import java.util.Map;

public class DeadCodeElimination extends  Optimization {
    public static final String ID = "dead-code-elim";

    public DeadCodeElimination(AlgorithmConfig config) {
        super(config);
    }

    @Override
    public IR optimize(IR ir) {
        IROperation operation = new IROperation(ir);
        Map<Stmt, DeadCodeDetection.Kind> deadStmts =
                ir.getResult(DeadCodeDetection.ID);

        for (Map.Entry<Stmt, DeadCodeDetection.Kind> entry : deadStmts.entrySet()) {
            Stmt stmt = entry.getKey();
            DeadCodeDetection.Kind kind = entry.getValue();
            if (kind == DeadCodeDetection.Kind.NORMAL) {
                operation.remove(stmt);
            } else if (kind == DeadCodeDetection.Kind.IF_TRUE) {
                Goto gotoStmt = new Goto();
                assert stmt instanceof If;
                Stmt target = operation.getNextStmt(stmt);
                assert target != null;
                assert !deadStmts.containsKey(target);
                gotoStmt.setTarget(target);
                operation.replace(stmt, gotoStmt);
            } else if (kind == DeadCodeDetection.Kind.IF_FALSE) {
                Goto gotoStmt = new Goto();
                assert stmt instanceof If;
                Stmt target = ((If) stmt).getTarget();
                assert target != null;
                assert !deadStmts.containsKey(target);
                gotoStmt.setTarget(target);
                operation.replace(stmt, gotoStmt);
            }
        }

        return operation.getIR();
    }
}
