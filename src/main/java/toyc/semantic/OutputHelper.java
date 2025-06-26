package toyc.semantic;

public class OutputHelper {
    public enum ErrorType {
        UNDEF_VAR("Undefined variable"),
        UNDEF_FUNC("Undefined function"),
        REDEF_VAR("Variable redefinition"),
        REDEF_PARAM("Parameter redefinition"),
        REDEF_FUNC("Function redefinition"),
        TYPE_MISMATCH_ASSIGN("Type mismatched for assignment"),
        TYPE_MISMATCH_OPERAND("Type mismatched for operands"),
        TYPE_MISMATCH_RETURN("Type mismatched for return"),
        ARGS_MISMATCH("Arguments mismatched"),
        NON_FUNC_CALL("Call of non-function"),
        NON_VAR_ASSIGN("Assignment to non-variable"),
        BREAK_OUTSIDE_WHILE("Break statement appears outside while block"),
        CONTINUE_OUTSIDE_WHILE("Continue statement appears outside while block");

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
        String errorMessage = String.format("Error type %d at Line %d: %s '%s'.",
                errorType.ordinal() + 1,
                line,
                errorType.getMessage(),
                identifier);
        System.err.println(errorMessage);
    }

    // Overloaded method for errors that don't need an identifier
    public static void printTypeError(ErrorType errorType, int line) {
        String errorMessage = String.format("Error type %d at Line %d: %s.",
                errorType.ordinal() + 1,
                line,
                errorType.getMessage());
        System.err.println(errorMessage);
    }
}