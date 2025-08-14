package toyc.codegen;

import toyc.World;
import toyc.algorithm.analysis.graph.callgraph.CallGraph;
import toyc.algorithm.analysis.graph.callgraph.CallGraphBuilder;
import toyc.codegen.regalloc.LinearScanAllocator;
import toyc.codegen.regalloc.LiveInterval;
import toyc.codegen.regalloc.LocalDataLocation;
import toyc.codegen.regalloc.RegisterAllocator;
import toyc.ir.IR;
import toyc.ir.exp.*;
import toyc.ir.stmt.*;
import toyc.language.Function;
import toyc.language.Program;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class RISCV32Generator implements AssemblyGenerator {
    public static final TargetArchitecture.Architecture ARCH =
            TargetArchitecture.Architecture.RISC_V_32;

    @Override
    public String generateProgramAssembly(Program program) {
        return generateProgramAssembly(program.allFunctions().toList());
    }

    @Override
    public String generateFunctionAssembly(Function function) {
        return generateFunctionAssembly(function.getIR());
    }

    /**
     * 为指定的函数列表生成程序汇编代码
     */
    public String generateProgramAssembly(List<Function> functions) {
        StringBuilder sb = new StringBuilder();
        
        // 添加汇编文件头部
        sb.append("# Generated RISC-V 32-bit assembly code\n");
        sb.append("# Target: RISC-V 32-bit\n\n");
        
        // 添加段声明
        sb.append(".text\n");
        sb.append(".align 2\n\n");
        
        // 生成所有函数的汇编代码
        for (Function function : functions) {
            sb.append(generateFunctionAssembly(function));
            sb.append("\n"); // 函数之间添加空行
        }
        
        return sb.toString();
    }

    @Override
    public String generateFunctionAssembly(IR ir) {
        // IR ir = function.getIR();
        Function function = ir.getFunction();  // 确保使用 IR 中的函数定义
        RISCV32AsmBuilder builder = new RISCV32AsmBuilder();

        // Create a simple register allocator - for now, use a basic set of intervals
        // TODO: Calculate proper live intervals using dataflow analysis
        Set<LiveInterval> intervals = calculateSimpleLiveIntervals(ir);
        RegisterAllocator allocator = new LinearScanAllocator(intervals);

        boolean hasCallSite = ir.getStmts().stream().anyMatch(stmt -> stmt instanceof Call);  // 假设 Call 是调用语句类
        // 获取栈大小和 callee-saved 寄存器
        int stackSize = allocator.getStackSize();  // 局部变量（spilled）空间，已对齐
        Set<String> calleeSavedSet = allocator.getUsedCalleeSavedRegisters();  // s0-s11
        // 使用 LinkedHashSet 保持保存顺序（callee-saved 先，ra 后）
        LinkedHashSet<String> savedRegs = new LinkedHashSet<>(calleeSavedSet);
        if (hasCallSite) {
            savedRegs.add("ra");  // 如果有子调用，添加 ra
        }
        // 计算保存区大小
        int saveSize = savedRegs.size() * 4;
        // 计算总栈帧大小，确保 16 字节对齐
        int totalStackSize = (stackSize + saveSize + 15) & ~15;

        String functionExitLabel = function.getName() + "_exit";

        builder.addGlobalFunc(function.getName());

        // --- Prologue ---
        if (totalStackSize > 0 || !savedRegs.isEmpty()) {
            builder.comment("Function prologue - allocate stack and save registers");
            if (totalStackSize > 0) {
                builder.addi("sp", "sp", String.valueOf(-totalStackSize));
            }
            int saveOffset = stackSize;  // 保存区从局部变量后开始
            int currentOffset = saveOffset;
            for (String reg : savedRegs) {
                builder.saveRegister(reg, currentOffset);
                currentOffset += 4;
            }
        }

        
        // --- Function Body ---
        // Generate code for each statement using visitor pattern
        StmtCodeGenerator codeGen = new StmtCodeGenerator(builder, allocator, functionExitLabel);
        codeGen.generateCode(ir.getStmts()); // 使用新的generateCode方法

        // --- Epilogue ---
        builder.label(functionExitLabel);
        if (totalStackSize > 0 || !savedRegs.isEmpty()) {
            builder.comment("Function epilogue - restore registers and deallocate stack");
            int saveOffset = stackSize;
            int currentOffset = saveOffset;
            for (String reg : savedRegs) {
                builder.restoreRegister(reg, currentOffset);
                currentOffset += 4;
            }
            if (totalStackSize > 0) {
                builder.addi("sp", "sp", String.valueOf(totalStackSize));
            }
        }
        builder.ret();

        return builder.toString();
    }

    /**
     * Calculate simple live intervals for all variables in the IR.
     * This is a placeholder implementation - in a full compiler, you would
     * use proper live variable analysis.
     */

    private Set<LiveInterval> calculateSimpleLiveIntervals(IR ir) {
        Map<String, Integer> firstUse = new HashMap<>();
        Map<String, Integer> lastUse = new HashMap<>();
        int n = ir.getStmts().size();
        for (int pos = 0; pos < n; pos++) {
            var stmt = ir.getStmts().get(pos);
            // 处理被使用的变量
            for (RValue r : stmt.getUses()) {
                if (r instanceof Var v) {
                    String var = v.getName();
                    firstUse.putIfAbsent(var, pos);
                    lastUse.put(var, pos);
                    // System.out.println("Use variable: " + var + " at position: " + pos);
                }
            }
            // 处理被定义的变量
            Optional<LValue> def = stmt.getDef();
            if (def.isPresent() && def.get() instanceof Var v) {
                String var = v.getName();
                firstUse.putIfAbsent(var, pos);
                lastUse.put(var, pos);
            }
        }
        Set<LiveInterval> intervals = new HashSet<>();
        for (String var : firstUse.keySet()) {
            intervals.add(new LiveInterval(firstUse.get(var), lastUse.get(var), var));
            // System.out.println("Live interval for variable: " + var + " from " + firstUse.get(var) + " to " + lastUse.get(var));
        }
        return intervals;
    }

    /**
     * Statement visitor for generating RISC-V assembly code
     */
    private static class StmtCodeGenerator implements StmtVisitor<Void> {
        private final RISCV32AsmBuilder builder;
        private final RegisterAllocator allocator;
        private final Map<Stmt, String> stmtLabels;
        private final String functionExitLabel;
        private int labelCounter = 0;

        public StmtCodeGenerator(RISCV32AsmBuilder builder, RegisterAllocator allocator, String functionExitLabel) {
            this.builder = builder;
            this.allocator = allocator;
            this.stmtLabels = new HashMap<>();
            this.functionExitLabel = functionExitLabel;
        }

        /**
         * 为语句生成或获取标签
         */
        private String getOrCreateLabel(Stmt stmt) {
            return stmtLabels.computeIfAbsent(stmt, s -> "L" + (labelCounter++));
        }

        /**
         * 预处理IR，为所有可能被跳转到的语句创建标签
         */
        public void preprocessLabels(List<Stmt> stmts) {
            for (Stmt stmt : stmts) {
                if (stmt instanceof If ifStmt) {
                    // If语句的目标需要标签
                    getOrCreateLabel(ifStmt.getTarget());
                } else if (stmt instanceof Goto gotoStmt) {
                    // Goto语句的目标需要标签
                    getOrCreateLabel(gotoStmt.getTarget());
                }
            }
        }

        /**
         * 生成所有语句的代码，同时处理标签
         */
        public void generateCode(List<Stmt> stmts) {
            // 首先预处理所有标签
            preprocessLabels(stmts);

            // 生成代码
            for (Stmt stmt : stmts) {
                // 如果这个语句有标签，先输出标签
                if (stmtLabels.containsKey(stmt)) {
                    builder.label(stmtLabels.get(stmt));
                }
                
                // 生成语句代码
                stmt.accept(this);
            }
            // 如果函数末尾没有return语句，添加一个跳转到退出点
            // 这确保所有执行路径都通过统一的退出点
            if (!stmts.isEmpty() && !(stmts.get(stmts.size() - 1) instanceof Return)) {
                builder.j(functionExitLabel);
    }
        }
        

        @Override
        public Void visit(AssignLiteral stmt) {
            Var lvalue = stmt.getLValue();
            Literal literal = stmt.getRValue();

            if (literal instanceof IntLiteral intLit) {
                LocalDataLocation location = allocator.allocate(lvalue.getName());
                if (location.getType() == LocalDataLocation.LocationType.STACK) {
                    builder.li("t0", intLit.getValue());
                    builder.store("sw", "t0", location.getOffset(), "sp");
                } else {
                    builder.li(location.getRegister(), intLit.getValue());
                }
            }
            return null;
        }


        @Override
        public Void visit(Copy stmt) {
            Var lvalue = stmt.getLValue();
            Var rvalue = stmt.getRValue();

            LocalDataLocation srcLoc = allocator.allocate(rvalue.getName());
            LocalDataLocation destLoc = allocator.allocate(lvalue.getName());

            if (srcLoc.getType() == LocalDataLocation.LocationType.STACK) {
                if (destLoc.getType() == LocalDataLocation.LocationType.STACK) {
                    builder.load("lw", "t0", srcLoc.getOffset(), "sp");
                    builder.store("sw", "t0", destLoc.getOffset(), "sp");
                } else {
                    builder.load("lw", destLoc.getRegister(), srcLoc.getOffset(), "sp");
                }
            } else {
                if (destLoc.getType() == LocalDataLocation.LocationType.STACK) {
                    builder.store("sw", srcLoc.getRegister(), destLoc.getOffset(), "sp");
                } else {
                    builder.mv(destLoc.getRegister(), srcLoc.getRegister());
                }
            }
            return null;
        }

        @Override
        public Void visit(Binary stmt) {
            Var lvalue = stmt.getLValue();
            BinaryExp binaryExp = stmt.getRValue();

            String src1 = loadOperand(binaryExp.getOperand1());
            String src2 = loadOperand(binaryExp.getOperand2());
            LocalDataLocation destLoc = allocator.allocate(lvalue.getName());
            
            String destReg = (destLoc.getType() == LocalDataLocation.LocationType.STACK) ? "t0" : destLoc.getRegister();

            if (binaryExp.getOperator() instanceof ArithmeticExp.Op arithOp) {
                String riscvOp = switch (arithOp) {
                    case ADD -> "add";
                    case SUB -> "sub";
                    case MUL -> "mul";
                    case DIV -> "div";
                    case REM -> "rem";
                };
                builder.op3(riscvOp, destReg, src1, src2);
            } else if (binaryExp.getOperator() instanceof ConditionExp.Op condOp) {
                switch (condOp) {
                    case EQ -> {
                        builder.op3("sub", destReg, src1, src2);
                        builder.sltiu(destReg, destReg, 1); // 如果相等，sub结果为0，sltiu设置为1
                    }
                    case NE -> {
                        builder.op3("sub", destReg, src1, src2);
                        builder.sltu(destReg, "zero", destReg); // 如果不等，sub结果非0，设置为1
                    }
                    case LT -> builder.slt(destReg, src1, src2);
                    case LE -> {
                        builder.slt(destReg, src2, src1); // src2 < src1
                        builder.xori(destReg, destReg, 1); // 取反得到 src1 <= src2
                    }
                    case GT -> builder.slt(destReg, src2, src1); // 交换操作数
                    case GE -> {
                        builder.slt(destReg, src1, src2); // src1 < src2
                        builder.xori(destReg, destReg, 1); // 取反得到 src1 >= src2
                    }
                }
            } else {
                throw new UnsupportedOperationException("Unsupported binary operation: " + binaryExp.getOperator());
            }

            if (destLoc.getType() == LocalDataLocation.LocationType.STACK) {
                builder.store("sw", "t0", destLoc.getOffset(), "sp");
            }

            return null;
        }

        @Override
        public Void visit(Call stmt) {
            CallExp callExp = stmt.getCallExp();
            Function callee = callExp.getFunction();

            // 计算需要通过栈传递的参数数量和空间
            int argCount = callExp.getArgCount();
            int stackArgCount = Math.max(0, argCount - 8); // 超过8个的参数需要通过栈传递
            int stackArgSize = stackArgCount * 4; // 每个参数4字节

            // 为栈参数分配空间（从高地址向低地址分配）
            if (stackArgSize > 0) {
                builder.addi("sp", "sp", String.valueOf(-stackArgSize));
            }

            // Calling Convention:
            // Load arguments into argument registers (a0, a1, ...)
            for (int i = 0; i < callExp.getArgCount(); i++) {
                Var arg = callExp.getArg(i);
                String srcReg = loadOperand(arg);

                if(i < 8) {
                    String argReg = "a" + i;
                    if (!srcReg.equals(argReg)) {
                        builder.mv(argReg, srcReg);
                    }
                }
                else {
                    // 超过8个的参数：压入栈中
                    // 栈参数从低偏移开始存放：第9个参数在sp+0，第10个在sp+4，依此类推
                    int stackOffset = (i - 8) * 4;
                    builder.store("sw", srcReg, stackOffset, "sp");
                }
            }

            builder.call(callee.getName());

            if (stackArgSize > 0) {
                builder.addi("sp", "sp", String.valueOf(stackArgSize));
            }

            // Store return value if needed
            if (stmt.getResult() != null) {
                LocalDataLocation resultLoc = allocator.allocate(stmt.getResult().getName());
                if (resultLoc.getType() == LocalDataLocation.LocationType.STACK) {
                    builder.store("sw", "a0", resultLoc.getOffset(), "sp");
                } else {
                    builder.mv(resultLoc.getRegister(), "a0");
                }
            }

            return null;
        }

        @Override
        public Void visit(Return stmt) {
            if (stmt.getValue() != null) {
                String srcReg = loadOperand(stmt.getValue());
                if (!srcReg.equals("a0")) {
                    builder.mv("a0", srcReg);
                }
            }
            builder.j(functionExitLabel); // Jump to function exit label
            return null;
        }

        @Override
        public Void visit(Goto stmt) {
            // Handle goto statements for control flow
            // For now, just add a placeholder - full implementation would need label management
            String targetLabel = getOrCreateLabel(stmt.getTarget());
            builder.j(targetLabel);
            return null;
        }

        @Override
        public Void visit(If stmt) {
            // Handle conditional jumps based on condition expression type
            String targetLabel = getOrCreateLabel(stmt.getTarget());
            RValue condition = stmt.getCondition();
            
            if (condition instanceof BinaryExp binaryExp && 
                binaryExp.getOperator() instanceof ConditionExp.Op condOp) {
                
                // 对于比较操作，直接生成对应的分支指令
                String src1 = loadOperand(binaryExp.getOperand1());
                String src2 = loadOperand(binaryExp.getOperand2());
                
                switch (condOp) {
                    case EQ -> builder.beq(src1, src2, targetLabel);
                    case NE -> builder.bne(src1, src2, targetLabel);
                    case LT -> builder.blt(src1, src2, targetLabel);
                    case GE -> builder.bge(src1, src2, targetLabel);
                    case GT -> builder.blt(src2, src1, targetLabel); // 交换操作数：src2 < src1 等价于 src1 > src2
                    case LE -> builder.bge(src2, src1, targetLabel); // 交换操作数：src2 >= src1 等价于 src1 <= src2
                }
            } else {
                // 对于其他类型的条件（变量或复杂表达式的结果），使用 bnez
                String condReg = loadOperand(condition);
                builder.bnez(condReg, targetLabel);
            }
            return null;
        }

        @Override
        public Void visit(Unary stmt) {
            Var lvalue = stmt.getLValue();
            UnaryExp unaryExp = stmt.getRValue();

            // Load the operand
            String srcReg = loadOperand(unaryExp.getOperand());

            // Generate unary operation (currently only negation is supported)
            if (unaryExp instanceof NegExp) {
                LocalDataLocation destLoc = allocator.allocate(lvalue.getName());
                if (destLoc.getType() == LocalDataLocation.LocationType.STACK) {
                    builder.op3("sub", "t0", "zero", srcReg);  // t0 = 0 - src (negation)
                    builder.store("sw", "t0", destLoc.getOffset(), "sp");
                } else {
                    builder.op3("sub", destLoc.getRegister(), "zero", srcReg);  // dest = 0 - src
                }
            }

            return null;
        }

        @Override
        public Void visit(Nop stmt) {
            // No operation for now
            return null;
        }

        /**
         * Load a variable operand into a register and return the register name.
         * Uses temporary registers t0, t1 as needed.
         */
        private String loadOperand(RValue operand) {
            if (operand instanceof Var var) {
                if (var.isConst() && var.getConstValue() instanceof IntLiteral intLit) {
                    builder.li("t0", intLit.getValue());
                    return "t0";
                }

                LocalDataLocation location = allocator.allocate(var.getName());
                if (location == null) {
                    throw new IllegalStateException("No location allocated for variable: " + var.getName());
                }
                if (location.getType() == LocalDataLocation.LocationType.STACK) {
                    builder.load("lw", "t0", location.getOffset(), "sp");
                    return "t0";
                } else {
                    return location.getRegister();
                }
            } else if (operand instanceof IntLiteral intLit) {
                builder.li("t1", intLit.getValue());
                return "t1";
            } else {
                throw new UnsupportedOperationException("Unsupported operand type: " + operand.getClass());
            }
        }

        /**
         * Map binary expression operators to RISC-V instructions.
         * This function seems useless now!
         */
        // private String getRISCVOp(BinaryExp.Op op) {
        //     if (op instanceof ArithmeticExp.Op arithOp) {
        //         return switch (arithOp) {
        //             case ADD -> "add";
        //             case SUB -> "sub";
        //             case MUL -> "mul";
        //             case DIV -> "div";
        //             case REM -> "rem";
        //         };
        //     } else if (op instanceof ConditionExp.Op condOp) {
        //         return switch (condOp) {
        //             case EQ -> "seq";   // Set if equal (pseudo-instruction)
        //             case NE -> "sne";   // Set if not equal (pseudo-instruction)
        //             case LT -> "slt";   // Set if less than
        //             case LE ->
        //                     "sle";   // Set if less than or equal (pseudo-instruction)
        //             case GT ->
        //                     "sgt";   // Set if greater than (pseudo-instruction)
        //             case GE ->
        //                     "sge";   // Set if greater than or equal (pseudo-instruction)
        //         };
        //     } else if (op instanceof ComparisonExp.Op compOp) {
        //         return switch (compOp) {
        //             case CMP -> "cmp";      // Generic compare
        //             case CMPL -> "cmpl";    // Compare with less bias
        //             case CMPG -> "cmpg";    // Compare with greater bias
        //         };
        //     }

        //     throw new IllegalArgumentException("Unsupported binary operation: " + op);
        // }
    }
}
