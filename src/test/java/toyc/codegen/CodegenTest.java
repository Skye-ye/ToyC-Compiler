package toyc.codegen;

import org.junit.jupiter.api.Test;
import toyc.codegen.regalloc.LinearScanAllocator;
import toyc.codegen.regalloc.StackOnlyAllocator;
import toyc.ir.DefaultIR;
import toyc.ir.IR;
import toyc.ir.exp.*;
import toyc.ir.stmt.*;
import toyc.language.Function;
import toyc.language.type.IntType;
import toyc.language.type.VoidType;

import java.util.*;

/**
 * Test cases for the code generation framework.
 * Tests various language constructs with manually constructed IR.
 */
public class CodegenTest {

    /**
     * Test simple arithmetic operations: a = 10; b = 20; c = a + b;
     */
    @Test
    public void testSimpleArithmetic() {
        System.out.println("=== Test: Simple Arithmetic ===");
        
        // Create a test function
        Function function = new Function("testArithmetic", List.of(), IntType.INT, null);
        
        // Create variables: a, b, c
        Var a = new Var(function, "a", IntType.INT, 0);
        Var b = new Var(function, "b", IntType.INT, 1);
        Var c = new Var(function, "c", IntType.INT, 2);
        
        // Create statements
        List<Stmt> stmts = new ArrayList<>();
        
        // a = 10
        stmts.add(new AssignLiteral(a, IntLiteral.get(10)));
        stmts.get(0).setIndex(0);
        
        // b = 20
        stmts.add(new AssignLiteral(b, IntLiteral.get(20)));
        stmts.get(1).setIndex(1);
        
        // c = a + b
        ArithmeticExp addExp = new ArithmeticExp(ArithmeticExp.Op.ADD, a, b);
        stmts.add(new Binary(c, addExp));
        stmts.get(2).setIndex(2);
        
        // return c
        stmts.add(new Return(c));
        stmts.get(3).setIndex(3);
        
        // Create IR
        IR ir = new DefaultIR(function, List.of(), Set.of(c), List.of(a, b, c), stmts);
        
        // Test with RISCV32 generator
        RISCV32Generator generator = new RISCV32Generator();
        String assembly = generator.generateFunctionAssembly(function);
        
        System.out.println("Generated RISC-V assembly:");
        System.out.println(assembly);
        System.out.println();
    }

    /**
     * Test function call: result = add(5, 3);
     */
    @Test
    public void testFunctionCall() {
        System.out.println("=== Test: Function Call ===");
        
        // Create main function
        Function mainFunc = new Function("main", List.of(), IntType.INT, null);
        
        // Create add function (parameters and signature)
        Function addFunc = new Function("add", 
            List.of(IntType.INT, IntType.INT), IntType.INT, List.of("x", "y"));
        
        // Create variables
        Var result = new Var(mainFunc, "result", IntType.INT, 0);
        Var arg1 = new Var(mainFunc, "arg1", IntType.INT, 1);
        Var arg2 = new Var(mainFunc, "arg2", IntType.INT, 2);
        
        // Create statements
        List<Stmt> stmts = new ArrayList<>();
        
        // arg1 = 5
        stmts.add(new AssignLiteral(arg1, IntLiteral.get(5)));
        stmts.get(0).setIndex(0);
        
        // arg2 = 3  
        stmts.add(new AssignLiteral(arg2, IntLiteral.get(3)));
        stmts.get(1).setIndex(1);
        
        // result = add(arg1, arg2)
        CallExp callExp = new CallExp(addFunc, List.of(arg1, arg2));
        stmts.add(new Call(mainFunc, callExp, result));
        stmts.get(2).setIndex(2);
        
        // return result
        stmts.add(new Return(result));
        stmts.get(3).setIndex(3);
        
        // Create IR
        IR ir = new DefaultIR(mainFunc, List.of(), Set.of(result), 
                             List.of(result, arg1, arg2), stmts);
        
        // Test with RISCV32 generator
        RISCV32Generator generator = new RISCV32Generator();
        String assembly = generator.generateFunctionAssembly(mainFunc);
        
        System.out.println("Generated RISC-V assembly:");
        System.out.println(assembly);
        System.out.println();
    }

