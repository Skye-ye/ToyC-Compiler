package toyc.ir;

import toyc.ir.util.CounterManager;

public class Label {
    private final String name;
    
    public Label(String name) {
        this.name = name;
    }
    
    public Label() {
        this.name = "L" + CounterManager.nextLabelId();
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
        Label label = (Label) obj;
        return name.equals(label.name);
    }
    
    @Override
    public int hashCode() {
        return name.hashCode();
    }
}