package toyc.ir.value;

import toyc.ir.util.CounterManager;

public class Temporary extends Value {
    private final String name;
    
    public Temporary() {
        this.name = "t" + CounterManager.nextTempId();
    }
    
    public String getName() {
        return name;
    }
    
    @Override
    public String toString() {
        return name;
    }
}