    /**
     * Test complex expressions: result = (a + b) * (c - d);
     */
    @Test
    public void testComplexExpressions() {
        System.out.println("=== Test: Complex Expressions ===");
        
        Function function = new Function("complexExpr", List.of(), IntType.INT, null);
        
        // Create variables
        Var a = new Var(function, "a", IntType.INT, 0);
        Var b = new Var(function, "b", IntType.INT, 1);
        Var c = new Var(function, "c", IntType.INT, 2);
        Var d = new Var(function, "d", IntType.INT, 3);
        Var temp1 = new Var(function, "temp1", IntType.INT, 4);
        Var temp2 = new Var(function, "temp2", IntType.INT, 5);
        Var result = new Var(function, "result", IntType.INT, 6);
        
        List<Stmt> stmts = new ArrayList<>();
        
        // a = 10, b = 5, c = 8, d = 3
        stmts.add(new AssignLiteral(a, IntLiteral.get(10)));
        stmts.add(new AssignLiteral(b, IntLiteral.get(5)));
        stmts.add(new AssignLiteral(c, IntLiteral.get(8)));
        stmts.add(new AssignLiteral(d, IntLiteral.get(3)));
        
        // temp1 = a + b
        ArithmeticExp addExp = new ArithmeticExp(ArithmeticExp.Op.ADD, a, b);
        stmts.add(new Binary(temp1, addExp));
        
        // temp2 = c - d  
        ArithmeticExp subExp = new ArithmeticExp(ArithmeticExp.Op.SUB, c, d);
        stmts.add(new Binary(temp2, subExp));
        
        // result = temp1 * temp2
        ArithmeticExp mulExp = new ArithmeticExp(ArithmeticExp.Op.MUL, temp1, temp2);
        stmts.add(new Binary(result, mulExp));
        
        // return result
        stmts.add(new Return(result));
        
        // Set indices
        for (int i = 0; i < stmts.size(); i++) {
            stmts.get(i).setIndex(i);
        }
        
        // Create IR
        IR ir = new DefaultIR(function, List.of(), Set.of(result),
                             List.of(a, b, c, d, temp1, temp2, result), stmts);
        
        // Test with RISCV32 generator
        RISCV32Generator generator = new RISCV32Generator();
        String assembly = generator.generateFunctionAssembly(function);
        
        System.out.println("Generated RISC-V assembly:");
        System.out.println(assembly);
        System.out.println();
    }

    /**
     * Test variable copying: a = 5; b = a; c = b;
     */
    @Test
    public void testVariableCopy() {
        System.out.println("=== Test: Variable Copy ===");
        
        Function function = new Function("testCopy", List.of(), IntType.INT, null);
        
        // Create variables
        Var a = new Var(function, "a", IntType.INT, 0);
        Var b = new Var(function, "b", IntType.INT, 1);
        Var c = new Var(function, "c", IntType.INT, 2);
        
        List<Stmt> stmts = new ArrayList<>();
        
        // a = 5
        stmts.add(new AssignLiteral(a, IntLiteral.get(5)));
        stmts.get(0).setIndex(0);
        
        // b = a
        stmts.add(new Copy(b, a));
        stmts.get(1).setIndex(1);
        
        // c = b
        stmts.add(new Copy(c, b));
        stmts.get(2).setIndex(2);
        
        // return c
        stmts.add(new Return(c));
        stmts.get(3).setIndex(3);
        
        // Create IR
        IR ir = new DefaultIR(function, List.of(), Set.of(c), List.of(a, b, c), stmts);
        
        // Test with RISCV32 generator
        RISCV32Generator generator = new RISCV32Generator();
        String assembly = generator.generateFunctionAssembly(function);
        
        System.out.println("Generated RISC-V assembly:");
        System.out.println(assembly);
        System.out.println();
    }

    /**
     * Test unary expressions: a = 10; b = -a;
     */
    @Test
    public void testUnaryExpressions() {
        System.out.println("=== Test: Unary Expressions ===");
        
        Function function = new Function("testUnary", List.of(), IntType.INT, null);
        
        // Create variables
        Var a = new Var(function, "a", IntType.INT, 0);
        Var b = new Var(function, "b", IntType.INT, 1);
        
        List<Stmt> stmts = new ArrayList<>();
        
        // a = 10
        stmts.add(new AssignLiteral(a, IntLiteral.get(10)));
        stmts.get(0).setIndex(0);
        
        // b = -a
        NegExp negExp = new NegExp(a);
        stmts.add(new Unary(b, negExp));
        stmts.get(1).setIndex(1);
        
        // return b
        stmts.add(new Return(b));
        stmts.get(2).setIndex(2);
        
        // Create IR
        IR ir = new DefaultIR(function, List.of(), Set.of(b), List.of(a, b), stmts);
        
        // Test with RISCV32 generator
        RISCV32Generator generator = new RISCV32Generator();
        String assembly = generator.generateFunctionAssembly(function);
        
        System.out.println("Generated RISC-V assembly:");
        System.out.println(assembly);
        System.out.println();
    }

