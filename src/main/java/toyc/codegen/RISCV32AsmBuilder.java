package toyc.codegen;

public class RISCV32AsmBuilder {
    private final StringBuffer textDeclareBuffer;
    private final StringBuffer textDefineBuffer;

    public RISCV32AsmBuilder() {
        textDeclareBuffer = new StringBuffer();
        textDefineBuffer = new StringBuffer();
    }

    /**
     * Add two-operand instruction (op dest, src)
     * @param op the operation to perform (e.g., "la")
     * @param dest the destination register
     * @param src the source register
     */
    public void op2(String op, String dest, String src) {
        textDefineBuffer.append(String.format("  %s %s, %s\n", op, dest, src));
    }

    /**
     * Add three-operand instruction (op dest, src1, src2)
     * This is typically used for arithmetic operations like add, sub, etc.
     * @param op the operation to perform (e.g., "add")
     * @param dest the destination register
     * @param src1 the first source register
     * @param src2 the second source register
     */
    public void op3(String op, String dest, String src1, String src2) {
        textDefineBuffer.append(String.format("  %s %s, %s, %s\n", op, dest, src1, src2));
    }

    /**
     * Load instructions (lw, lhw etc.)
     * These instructions load a value from memory into a register.
     * @param op the operation to perform (e.g., "lw", "lh")
     * @param dest the destination register where the value will be loaded
     * @param offset the offset from the base address
     * @param base the base register that holds the address
     */
    public void load(String op, String dest, int offset, String base) {
        textDefineBuffer.append(String.format("  %s %s, %d(%s)\n", op, dest, offset, base));
    }

    /**
     * Store instructions (sw, sh etc.)
     * @param op the operation to perform (e.g., "sw", "sh")
     * @param src the source register that holds the value to be stored
     * @param offset the offset from the base address
     * @param base the base register that holds the address
     */
    public void store(String op, String src, int offset, String base) {
        textDefineBuffer.append(String.format("  %s %s, %d(%s)\n", op, src, offset, base));
    }

    /**
     * Load immediate value into a register (pseudo-instruction for addi
     * dest, x0, value)
     * @param dest the destination register where the immediate value will be loaded
     * @param value the immediate value to be loaded
     */
    public void li(String dest, int value) {
        textDefineBuffer.append(String.format("  li %s, %d\n", dest, value));
    }

    /**
     * Move value from one register to another (pseudo-instruction for add
     * dest, src, x0)
     * @param dest the destination register
     * @param src the source register
     */
    public void mv(String dest, String src) {
        textDefineBuffer.append(String.format("  mv %s, %s\n", dest, src));
    }

    /**
     * Call a function by its name
     * @param funcName the name of the function to call
     */
    public void call(String funcName) {
        textDefineBuffer.append(String.format("  call %s\n", funcName));
    }

    public void ret() {
        textDefineBuffer.append("  ret\n");
    }

    public void addGlobalFunc(String name) {
        textDeclareBuffer.append(String.format("  .global %s\n", name));
        textDefineBuffer.append(String.format("%s:\n", name));
    }

    public void addLabel(String label) {
        textDefineBuffer.append(String.format("%s:\n", label));
    }

    // Get the assembled code
    public String toString() {
        return textDeclareBuffer.toString() + textDefineBuffer.toString();
    }
}