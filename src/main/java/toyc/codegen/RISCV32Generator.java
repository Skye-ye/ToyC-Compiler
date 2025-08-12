package toyc.codegen;

import toyc.codegen.regalloc.LinearScanAllocator;
import toyc.codegen.regalloc.LiveInterval;
import toyc.codegen.regalloc.LocalDataLocation;
import toyc.codegen.regalloc.RegisterAllocator;
import toyc.ir.IR;
import toyc.ir.exp.*;
import toyc.ir.stmt.*;
import toyc.language.Function;
import toyc.language.Program;

import java.util.HashSet;
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

        // 计算栈帧信息
        StackFrameInfo frameInfo = new StackFrameInfo(ir, allocator);

        builder.addGlobalFunc(function.getName());

        // 4. 生成prologue
        PrologueEpilogueGen.generatePrologue(builder, frameInfo);

        // Generate code for each statement using visitor pattern
        StmtCodeGenerator codeGen = new StmtCodeGenerator(builder, allocator);
        for (Stmt stmt : ir.getStmts()) {
            // 如果是return语句，需要在返回前生成epilogue
            if (stmt instanceof Return) {
                // 先处理return值的设置
                stmt.accept(codeGen);
                // 然后生成epilogue
                PrologueEpilogueGen.generateEpilogue(builder, frameInfo);
                // 最后添加返回指令
                builder.ret();
            } else {
                stmt.accept(codeGen);
            }
        }

        // 如果函数没有显式的return语句，在最后添加epilogue和return
        if (ir.getStmts().isEmpty() || !(ir.getStmts().get(ir.getStmts().size() - 1) instanceof Return)) {
            PrologueEpilogueGen.generateEpilogue(builder, frameInfo);
            builder.ret();
        }
        return builder.toString();
    }

    /**
     * Calculate simple live intervals for all variables in the IR.
     * This is a placeholder implementation - in a full compiler, you would
     * use proper live variable analysis.
     */
    private Set<LiveInterval> calculateSimpleLiveIntervals(IR ir) {
        Set<LiveInterval> intervals = new HashSet<>();

        // For simplicity, create an interval for each variable spanning the entire function
        for (Var var : ir.getVars()) {
            intervals.add(new LiveInterval(0, ir.getStmts().size() - 1, var.getName()));
        }

        return intervals;
    }

    /**
     * Statement visitor for generating RISC-V assembly code
     */
    private record StmtCodeGenerator(RISCV32AsmBuilder builder, RegisterAllocator allocator) implements StmtVisitor<Void> {

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

            // Load operands
            String src1 = loadOperand(binaryExp.getOperand1());
            String src2 = loadOperand(binaryExp.getOperand2());

            // Generate operation
            String riscvOp = getRISCVOp(binaryExp.getOperator());

            // Store result
            LocalDataLocation destLoc = allocator.allocate(lvalue.getName());
            if (destLoc.getType() == LocalDataLocation.LocationType.STACK) {
                builder.op3(riscvOp, "t0", src1, src2);
                builder.store("sw", "t0", destLoc.getOffset(), "sp");
            } else {
                builder.op3(riscvOp, destLoc.getRegister(), src1, src2);
            }

            return null;
        }

        @Override
        public Void visit(Call stmt) {
            CallExp callExp = stmt.getCallExp();
            Function callee = callExp.getFunction();

            // 1. 准备参数 - RISC-V调用约定: a0-a7传递前8个参数，其余通过栈传递
            int argCount = callExp.getArgCount();
            int stackArgsCount = Math.max(0, argCount - 8); // 超过8个参数需要用栈
            
            // 为栈参数预留空间
            if (stackArgsCount > 0) {
                builder.op3("addi", "sp", "sp", String.valueOf(-stackArgsCount * 4));
            }
            
            // 传递参数
            for (int i = 0; i < argCount; i++) {
                Var arg = callExp.getArg(i);
                String srcReg = loadOperand(arg);
                
                if (i < 8) {
                    // 前8个参数用寄存器 a0-a7 传递
                    String argReg = "a" + i;
                    if (!srcReg.equals(argReg)) {
                        builder.mv(argReg, srcReg);
                    }
                } else {
                    // 超过8个的参数通过栈传递
                    int stackOffset = (i - 8) * 4;
                    builder.store("sw", srcReg, stackOffset, "sp");
                }
            }

            // 2. 调用函数
            builder.call(callee.getName());

            // 3. 清理栈上的参数
            if (stackArgsCount > 0) {
                builder.op3("addi", "sp", "sp", String.valueOf(stackArgsCount * 4));
            }

            // 4. 处理返回值（如果有）
            if (stmt.getResult() != null) {
                // 返回值在 a0 寄存器中
                storeOperand(stmt.getResult(), "a0");
            }

            return null;
        }

        @Override
        public Void visit(Return stmt) {
            if (stmt.getValue() != null) {
                // 将返回值加载到 a0 寄存器
                String srcReg = loadOperand(stmt.getValue());
                if (!srcReg.equals("a0")) {
                    builder.mv("a0", srcReg);
                }
            }
            // 返回到调用者
            builder.ret();
            return null;
        }

        @Override
        public Void visit(Goto stmt) {
            // Handle goto statements for control flow
            // For now, just add a placeholder - full implementation would need label management
            return null;
        }

        @Override
        public Void visit(If stmt) {
            // Handle conditional jumps
            // For now, just add a placeholder - full implementation would need condition evaluation
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
        private String loadOperand(Var operand) {
            if (operand.isConst() && operand.getConstValue() instanceof IntLiteral intLit) {
                builder.li("t0", intLit.getValue());
                return "t0";
            }

            LocalDataLocation location = allocator.allocate(operand.getName());
            if (location.getType() == LocalDataLocation.LocationType.STACK) {
                builder.load("lw", "t0", location.getOffset(), "sp");
                return "t0";
            } else {
                return location.getRegister();
            }
        }

        /**
         * Store a register value into a variable location
         */
        private void storeOperand(Var operand, String srcReg) {
            LocalDataLocation location = allocator.allocate(operand.getName());
            if (location.getType() == LocalDataLocation.LocationType.STACK) {
                builder.store("sw", srcReg, location.getOffset(), "sp");
            } else {
                if (!srcReg.equals(location.getRegister())) {
                    builder.mv(location.getRegister(), srcReg);
                }
            }
        }

        /**
         * Map binary expression operators to RISC-V instructions.
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
