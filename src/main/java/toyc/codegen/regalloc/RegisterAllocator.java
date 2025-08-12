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


}