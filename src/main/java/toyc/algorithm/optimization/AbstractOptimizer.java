package toyc.algorithm.optimization;

import toyc.ir.IR;
import toyc.ir.stmt.Stmt;

public class AbstractOptimizer implements Optimizer {
    @Override
    public void insert(IR ir, Stmt stmt, int index) {
        // TODO: Basic insertion logic.
    }

    @Override
    public void remove(IR ir, int index) {
        // TODO: Basic removal logic.
    }
}
