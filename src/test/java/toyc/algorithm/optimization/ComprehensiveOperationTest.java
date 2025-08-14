// package toyc.algorithm.optimization;

// import toyc.ir.*;
// import toyc.ir.stmt.*;

// /**
//  * 全面测试AbstractOperation的功能，覆盖未测试的方法和边缘情况
//  */
// public class ComprehensiveOperationTest {
//     private final IROperation op;

//     public ComprehensiveOperationTest(IR ir) {
//         this.op = new IROperation(ir);
//     }

//     public IR testAll() {
//         System.out.println("=== 全面操作测试开始 ===");
//         System.out.println("初始IR：");
//         IRPrinter.print(op.getIR(), System.out);

//         testRemoveTarget();
//         testIndexMapping();
//         testInvalidIndex();
//         testEdgeCases();

//         System.out.println("=== 全面操作测试结束 ===");
//         IRPrinter.print(op.getIR(), System.out);
//         return op.getIR();
//     }

//     private void testRemoveTarget() {
//         System.out.println("\n=== 测试在索引7（If语句的目标）上执行removeTarget ===");
//         // 索引7是一个Copy语句，由索引4的If语句引用
//         boolean success = op.removeTarget(7);
//         System.out.println("在原始索引7处删除目标：" + (success ? "成功" : "失败"));
//         IRPrinter.print(op.getIR(), System.out);

//         // 验证索引4处的If语句现在指向正确的新目标
//         Stmt ifStmt = op.getStmt(op.getCurrentIndex(4));
//         if (ifStmt instanceof If) {
//             Stmt newTarget = ((If) ifStmt).getTarget();
//             System.out.println("删除后If语句的目标：" + (newTarget != null ? newTarget.toString() : "null"));
//         }
//     }

//     private void testIndexMapping() {
//         System.out.println("\n=== 测试索引映射 ===");
//         // 在原始索引2处插入一个NOP语句
//         Stmt nopStmt = new Nop();
//         op.insertByOrigin(nopStmt, 2);
//         System.out.println("在原始索引2处插入NOP后：");
//         IRPrinter.print(op.getIR(), System.out);

//         // 检查原始索引的当前索引
//         Integer currentIndex = op.getCurrentIndex(2);
//         System.out.println("原始索引2的当前索引：" + currentIndex);
//         currentIndex = op.getCurrentIndex(3);
//         System.out.println("原始索引3的当前索引：" + currentIndex);
//     }

//     private void testInvalidIndex() {
//         System.out.println("\n=== 测试无效索引 ===");
//         // 测试无效的原始索引
//         boolean isValid = op.isValidOriginalIndex(100);
//         System.out.println("原始索引100是否有效：" + isValid);

//         // 尝试在无效索引处插入
//         Stmt nopStmt = new Nop();
//         boolean success = op.insertByOrigin(nopStmt, 100);
//         System.out.println("在无效索引100处插入：" + (success ? "成功" : "失败"));

//         // 尝试删除无效索引
//         success = op.removeByOrigin(100);
//         System.out.println("删除无效索引100：" + (success ? "成功" : "失败"));
//     }

//     private void testEdgeCases() {
//         System.out.println("\n=== 测试边缘情况 ===");
//         // 测试在IR开头插入
//         Stmt nopStmt = new Nop();
//         op.insertByOrigin(nopStmt, 0);
//         System.out.println("在原始索引0处插入NOP后：");
//         IRPrinter.print(op.getIR(), System.out);

//         // 测试删除最后一个语句
//         int lastIndex = op.getIR().getStmts().size() - 1;
//         op.removeByOrigin(lastIndex);
//         System.out.println("删除最后一个语句后：");
//         IRPrinter.print(op.getIR(), System.out);
//     }
// }