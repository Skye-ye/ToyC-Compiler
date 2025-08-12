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
import java.util.LinkedHashSet;
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

        // 获取使用的 callee-saved 寄存器
        Set<String> usedCalleeSavedRegs = new LinkedHashSet<>(allocator.getUsedCalleeSavedRegisters());
        if (hasFunctionCall(ir)) {
            usedCalleeSavedRegs.add("ra");
        }

        int localVarStackSize = allocator.getStackSize();
        int savedRegistersSize = usedCalleeSavedRegs.size() * 4;
        int totalStackSize = (localVarStackSize + savedRegistersSize + 15) & ~15;

        builder.addGlobalFunc(function.getName());

        // 生成 prologue - 保存寄存器在栈顶
        if (totalStackSize > 0) {
            builder.comment("Function prologue");
            builder.addi("sp", "sp", String.valueOf(-totalStackSize));

            // 保存在栈顶，偏移从 totalStackSize - 4 开始
            int offset = totalStackSize - savedRegistersSize;
            for (String reg : usedCalleeSavedRegs) {
                builder.store("sw", reg, offset, "sp");
                offset += 4;
            }
        }



        // Generate code for each statement using visitor pattern
        StmtCodeGenerator codeGen = new StmtCodeGenerator(builder, allocator);
        for (Stmt stmt : ir.getStmts()) {
            stmt.accept(codeGen);
        }


        // 在函数末尾生成统一的 epilogue（不完美但简单）
        if (totalStackSize > 0) {
            builder.comment("Function epilogue");
            int offset = totalStackSize - savedRegistersSize;
            for (String reg : usedCalleeSavedRegs) {
                builder.load("lw", reg, offset, "sp");
                offset += 4;
            }
            builder.addi("sp", "sp", String.valueOf(totalStackSize));
        }
        builder.ret();



        return builder.toString();
    }

    /**
     * Calculate simple live intervals for all variables in the IR.
     * This is a placeholder implementation - in a full compiler, you would
     * use proper live variable analysis.
     */
    private boolean hasFunctionCall(IR ir) {
        return ir.getStmts().stream().anyMatch(stmt -> stmt instanceof Call);
    }

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