    /**
     * Test function with parameters: int multiply(int x, int y) { return x * y; }
     */
    @Test
    public void testFunctionWithParameters() {
        System.out.println("=== Test: Function with Parameters ===");
        
        Function function = new Function("multiply", 
            List.of(IntType.INT, IntType.INT), IntType.INT, List.of("x", "y"));
        
        // Create parameter variables
        Var x = new Var(function, "x", IntType.INT, 0);
        Var y = new Var(function, "y", IntType.INT, 1);
        Var result = new Var(function, "result", IntType.INT, 2);
        
        List<Stmt> stmts = new ArrayList<>();
        
        // result = x * y
        ArithmeticExp mulExp = new ArithmeticExp(ArithmeticExp.Op.MUL, x, y);
        stmts.add(new Binary(result, mulExp));
        stmts.get(0).setIndex(0);
        
        // return result
        stmts.add(new Return(result));
        stmts.get(1).setIndex(1);
        
        // Create IR
        IR ir = new DefaultIR(function, List.of(x, y), Set.of(result), 
                             List.of(x, y, result), stmts);
        
        // Test with RISCV32 generator
        RISCV32Generator generator = new RISCV32Generator();
        String assembly = generator.generateFunctionAssembly(function);
        
        System.out.println("Generated RISC-V assembly:");
        System.out.println(assembly);
        System.out.println();
    }

    /**
     * Test void function: void printValue(int x) { return; }
     */
    @Test
    public void testVoidFunction() {
        System.out.println("=== Test: Void Function ===");
        
        Function function = new Function("printValue", 
            List.of(IntType.INT), VoidType.VOID, List.of("x"));
        
        // Create parameter variable
        Var x = new Var(function, "x", IntType.INT, 0);
        
        List<Stmt> stmts = new ArrayList<>();
        
        // Just return (void function)
        stmts.add(new Return(null));
        stmts.get(0).setIndex(0);
        
        // Create IR
        IR ir = new DefaultIR(function, List.of(x), Set.of(), List.of(x), stmts);
        
        // Test with RISCV32 generator
        RISCV32Generator generator = new RISCV32Generator();
        String assembly = generator.generateFunctionAssembly(function);
        
        System.out.println("Generated RISC-V assembly:");
        System.out.println(assembly);
        System.out.println();
    }

    /**
     * Test all arithmetic operations
     */
    @Test 
    public void testAllArithmeticOps() {
        System.out.println("=== Test: All Arithmetic Operations ===");
        
        Function function = new Function("testAllOps", List.of(), IntType.INT, null);
        
        // Create variables for operands and results
        Var a = new Var(function, "a", IntType.INT, 0);
        Var b = new Var(function, "b", IntType.INT, 1);
        Var sum = new Var(function, "sum", IntType.INT, 2);
        Var diff = new Var(function, "diff", IntType.INT, 3);  
        Var prod = new Var(function, "prod", IntType.INT, 4);
        Var quot = new Var(function, "quot", IntType.INT, 5);
        Var rem = new Var(function, "rem", IntType.INT, 6);
        
        List<Stmt> stmts = new ArrayList<>();
        
        // a = 20, b = 3
        stmts.add(new AssignLiteral(a, IntLiteral.get(20)));
        stmts.add(new AssignLiteral(b, IntLiteral.get(3)));
        
        // Test all arithmetic operations
        stmts.add(new Binary(sum, new ArithmeticExp(ArithmeticExp.Op.ADD, a, b))); // 23
        stmts.add(new Binary(diff, new ArithmeticExp(ArithmeticExp.Op.SUB, a, b))); // 17
        stmts.add(new Binary(prod, new ArithmeticExp(ArithmeticExp.Op.MUL, a, b))); // 60
        stmts.add(new Binary(quot, new ArithmeticExp(ArithmeticExp.Op.DIV, a, b))); // 6
        stmts.add(new Binary(rem, new ArithmeticExp(ArithmeticExp.Op.REM, a, b))); // 2
        
        // return sum (or any result)
        stmts.add(new Return(sum));
        
        // Set indices
        for (int i = 0; i < stmts.size(); i++) {
            stmts.get(i).setIndex(i);
        }
        
        // Create IR
        IR ir = new DefaultIR(function, List.of(), Set.of(sum),
                             List.of(a, b, sum, diff, prod, quot, rem), stmts);
        
        // Test with RISCV32 generator
        RISCV32Generator generator = new RISCV32Generator();
        String assembly = generator.generateFunctionAssembly(function);
        
        System.out.println("Generated RISC-V assembly:");
        System.out.println(assembly);
        System.out.println();
    }

