package toyc.codegen;

public class AsmBuilder {
    private final StringBuffer textDeclareBuffer;
    private final StringBuffer textDefineBuffer;

    public AsmBuilder() {
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

    // Load instructions (load dest, offset(base))
    public void load(String op, String dest, int offset, String base) {
        textDefineBuffer.append(String.format("  %s %s, %d(%s)\n", op, dest, offset, base));
    }

    // Store instructions (store src, offset(base))
    public void store(String op, String src, int offset, String base) {
        textDefineBuffer.append(String.format("  %s %s, %d(%s)\n", op, src, offset, base));
    }

    // Load immediate (li dest, value)
    public void li(String dest, int value) {
        textDefineBuffer.append(String.format("  li %s, %d\n", dest, value));
    }

    // Move (mv dest, src)
    public void mv(String dest, String src) {
        textDefineBuffer.append(String.format("  mv %s, %s\n", dest, src));
    }

    // Call (call funcName)
    public void call(String funcName) {
        textDefineBuffer.append(String.format("  call %s\n", funcName));
    }

    public void ret(int epilogue) {
        textDefineBuffer.append(String.format("  addi sp, sp, %d\n", epilogue));
        textDefineBuffer.append("  ret\n");
    }

    public void addPrologue(String funcName, int size) {
        String label = funcName + ":\n";
        int labelIndex = textDefineBuffer.indexOf(label);

        if (labelIndex != -1) {
            int insertPoint = labelIndex + label.length();
            StringBuilder prologue = new StringBuilder();
            prologue.append("  addi sp, sp, ").append(-size).append("\n");
            textDefineBuffer.insert(insertPoint, prologue);
        }
    }

    public void addGlobalFunc(String name, int size) {
        textDeclareBuffer.append(String.format("  .global %s\n", name));
        textDefineBuffer.append(String.format("%s:\n", name));
        textDefineBuffer.append(String.format("  addi sp, sp, %d\n", -size));
    }

    public void addLabel(String label) {
        textDefineBuffer.append(String.format("%s:\n", label));
    }

    // Get the assembled code
    public String toString() {
        return textDeclareBuffer.toString() + textDefineBuffer.toString();
    }
}