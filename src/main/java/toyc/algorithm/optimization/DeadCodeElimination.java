package toyc.algorithm.optimization;

import toyc.algorithm.analysis.deadcode.DeadCodeDetection;
import toyc.config.AlgorithmConfig;
import toyc.ir.IR;
import toyc.ir.stmt.Stmt;

import java.util.Set;

public class DeadCodeElimination extends  Optimization {
    public static final String ID = "dead-code-elim";

    public DeadCodeElimination(AlgorithmConfig config) {
        super(config);
    }

    @Override
    public IR optimize(IR ir) {
        IROperation operation = new IROperation(ir);
        Set<Stmt> deadStmts = ir.getResult(DeadCodeDetection.ID);

        for (Stmt stmt : deadStmts) {
            operation.remove(stmt);
        }

        return operation.getIR();
    }
}
