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
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String generateFunctionAssembly(Function function) {
        IR ir = function.getIR();
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
        StmtCodeGenerator codeGen = new StmtCodeGenerator(builder, allocator);
        codeGen.generateCode(ir.getStmts()); // 使用新的generateCode方法

        // --- Epilogue ---
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
        private int labelCounter = 0;

        public StmtCodeGenerator(RISCV32AsmBuilder builder, RegisterAllocator allocator) {
            this.builder = builder;
            this.allocator = allocator;
            this.stmtLabels = new HashMap<>();
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

            // Calling Convention:
            // Load arguments into argument registers (a0, a1, ...)
            for (int i = 0; i < callExp.getArgCount(); i++) {
                Var arg = callExp.getArg(i);
                String argReg = "a" + i;
                String srcReg = loadOperand(arg);
                if (!srcReg.equals(argReg)) {
                    builder.mv(argReg, srcReg);
                }
            }

            builder.call(callee.getName());

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
            // Handle conditional jumps
            // For now, just add a placeholder - full implementation would need condition evaluation
            
            String condReg = loadOperand(stmt.getCondition());
            String targetLabel = getOrCreateLabel(stmt.getTarget());
            
            // 如果条件为真，跳转到目标标签
            builder.bnez(condReg, targetLabel);
            
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
        private String getRISCVOp(BinaryExp.Op op) {
            if (op instanceof ArithmeticExp.Op arithOp) {
                return switch (arithOp) {
                    case ADD -> "add";
                    case SUB -> "sub";
                    case MUL -> "mul";
                    case DIV -> "div";
                    case REM -> "rem";
                };
            } else if (op instanceof ConditionExp.Op condOp) {
                return switch (condOp) {
                    case EQ -> "seq";   // Set if equal (pseudo-instruction)
                    case NE -> "sne";   // Set if not equal (pseudo-instruction)
                    case LT -> "slt";   // Set if less than
                    case LE ->
                            "sle";   // Set if less than or equal (pseudo-instruction)
                    case GT ->
                            "sgt";   // Set if greater than (pseudo-instruction)
                    case GE ->
                            "sge";   // Set if greater than or equal (pseudo-instruction)
                };
            } else if (op instanceof ComparisonExp.Op compOp) {
                return switch (compOp) {
                    case CMP -> "cmp";      // Generic compare
                    case CMPL -> "cmpl";    // Compare with less bias
                    case CMPG -> "cmpg";    // Compare with greater bias
                };
            }

            throw new IllegalArgumentException("Unsupported binary operation: " + op);
        }
    }
}
