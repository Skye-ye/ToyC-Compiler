package toyc.ir.util;

/**
 * Centralized counter management for all IR component ID generation
 */
public final class CounterManager {
    private static int instructionCounter = 0;
    private static int blockCounter = 0;
    private static int tempCounter = 0;
    private static int labelCounter = 0;
    
    private CounterManager() {
        // Utility class - prevent instantiation
    }
    
    public static void resetAll() {
        instructionCounter = 0;
        blockCounter = 0;
        tempCounter = 0;
        labelCounter = 0;
    }
    
    public static int nextInstructionId() {
        return instructionCounter++;
    }
    
    public static int nextBlockId() {
        return blockCounter++;
    }
    
    public static int nextTempId() {
        return tempCounter++;
    }
    
    public static int nextLabelId() {
        return labelCounter++;
    }
    
    public static void resetBlockCounter() {
        blockCounter = 0;
    }
}