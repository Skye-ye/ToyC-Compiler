package toyc.codegen.regalloc;

import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

public class StackOnlyAllocator implements RegisterAllocator {
    private int currentOffset = 0;
    private final Map<String, LocalDataLocation> allocations = new HashMap<>();

    @Override
    public LocalDataLocation allocate(String varName) {
        // Return existing allocation if already allocated
        if (allocations.containsKey(varName)) {
            return allocations.get(varName);
        }

        // Allocate new stack location
        LocalDataLocation location = LocalDataLocation.createStack(currentOffset);
        currentOffset += 4;
        allocations.put(varName, location);
        return location;
    }

    @Override
    public int getStackSize() {
        return (currentOffset + 15) & ~15;
    }

    /**
     * 【新增方法】: 实现 getUsedCalleeSavedRegisters 接口
     * StackOnlyAllocator 不使用任何寄存器，所以返回空集合
     */
    @Override
    public Set<String> getUsedCalleeSavedRegisters() {
        return Collections.emptySet();  // 不使用任何寄存器
    }
}