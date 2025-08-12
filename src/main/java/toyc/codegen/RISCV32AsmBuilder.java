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

    /**
     * Add immediate instruction (addi dest, src, immediate)
     * @param dest the destination register
     * @param src the source register  
     * @param immediate the immediate value
     */
    public void addi(String dest, String src, String immediate) {
        textDefineBuffer.append(String.format("  addi %s, %s, %s\n", dest, src, immediate));
    }

    /**
     * Store word with string offset
     * @param src the source register
     * @param offset the offset as string
     * @param base the base register
     */
    public void sw(String src, String offset, String base) {
        textDefineBuffer.append(String.format("  sw %s, %s(%s)\n", src, offset, base));
    }

    /**
     * Load word with string offset
     * @param dest the destination register
     * @param offset the offset as string
     * @param base the base register
     */
    public void lw(String dest, String offset, String base) {
        textDefineBuffer.append(String.format("  lw %s, %s(%s)\n", dest, offset, base));
    }

    /**
     * Add comment to the assembly output
     * @param comment the comment text
     */
    public void comment(String comment) {
        textDefineBuffer.append(String.format("  # %s\n", comment));
    }

    /**
     * Unconditional jump to label
     * @param label the target label
     */
    public void j(String label) {
        textDefineBuffer.append(String.format("  j %s\n", label));
    }

    /**
     * Jump and link (function call to label)
     * @param label the target label
     */
    public void jal(String label) {
        textDefineBuffer.append(String.format("  jal %s\n", label));
    }

    /**
     * Jump register (indirect jump)
     * @param reg the register containing target address
     */
    public void jr(String reg) {
        textDefineBuffer.append(String.format("  jr %s\n", reg));
    }

    /**
     * Branch if equal
     * @param rs1 first register
     * @param rs2 second register 
     * @param label target label
     */
    public void beq(String rs1, String rs2, String label) {
        textDefineBuffer.append(String.format("  beq %s, %s, %s\n", rs1, rs2, label));
    }

    /**
     * Branch if not equal
     * @param rs1 first register
     * @param rs2 second register
     * @param label target label
     */
    public void bne(String rs1, String rs2, String label) {
        textDefineBuffer.append(String.format("  bne %s, %s, %s\n", rs1, rs2, label));
    }

    /**
     * Branch if less than
     * @param rs1 first register
     * @param rs2 second register
     * @param label target label
     */
    public void blt(String rs1, String rs2, String label) {
        textDefineBuffer.append(String.format("  blt %s, %s, %s\n", rs1, rs2, label));
    }

    /**
     * Branch if greater than or equal
     * @param rs1 first register
     * @param rs2 second register
     * @param label target label
     */
    public void bge(String rs1, String rs2, String label) {
        textDefineBuffer.append(String.format("  bge %s, %s, %s\n", rs1, rs2, label));
    }

    /**
     * Branch if less than (unsigned)
     * @param rs1 first register
     * @param rs2 second register
     * @param label target label
     */
    public void bltu(String rs1, String rs2, String label) {
        textDefineBuffer.append(String.format("  bltu %s, %s, %s\n", rs1, rs2, label));
    }

    /**
     * Branch if greater than or equal (unsigned)
     * @param rs1 first register
     * @param rs2 second register
     * @param label target label
     */
    public void bgeu(String rs1, String rs2, String label) {
        textDefineBuffer.append(String.format("  bgeu %s, %s, %s\n", rs1, rs2, label));
    }

    /**
     * Set if less than
     * @param rd destination register
     * @param rs1 first source register
     * @param rs2 second source register
     */
    public void slt(String rd, String rs1, String rs2) {
        textDefineBuffer.append(String.format("  slt %s, %s, %s\n", rd, rs1, rs2));
    }

    /**
     * Set if less than immediate
     * @param rd destination register
     * @param rs1 source register
     * @param imm immediate value
     */
    public void slti(String rd, String rs1, int imm) {
        textDefineBuffer.append(String.format("  slti %s, %s, %d\n", rd, rs1, imm));
    }

    /**
     * Set if less than (unsigned)
     * @param rd destination register
     * @param rs1 first source register
     * @param rs2 second source register
     */
    public void sltu(String rd, String rs1, String rs2) {
        textDefineBuffer.append(String.format("  sltu %s, %s, %s\n", rd, rs1, rs2));
    }

    /**
     * Bitwise XOR
     * @param rd destination register
     * @param rs1 first source register
     * @param rs2 second source register
     */
    public void xor(String rd, String rs1, String rs2) {
        textDefineBuffer.append(String.format("  xor %s, %s, %s\n", rd, rs1, rs2));
    }

    /**
     * Bitwise OR
     * @param rd destination register
     * @param rs1 first source register
     * @param rs2 second source register
     */
    public void or(String rd, String rs1, String rs2) {
        textDefineBuffer.append(String.format("  or %s, %s, %s\n", rd, rs1, rs2));
    }

    /**
     * Bitwise AND
     * @param rd destination register
     * @param rs1 first source register
     * @param rs2 second source register
     */
    public void and(String rd, String rs1, String rs2) {
        textDefineBuffer.append(String.format("  and %s, %s, %s\n", rd, rs1, rs2));
    }

    /**
     * No operation (pseudo-instruction)
     */
    public void nop() {
        textDefineBuffer.append("  nop\n");
    }

    /**
     * Load address (pseudo-instruction)
     * @param rd destination register
     * @param symbol symbol name
     */
    public void la(String rd, String symbol) {
        textDefineBuffer.append(String.format("  la %s, %s\n", rd, symbol));
    }

    /**
     * Set register to zero (pseudo-instruction for add rd, x0, x0)
     * @param rd destination register
     */
    public void zero(String rd) {
        textDefineBuffer.append(String.format("  mv %s, zero\n", rd));
    }
}