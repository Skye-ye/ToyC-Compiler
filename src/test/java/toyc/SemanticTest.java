package toyc;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class SemanticTest {
    static class OutputCapture {
        private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
        private final PrintStream originalOut = System.out;
        private final PrintStream originalErr = System.err;

        public OutputCapture() {
            ByteArrayOutputStream outContent = new ByteArrayOutputStream();
            System.setOut(new PrintStream(outContent));
            System.setErr(new PrintStream(errContent));
        }

        public void restore() {
            System.setOut(originalOut);
            System.setErr(originalErr);
        }

        public String getError() {
            return errContent.toString();
        }
    }

    private String getResourcePath(String resourceName) {
        String resourcePath = "/toyc/semantic/" + resourceName;
        URL resource = this.getClass().getResource(resourcePath);
        Assertions.assertNotNull(resource, "Test resource not found: " + resourcePath);
        return resource.getPath();
    }

    private void assertCompilerError(String resourceName, String expectedErrorSnippet) throws IOException {
        OutputCapture capture = new OutputCapture();
        try {
            String filePath = getResourcePath(resourceName);
            Compiler.main(new String[]{filePath});
            String actualError = capture.getError();

            String assertionMessage = String.format(
                    """
                            Test for '%s' failed.
                            Expected to find snippet:
                             \
                            "%s"
                            Actual error was: "%s\"""",
                    resourceName,
                    expectedErrorSnippet,
                    actualError.trim()
            );
            assertTrue(actualError.contains(expectedErrorSnippet), assertionMessage);
        } finally {
            capture.restore();
        }
    }

    @Test
    void testVarUndef() throws IOException {
        assertCompilerError("VarUnDef.toyc", "Undefined variable 'x'");
    }

    @Test
    void testFuncUndef() throws IOException {
        assertCompilerError("FuncUnDef.toyc", "Undefined function 'func'");
    }

    @Test
    void testVarRedef() throws IOException {
        assertCompilerError("VarReDef.toyc", "Variable redefinition 'x'");
    }

    @Test
    void testParamRedef() throws IOException {
        assertCompilerError("ParamReDef.toyc", "Parameter redefinition 'a'");
    }

    @Test
    void testFuncRedef() throws IOException {
        assertCompilerError("FuncReDef.toyc", "Function redefinition 'func'");
    }

    @Test
    void testAssignTypeMismatch() throws IOException {
        // Corrected: The original code passed an error message to getResourcePath.
        assertCompilerError("AssignTypeMismatch.toyc", "Type mismatched for assignment");
    }

    @Test
    void testOperandTypeMismatch() throws IOException {
        // Corrected: The original code passed an error message to getResourcePath.
        assertCompilerError("OperandTypeMismatch.toyc", "Type mismatched for " +
                "operand '+'");
    }

    @Test
    void testReturnTypeMismatch() throws IOException {
        assertCompilerError("ReturnTypeMismatch.toyc", "Type mismatched for return");
    }

    @Test
    void testArgsMismatch() throws IOException {
        assertCompilerError("ArgsMismatch.toyc", "Arguments mismatched 'func'");
    }

    @Test
    void testNonFuncCall() throws IOException {
        assertCompilerError("NonFuncCall.toyc", "Call of non-function 'a'");
    }

    @Test
    void testAssignNonVar() throws IOException {
        assertCompilerError("AssignNonVar.toyc", "Assignment to non-variable " +
                "'func'");
    }

    @Test
    void testBreakOutsideWhile() throws IOException {
        assertCompilerError("BreakOutsideWhile.toyc", "Break statement appears outside while block");
    }

    @Test
    void testContinueOutsideWhile() throws IOException {
        assertCompilerError("ContinueOutsideWhile.toyc", "Continue statement appears outside while block");
    }

    @Test
    void testUndefMain() throws IOException {
        assertCompilerError("UnDefMain.toyc", "Main function is not defined");
    }

    @Test
    void testMainReturnNonIntType() throws IOException {
        assertCompilerError("MainReturnNonIntType.toyc", "Main function must return int");
    }

    @Test
    void testMainNonEmptyParam() throws IOException {
        assertCompilerError("MainNonEmptyParam.toyc", "Main function must not have parameters");
    }

    @Test
    void testVoidReturnFuncUseAsRval() throws IOException {
        assertCompilerError("VoidReturnFuncUseAsRval.toyc", "Error at Line 4:" +
                " Void return function cannot be used as rvalue 'func'\nError" +
                " at Line 8: Void return function cannot be used as rvalue 'func'");
    }

    @Test
    void testNonVoidFuncMissingReturn() throws IOException {
        assertCompilerError("NonVoidFuncMissingReturn.toyc", """
                Error \
                at line 1: Non-void function must return a value \
                'func'
                Error at line 3: Non-void function must return a \
                value 'func2'
                Error at line 12: Non-void function must return a value 'func3'""");
    }

    @Test
    void testIntegerOverflow() throws IOException {
        assertCompilerError("IntegerOverflow.toyc", "Integer overflow occurred");
    }

    @Test
    void testZeroDivision() throws IOException {
        assertCompilerError("ZeroDivision.toyc", "Division by zero is not allowed");
    }
}