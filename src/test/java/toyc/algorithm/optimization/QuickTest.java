// package toyc.algorithm.optimization;

// import toyc.ir.*;

// public class QuickTest {
    
//     public static void main(String[] args) {
//         System.out.println("快速测试");
        
//         IR ir = ManualIRCreator.createSampleIR();
//         BasicOperationTest test = new BasicOperationTest(ir);
//         IR result = test.test();
        
//         System.out.println("测试完成, 最终语句数: " + result.getStmts().size());

//         // 测试复杂IR的ComprehensiveOperationTest
//         System.out.println("\n=== 运行全面操作测试 ===");
//         IR complexIR = ManualIRCreator.createComplexSampleIR();
//         ComprehensiveOperationTest comprehensiveTest = new ComprehensiveOperationTest(complexIR);
//         IR comprehensiveResult = comprehensiveTest.testAll();
//         System.out.println("全面操作测试完成，最终语句数: " + comprehensiveResult.getStmts().size());
//     }
// }