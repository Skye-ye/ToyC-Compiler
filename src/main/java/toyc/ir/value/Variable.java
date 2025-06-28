package toyc.ir.value;

public class Variable extends Value {
    private final String name;
    
    public Variable(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
    
    @Override
    public String toString() {
        return name;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Variable variable = (Variable) obj;
        return name.equals(variable.name);
    }
    
    @Override
    public int hashCode() {
        return name.hashCode();
    }
}