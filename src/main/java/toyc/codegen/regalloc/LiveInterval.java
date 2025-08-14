package toyc.codegen.regalloc;

public class LiveInterval {
    private final int start;        // First position
    private final int end;          // Last position
    private final int length;       // Length of the interval
    private final String variable;  // Variable name/identifier
    private int register;           // Assigned register (-1 if not assigned)

    public LiveInterval(int start, int end, String variable) {
        this.start = start;
        this.end = end;
        this.length = end - start + 1;
        this.variable = variable;
        this.register = -1;
    }

    public int getStart() { return start; }
    public int getEnd() { return end; }
    public int getLength() { return length; }
    public String getVariable() { return variable; }
    public int getRegister() { return register; }
    public void setRegister(int reg) { this.register = reg; }

    @Override
    public String toString() {
        return String.format("LiveInterval(%s, %d, %d, %d)", variable, start, end, register);
    }
}