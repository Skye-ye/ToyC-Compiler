package toyc.codegen;

import toyc.codegen.regalloc.LocalDataLocation;
import toyc.codegen.regalloc.RegisterAllocator;
import toyc.ir.IR;
import toyc.ir.exp.Var;

import java.util.*;

/**
 * 计算和管理RISC-V32函数的栈帧信息
 */
public class StackFrameInfo {
    // RISC-V32 callee-saved寄存器（被调用者需要保存的寄存器）
    private static final Set<String> CALLEE_SAVED_REGISTERS = Set.of(
            "s1", "s2", "s3", "s4", "s5", "s6", "s7", "s8", "s9", "s10", "s11"
    );

    private final Set<String> usedCalleeSavedRegs;  // 实际使用的callee-saved寄存器
    private final int localVarStackSize;           // 局部变量占用的栈空间
    private final int calleeSavedRegSize;          // 保存callee-saved寄存器需要的栈空间
    private final int totalStackSize;             // 总栈空间大小

    public StackFrameInfo(IR ir, RegisterAllocator allocator) {
        this.usedCalleeSavedRegs = findUsedCalleeSavedRegisters(ir, allocator);
        this.localVarStackSize = allocator.getStackSize(); // 从寄存器分配器获取局部变量栈大小
        this.calleeSavedRegSize = usedCalleeSavedRegs.size() * 4; // 每个寄存器4字节

        // 计算总栈大小：局部变量 + 保存的寄存器 + 对齐
        int rawSize = localVarStackSize + calleeSavedRegSize;
        // RISC-V要求栈16字节对齐
        this.totalStackSize = (rawSize + 15) & ~15;
    }

    /**
     * 找出IR中实际使用的callee-saved寄存器
     */
    private Set<String> findUsedCalleeSavedRegisters(IR ir, RegisterAllocator allocator) {
        Set<String> used = new HashSet<>();

        // 遍历所有变量，检查分配给它们的寄存器
        for (Var var : ir.getVars()) {
            LocalDataLocation location = allocator.allocate(var.getName());
            if (location.getType() == LocalDataLocation.LocationType.REGISTER) {
                String register = location.getRegister();
                if (CALLEE_SAVED_REGISTERS.contains(register)) {
                    used.add(register);
                }
            }
        }

        return used;
    }

    /**
     * 获取callee-saved寄存器在栈中的偏移量
     * @param register 寄存器名称
     * @return 相对于sp的偏移量
     */
    public int getCalleeSavedRegOffset(String register) {
        if (!usedCalleeSavedRegs.contains(register)) {
            throw new IllegalArgumentException("Register " + register + " is not used");
        }

        // callee-saved寄存器保存在栈的底部（高地址）
        // 按照寄存器名称排序，确保偏移量的一致性
        List<String> sortedRegs = new ArrayList<>(usedCalleeSavedRegs);
        Collections.sort(sortedRegs);

        int index = sortedRegs.indexOf(register);
        // 从栈顶开始，局部变量空间之后就是callee-saved寄存器空间
        return localVarStackSize + index * 4;
    }

    // Getters
    public Set<String> getUsedCalleeSavedRegs() { return usedCalleeSavedRegs; }
    public int getTotalStackSize() { return totalStackSize; }
    public boolean hasCalleeSavedRegs() { return !usedCalleeSavedRegs.isEmpty(); }
}