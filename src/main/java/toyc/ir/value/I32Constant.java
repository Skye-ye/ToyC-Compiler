package toyc.ir.value;

public class I32Constant extends Value {
    private final int value;
    
    public I32Constant(int value) {
        this.value = value;
    }
    
    public int getValue() {
        return value;
    }
    
    @Override
    public String toString() {
        return String.valueOf(value);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        I32Constant constant = (I32Constant) obj;
        return value == constant.value;
    }
    
    @Override
    public int hashCode() {
        return Integer.hashCode(value);
    }
}