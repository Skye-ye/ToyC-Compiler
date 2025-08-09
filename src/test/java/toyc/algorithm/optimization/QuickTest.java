package toyc.algorithm.optimization;

import toyc.ir.*;

public class QuickTest {
    
    public static void main(String[] args) {
        System.out.println("快速测试");
        
        IR ir = ManualIRCreator.createSampleIR();
        BasicOperationTest test = new BasicOperationTest(ir);
        IR result = test.test();
        
        System.out.println("测试完成, 最终语句数: " + result.getStmts().size());
    }
}