package toyc.codegen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 生成RISC-V32函数的prologue和epilogue代码
 */
public class PrologueEpilogueGen {

    /**
     * 生成函数的prologue代码
     * @param builder 汇编代码构建器
     * @param frameInfo 栈帧信息
     */
    public static void generatePrologue(RISCV32AsmBuilder builder, StackFrameInfo frameInfo) {
        // 1. 如果需要栈空间，则分配栈空间
        if (frameInfo.getTotalStackSize() > 0) {
            // 分配栈空间：sp = sp - 总栈大小
            builder.op3("addi", "sp", "sp", "-" + frameInfo.getTotalStackSize());
        }

        // 2. 保存使用的callee-saved寄存器
        if (frameInfo.hasCalleeSavedRegs()) {
            saveCalleeSavedRegisters(builder, frameInfo);
        }
    }

    /**
     * 生成函数的epilogue代码
     * @param builder 汇编代码构建器
     * @param frameInfo 栈帧信息
     */
    public static void generateEpilogue(RISCV32AsmBuilder builder, StackFrameInfo frameInfo) {
        // 1. 恢复使用的callee-saved寄存器
        if (frameInfo.hasCalleeSavedRegs()) {
            restoreCalleeSavedRegisters(builder, frameInfo);
        }

        // 2. 恢复栈指针
        if (frameInfo.getTotalStackSize() > 0) {
            // 恢复栈空间：sp = sp + 总栈大小
            builder.op3("addi", "sp", "sp", String.valueOf(frameInfo.getTotalStackSize()));
        }
    }

    /**
     * 保存callee-saved寄存器到栈中
     */
    private static void saveCalleeSavedRegisters(RISCV32AsmBuilder builder, StackFrameInfo frameInfo) {
        // 按寄存器名称排序，确保保存和恢复的顺序一致
        List<String> sortedRegs = new ArrayList<>(frameInfo.getUsedCalleeSavedRegs());
        Collections.sort(sortedRegs);

        for (String register : sortedRegs) {
            int offset = frameInfo.getCalleeSavedRegOffset(register);
            // 保存寄存器：sw 寄存器, 偏移量(sp)
            builder.store("sw", register, offset, "sp");
        }
    }

    /**
     * 从栈中恢复callee-saved寄存器
     */
    private static void restoreCalleeSavedRegisters(RISCV32AsmBuilder builder, StackFrameInfo frameInfo) {
        // 按寄存器名称排序，确保保存和恢复的顺序一致
        List<String> sortedRegs = new ArrayList<>(frameInfo.getUsedCalleeSavedRegs());
        Collections.sort(sortedRegs);

        for (String register : sortedRegs) {
            int offset = frameInfo.getCalleeSavedRegOffset(register);
            // 恢复寄存器：lw 寄存器, 偏移量(sp)
            builder.load("lw", register, offset, "sp");
        }
    }
}