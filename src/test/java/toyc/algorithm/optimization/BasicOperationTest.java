package toyc.algorithm.optimization;

import toyc.ir.*;
import toyc.ir.stmt.Stmt;

public class BasicOperationTest {
    IROperation op;

    public BasicOperationTest(IR ir) {
        this.op = new IROperation(ir);
    }

    public IR test() {
        System.out.println("=== 初始IR ===");
        IRPrinter.print(op.getCurrentIR(), System.out);
        
        removeTest();
        insertTest();
        replaceTest();


        return op.getCurrentIR();
    }

    public void removeTest() {
        System.out.println("\n=== 删除第0条语句 ===");
        op.removeByOrigin(0);
        IRPrinter.print(op.getCurrentIR(), System.out);
    }

    public void insertTest() {
        System.out.println("\n=== 在第1位置插入第0条语句 ===");
        Stmt stmt = op.getStmt(0);
        assert stmt != null;
        op.insertByOrigin(stmt, 1);
        IRPrinter.print(op.getCurrentIR(), System.out);
    }

    public void replaceTest() {
        System.out.println("\n=== 替换第2条语句为nop ===");
        op.replaceWithNop(2);
        IRPrinter.print(op.getCurrentIR(), System.out);
    }


}