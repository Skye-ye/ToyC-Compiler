package toyc.ir.util;

import toyc.ir.instruction.BinaryOpInstruction;

public final class OperatorUtils {
    
    private OperatorUtils() {
        // Utility class - prevent instantiation
    }
    
    public static String operatorToString(BinaryOpInstruction.BinaryOp operator) {
        return switch (operator) {
            case ADD -> "+";
            case SUB -> "-";
            case MUL -> "*";
            case DIV -> "/";
            case MOD -> "%";
            case LT -> "<";
            case GT -> ">";
            case LE -> "<=";
            case GE -> ">=";
            case EQ -> "==";
            case NEQ -> "!=";
            case AND -> "&&";
            case OR -> "||";
        };
    }
}