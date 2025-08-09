package toyc.algorithm.optimization;

import toyc.ir.exp.*;
import toyc.ir.stmt.*;
import toyc.ir.*;
import toyc.language.Function;
import toyc.language.type.IntType;
import toyc.language.type.VoidType;

import java.util.ArrayList;
import java.util.List;

/**
 * 手工创建IR对象的工具类，用于测试目的
 */
public class ManualIRCreator {

    public static IR createSampleIR() {
        // 创建一个简单的函数：int add(int a, int b) { return a + b; }
        Function function = createSampleFunction();
        
        // 使用IRBuildHelper来构建IR
        IRBuildHelper helper = new IRBuildHelper(function);
        
        // 获取参数变量
        Var param_a = helper.getParam(0);  // %param0 (int a)
        Var param_b = helper.getParam(1);  // %param1 (int b)
        
        // 创建临时变量用于存储计算结果
        Var temp = helper.newTempVar(IntType.INT);  // %temp0
        
        // 获取返回变量
        Var returnVar = helper.getReturnVar();  // %return
        
        // 创建语句列表
        List<Stmt> statements = new ArrayList<>();
        
        // 1. 二元运算：%temp0 = %param0 + %param1
        ArithmeticExp addExp = new ArithmeticExp(ArithmeticExp.Op.ADD, param_a, param_b);
        Binary binaryStmt = new Binary(temp, addExp);
        statements.add(binaryStmt);
        
        // 2. 复制语句：%return = %temp0
        Copy copyStmt = new Copy(returnVar, temp);
        statements.add(copyStmt);
        
        // 3. 返回语句
        Return returnStmt = helper.newReturn();
        statements.add(returnStmt);
        
        // 构建并返回IR
        return helper.build(statements);
    }
    
    public static IR createComplexSampleIR() {
        // 创建一个更复杂的函数：int complex(int x, int y)
        Function function = createComplexFunction();
        IRBuildHelper helper = new IRBuildHelper(function);
        
        // 参数变量
        Var param_x = helper.getParam(0);  // %param0
        Var param_y = helper.getParam(1);  // %param1
        
        // 临时变量
        Var temp1 = helper.newTempVar(IntType.INT);  // %temp0
        Var temp2 = helper.newTempVar(IntType.INT);  // %temp1
        Var temp3 = helper.newTempVar(IntType.INT);  // %temp2
        Var temp4 = helper.newTempVar(IntType.INT);  // %temp3
        
        Var returnVar = helper.getReturnVar();
        
        List<Stmt> statements = new ArrayList<>();
        
        // 1. 常量赋值：%temp0 = 10
        IntLiteral literal10 = IntLiteral.get(10);
        AssignLiteral assignLit = new AssignLiteral(temp1, literal10);
        statements.add(assignLit);
        
        // 2. 乘法：%temp1 = %param0 * %temp0
        ArithmeticExp mulExp = new ArithmeticExp(ArithmeticExp.Op.MUL, param_x, temp1);
        Binary mulStmt = new Binary(temp2, mulExp);
        statements.add(mulStmt);
        
        // 3. 加法：%temp2 = %temp1 + %param1
        ArithmeticExp addExp = new ArithmeticExp(ArithmeticExp.Op.ADD, temp2, param_y);
        Binary addStmt = new Binary(temp3, addExp);
        statements.add(addStmt);
        
        // 4. 取负：%temp3 = -%temp2
        NegExp negExp = new NegExp(temp3);
        Unary negStmt = new Unary(temp4, negExp);
        statements.add(negStmt);
        
        // 5. 条件判断：if (%param0 > %param1) goto [7]
        ConditionExp condExp = new ConditionExp(ConditionExp.Op.GT, param_x, param_y);
        If ifStmt = new If(condExp);
        statements.add(ifStmt);
        
        // 6. 复制（条件为false时执行）：%return = %temp3
        Copy copyStmt1 = new Copy(returnVar, temp4);
        statements.add(copyStmt1);
        
        // 7. goto [9] (跳过下一个语句)
        Goto gotoStmt = new Goto();
        statements.add(gotoStmt);
        
        // 8. 复制（条件为true时执行）：%return = %temp2
        Copy copyStmt2 = new Copy(returnVar, temp3);
        statements.add(copyStmt2);
        
        // 9. nop (跳转目标)
        Nop nopStmt = new Nop();
        statements.add(nopStmt);
        
        // 10. 返回语句
        Return returnStmt = helper.newReturn();
        statements.add(returnStmt);
        
        // 构建IR
        IR ir = helper.build(statements);
        
        // 设置跳转目标（需要在语句有索引之后）
        ifStmt.setTarget(ir.getStmt(7));  // if跳转到第8个语句（索引7）
        gotoStmt.setTarget(ir.getStmt(9)); // goto跳转到第10个语句（索引9）
        
        return ir;
    }
    
    public static IR createCallSampleIR() {
        // 创建包含函数调用的IR
        Function mainFunc = createMainFunction();
        Function calledFunc = createSampleFunction(); // 被调用的add函数
        
        IRBuildHelper helper = new IRBuildHelper(mainFunc);
        
        // 创建变量
        Var temp1 = helper.newTempVar(IntType.INT);  // %temp0
        Var temp2 = helper.newTempVar(IntType.INT);  // %temp1
        Var temp3 = helper.newTempVar(IntType.INT);  // %temp2
        
        List<Stmt> statements = new ArrayList<>();
        
        // 1. %temp0 = 5
        AssignLiteral assign1 = new AssignLiteral(temp1, IntLiteral.get(5));
        statements.add(assign1);
        
        // 2. %temp1 = 3
        AssignLiteral assign2 = new AssignLiteral(temp2, IntLiteral.get(3));
        statements.add(assign2);
        
        // 3. %temp2 = call add(%temp0, %temp1)
        List<Var> args = List.of(temp1, temp2);
        CallExp callExp = new CallExp(calledFunc, args);
        Call callStmt = new Call(mainFunc, callExp, temp3);
        statements.add(callStmt);
        
        // 4. return
        Return returnStmt = new Return(); // void函数的return
        statements.add(returnStmt);
        
        return helper.build(statements);
    }
    
    // 辅助方法：创建简单的add函数
    private static Function createSampleFunction() {
        return new Function("add", List.of(IntType.INT, IntType.INT), IntType.INT, null);
    }
    
    // 辅助方法：创建复杂函数
    private static Function createComplexFunction() {
        return new Function("complex", List.of(IntType.INT, IntType.INT), IntType.INT, null);
    }
    
    // 辅助方法：创建main函数
    private static Function createMainFunction() {
        return new Function("main", List.of(), VoidType.VOID, null);
    }
    
    // 主方法用于测试
    public static void main(String[] args) {
        System.out.println("=== 简单IR示例 ===");
        IR simpleIR = createSampleIR();
        IRPrinter.print(simpleIR, System.out);
        
        System.out.println("\n=== 复杂IR示例 ===");
        IR complexIR = createComplexSampleIR();
        IRPrinter.print(complexIR, System.out);
        
        System.out.println("\n=== 包含函数调用的IR示例 ===");
        IR callIR = createCallSampleIR();
        IRPrinter.print(callIR, System.out);
    }
}