package toyc.ir.value;

public class Temporary extends Variable {
    private static int nextId = 0;
    
    public Temporary() {
        super("t" + nextId++);
    }
    
    public static void resetCounter() {
        nextId = 0;
    }
}