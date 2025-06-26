package toyc.semantic;

public final class OutputHelper {
    private OutputHelper() {}

    public enum ErrorType {
        UNDEF_VAR("Undefined variable"),
        UNDEF_FUNC("Undefined function"),

        REDEF_VAR("Variable redefinition"),
        REDEF_PARAM("Parameter redefinition"),
        REDEF_FUNC("Function redefinition"),

        TYPE_MISMATCH_ASSIGN("Type mismatched for assignment"),
        TYPE_MISMATCH_OPERAND("Type mismatched for operand"),
        TYPE_MISMATCH_RETURN("Type mismatched for return"),

        ARGS_MISMATCH("Arguments mismatched"),

        NON_FUNC_CALL("Call of non-function"),
        NON_VAR_ASSIGN("Assignment to non-variable"),

        BREAK_OUTSIDE_WHILE("Break statement appears outside while block"),
        CONTINUE_OUTSIDE_WHILE("Continue statement appears outside while " +
                "block"),

        UNDEF_MAIN("Main function is not defined"),
        MAIN_RETURN_NON_INT_TYPE("Main function must return int"),
        MAIN_NON_EMPTY_PARAM("Main function must not have parameters"),

        VOID_RETURN_FUNC_USE_AS_RVAL("Void return function cannot be used as " +
                "rvalue"),

        NON_VOID_FUNC_MISSING_RETURN("Non-void function must return a value"),

        INTEGER_OVERFLOW("Integer overflow occurred"),

        ZERO_DIVISION("Division by zero is not allowed");

        private final String message;

        ErrorType(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    // Static method to print semantic errors with line number and identifier
    public static void printTypeError(ErrorType errorType, int line, String identifier) {
        String errorMessage = String.format("Error at Line %d: %s '%s'.",
                line,
                errorType.getMessage(),
                identifier);
        System.err.println(errorMessage);
    }

    // Overloaded method for errors that don't need an identifier
    public static void printTypeError(ErrorType errorType, int line) {
        String errorMessage = String.format("Error at Line %d: %s.",
                line,
                errorType.getMessage());
        System.err.println(errorMessage);
    }
}