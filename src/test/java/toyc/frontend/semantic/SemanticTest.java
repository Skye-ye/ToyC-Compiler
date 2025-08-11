package toyc.frontend.semantic;

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
            SemanticTestDriver.main(new String[]{filePath});
            String actualError = capture.getError();

            String assertionMessage = String.format(
                    """
                            Test for '%s' failed.
                            Expected to find snippet:
                            \
                            %s
                            Actual error was:
                            \
                            %s\"""",
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
        assertCompilerError("VarUnDef.tc", "Undefined variable 'x'");
    }

    @Test
    void testFuncUndef() throws IOException {
        assertCompilerError("FuncUnDef.tc", "Undefined function 'func'");
    }

    @Test
    void testVarRedef() throws IOException {
        assertCompilerError("VarReDef.tc", "Variable redefinition 'x'");
    }

    @Test
    void testParamRedef() throws IOException {
        assertCompilerError("ParamReDef.tc", "Parameter redefinition 'a'");
    }

    @Test
    void testFuncRedef() throws IOException {
        assertCompilerError("FuncReDef.tc", "Function redefinition 'func'");
    }

    @Test
    void testAssignTypeMismatch() throws IOException {
        // Corrected: The original code passed an error message to getResourcePath.
        assertCompilerError("AssignTypeMismatch.tc", "Type mismatched for assignment");
    }

    @Test
    void testOperandTypeMismatch() throws IOException {
        // Corrected: The original code passed an error message to getResourcePath.
        assertCompilerError("OperandTypeMismatch.tc", "Type mismatched for " +
                "operand '+'");
    }

    @Test
    void testReturnTypeMismatch() throws IOException {
        assertCompilerError("ReturnTypeMismatch.tc", "Type mismatched for return");
    }

    @Test
    void testArgsMismatch() throws IOException {
        assertCompilerError("ArgsMismatch.tc", "Arguments mismatched 'func'");
    }

    @Test
    void testNonFuncCall() throws IOException {
        assertCompilerError("NonFuncCall.tc", "Call of non-function 'a'");
    }

    @Test
    void testAssignNonVar() throws IOException {
        assertCompilerError("AssignNonVar.tc", "Assignment to non-variable " +
                "'func'");
    }

    @Test
    void testBreakOutsideWhile() throws IOException {
        assertCompilerError("BreakOutsideWhile.tc", "Error at Line 2, Column 5: Break statement appears outside while block.");
    }

    @Test
    void testContinueOutsideWhile() throws IOException {
        assertCompilerError("ContinueOutsideWhile.tc", "Error at Line 2, Column 5: Continue statement appears outside while block.");
    }

    @Test
    void testUndefMain() throws IOException {
        assertCompilerError("UnDefMain.tc", "Main function is not defined");
    }

    @Test
    void testMainReturnNonIntType() throws IOException {
        assertCompilerError("MainReturnNonIntType.tc", "Main function must return int");
    }

    @Test
    void testMainNonEmptyParam() throws IOException {
        assertCompilerError("MainNonEmptyParam.tc", "Main function must not have parameters");
    }

    @Test
    void testVoidReturnFuncUseAsRval() throws IOException {
        assertCompilerError("VoidReturnFuncUseAsRval.tc", "Error at Line 4," +
                " Column 12: Void return function cannot be used as rvalue " +
                "'func'.\nError at Line 8, Column 9: Void return function " +
                "cannot be used as rvalue 'func'.");
    }

    @Test
    void testNonVoidFuncMissingReturn() throws IOException {
        assertCompilerError("NonVoidFuncMissingReturn.tc", """
                Error at Line \
                1, Column 5: Non-void function must return a value 'func'.
                Error at Line 3, Column 5: Non-void function must return \
                a value 'func2'.
                Error at Line 12, Column 5: Non-void \
                function must return a value 'func3'.""");
    }

    @Test
    void testIntegerOverflow() throws IOException {
        assertCompilerError("IntegerOverflow.tc", """
                Error at Line 2, Column 13: Integer overflow occurred.
                Error at Line 3, Column 14: Integer overflow occurred.
                Error at Line 4, Column 13: Integer overflow occurred.
                Error at Line 5, Column 16: Integer overflow occurred.
                """);
    }

    @Test
    void testZeroDivision() throws IOException {
        assertCompilerError("ZeroDivision.tc", "Error at Line 2, Column 15: Division by zero is not allowed.");
    }
}