    /**
     * Test stack operations and local variable management
     */
    @Test
    public void testStackOperations() {
        System.out.println("=== Test: Stack Operations ===");
        
        Function function = new Function("testStack", List.of(), IntType.INT, null);
        
        // Create many local variables to force stack usage
        List<Var> vars = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            vars.add(new Var(function, "var" + i, IntType.INT, i));
        }
        
        List<Stmt> stmts = new ArrayList<>();
        
        // Initialize all variables with different values
        for (int i = 0; i < vars.size(); i++) {
            stmts.add(new AssignLiteral(vars.get(i), IntLiteral.get(i + 1)));
        }
        
        // Create a chain of operations: var0 = var1 + var2; var3 = var4 + var5; etc.
        Var result1 = new Var(function, "result1", IntType.INT, vars.size());
        Var result2 = new Var(function, "result2", IntType.INT, vars.size() + 1);
        Var finalResult = new Var(function, "finalResult", IntType.INT, vars.size() + 2);
        
        stmts.add(new Binary(result1, new ArithmeticExp(ArithmeticExp.Op.ADD, vars.get(0), vars.get(1))));
        stmts.add(new Binary(result2, new ArithmeticExp(ArithmeticExp.Op.ADD, vars.get(2), vars.get(3))));
        stmts.add(new Binary(finalResult, new ArithmeticExp(ArithmeticExp.Op.MUL, result1, result2)));
        
        stmts.add(new Return(finalResult));
        
        // Set indices
        for (int i = 0; i < stmts.size(); i++) {
            stmts.get(i).setIndex(i);
        }
        
        List<Var> allVars = new ArrayList<>(vars);
        allVars.addAll(List.of(result1, result2, finalResult));
        
        // Create IR
        IR ir = new DefaultIR(function, List.of(), Set.of(finalResult), allVars, stmts);
        
        // Test with stack-only allocation (should use stack for all variables)
        RISCV32Generator generator = new RISCV32Generator();
        String assembly = generator.generateFunctionAssembly(function);
        
