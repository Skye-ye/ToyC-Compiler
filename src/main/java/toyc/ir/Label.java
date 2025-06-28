package toyc.ir;

public class Label {
    private static int nextId = 0;
    private final String name;
    
    public Label(String name) {
        this.name = name;
    }
    
    public Label() {
        this.name = "L" + nextId++;
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
    
    public static void resetCounter() {
        nextId = 0;
    }
}