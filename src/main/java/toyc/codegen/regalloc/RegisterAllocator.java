package toyc.codegen.regalloc;
import java.util.Set;
import java.util.List;
public interface RegisterAllocator {
    /**
     * Allocate a location (register or stack) for a variable
     * @param varName The variable name to allocate
     * @return Location The allocated location (register or stack offset)
     */
    LocalDataLocation allocate(String varName);

    /**
     * Get the total stack size needed for the current function
     * @return int The required stack size in bytes
     */
    int getStackSize();
    Set<String> getUsedCalleeSavedRegisters();

    // 新增：本函数使用到的所有 caller-saved（t0–t6）寄存器
    // 供调用点在 call 前后成对保存/恢复
    Set<String> getUsedCallerSavedRegisters();
}