        System.out.println("Generated RISC-V assembly (many local variables):");
        System.out.println(assembly);
        System.out.println();
    }

    /**
     * Test edge cases in code generation
     */
    @Test
    public void testEdgeCases() {
        System.out.println("=== Test: Edge Cases ===");
        
        // Test 1: Function with no statements (just return)
        System.out.println("--- Empty function ---");
        Function emptyFunc = new Function("empty", List.of(), IntType.INT, null);
        Var retVar = new Var(emptyFunc, "ret", IntType.INT, 0);
        List<Stmt> emptyStmts = List.of(
            new AssignLiteral(retVar, IntLiteral.get(42)),
            new Return(retVar)
        );
        emptyStmts.get(0).setIndex(0);
        emptyStmts.get(1).setIndex(1);
        
        IR emptyIR = new DefaultIR(emptyFunc, List.of(), Set.of(), List.of(), emptyStmts);
        
        RISCV32Generator generator = new RISCV32Generator();
        String emptyAssembly = generator.generateFunctionAssembly(emptyFunc);
        System.out.println(emptyAssembly);
        
        // Test 2: Function with zero values
        System.out.println("--- Zero values ---");
        Function zeroFunc = new Function("testZero", List.of(), IntType.INT, null);
        Var zero = new Var(zeroFunc, "zero", IntType.INT, 0);
        
        List<Stmt> zeroStmts = new ArrayList<>();
        zeroStmts.add(new AssignLiteral(zero, IntLiteral.get(0)));
        zeroStmts.add(new Return(zero));
        
        for (int i = 0; i < zeroStmts.size(); i++) {
            zeroStmts.get(i).setIndex(i);
        }
        
        IR zeroIR = new DefaultIR(zeroFunc, List.of(), Set.of(zero), List.of(zero), zeroStmts);
        String zeroAssembly = generator.generateFunctionAssembly(zeroFunc);
        System.out.println(zeroAssembly);
        
        // Test 3: Large immediate values
        System.out.println("--- Large immediate values ---");
        Function largeFunc = new Function("testLarge", List.of(), IntType.INT, null);
        Var large = new Var(largeFunc, "large", IntType.INT, 0);
        
        List<Stmt> largeStmts = new ArrayList<>();
        largeStmts.add(new AssignLiteral(large, IntLiteral.get(0x7FFFFFFF))); // Max int
        largeStmts.add(new Return(large));
        
        for (int i = 0; i < largeStmts.size(); i++) {
            largeStmts.get(i).setIndex(i);
        }
        
        IR largeIR = new DefaultIR(largeFunc, List.of(), Set.of(large), List.of(large), largeStmts);
        String largeAssembly = generator.generateFunctionAssembly(largeFunc);
        System.out.println(largeAssembly);
        
        System.out.println();
    }

    /**
     * Test the target architecture framework (demonstrating extensibility)
     */
    @Test
    public void testArchitectureFramework() {
        System.out.println("=== Test: Architecture Framework ===");
        
        // Test the existing RISCV32Generator
        RISCV32Generator riscvGen = new RISCV32Generator();
        System.out.println("RISC-V 32-bit Generator Architecture: " + riscvGen.getArchitecture());
        System.out.println("Is 32-bit: " + riscvGen.getArchitecture().is32Bit());
        System.out.println("Is Little Endian: " + riscvGen.getArchitecture().isLittleEndian());
        System.out.println();
        
        // Create a simple test function to demonstrate architecture info
        Function archFunc = new Function("archTest", List.of(), IntType.INT, null);
        Var result = new Var(archFunc, "result", IntType.INT, 0);
        
        List<Stmt> stmts = new ArrayList<>();
        stmts.add(new AssignLiteral(result, IntLiteral.get(123)));
        stmts.add(new Return(result));
        
        for (int i = 0; i < stmts.size(); i++) {
            stmts.get(i).setIndex(i);
        }
        
        IR archIR = new DefaultIR(archFunc, List.of(), Set.of(result), List.of(result), stmts);
        
        System.out.println("Sample code generation for architecture framework:");
        String assembly = riscvGen.generateFunctionAssembly(archFunc);
        System.out.println(assembly);
    }

    /**
     * Demonstration of how the framework could be extended for other architectures
     * (This is a mock implementation for demonstration purposes)
     */
    public static class MockX86Generator implements AssemblyGenerator {
        public static final TargetArchitecture.Architecture ARCH = 
                TargetArchitecture.Architecture.X86_32;
                
        @Override
        public String generateProgramAssembly(toyc.language.Program program) {
            throw new UnsupportedOperationException("Program assembly not implemented yet");
        }
        
        @Override
        public String generateFunctionAssembly(toyc.language.Function function) {
            // Mock implementation - would normally generate x86 assembly
            StringBuilder sb = new StringBuilder();
            sb.append("; Mock x86-32 assembly for function: ").append(function.getName()).append("\n");
            sb.append("section .text\n");
            sb.append("global ").append(function.getName()).append("\n");
            sb.append(function.getName()).append(":\n");
            sb.append("    ; Mock x86 instructions would go here\n");
            sb.append("    push ebp\n");
            sb.append("    mov ebp, esp\n");
            sb.append("    ; ... function body ...\n");
            sb.append("    mov esp, ebp\n");
            sb.append("    pop ebp\n");
            sb.append("    ret\n");
            return sb.toString();
        }
    }

    /**
     * Test the extensibility of the architecture framework with a mock generator
     */
    // @Test
    // public void testArchitectureExtensibility() {
    //     System.out.println("=== Test: Architecture Extensibility ===");
        
    //     // Demonstrate how easy it is to add new architectures
    //     MockX86Generator x86Gen = new MockX86Generator();
    //     System.out.println("Mock X86 Generator Architecture: " + x86Gen.getArchitecture());
    //     System.out.println("Is 32-bit: " + x86Gen.getArchitecture().is32Bit());
    //     System.out.println("Is Little Endian: " + x86Gen.getArchitecture().isLittleEndian());
        
    //     // Create a simple function to test with
    //     Function testFunc = new Function("mockTest", List.of(), IntType.INT, null);
    //     String mockAssembly = x86Gen.generateFunctionAssembly(testFunc);
        
    //     System.out.println("\nGenerated Mock x86 assembly:");
    //     System.out.println(mockAssembly);
    // }

    /**
     * Run all tests to demonstrate different code generation scenarios
     */
    @Test
    public void runAllCodegenTests() {
        testSimpleArithmetic();
        testFunctionCall();
        testComplexExpressions(); 
        testVariableCopy();
        testUnaryExpressions();
        testFunctionWithParameters();
        testVoidFunction();
        testAllArithmeticOps();
        testStackOperations();
        testEdgeCases();
        testArchitectureFramework();
        // testArchitectureExtensibility();
    }
}
