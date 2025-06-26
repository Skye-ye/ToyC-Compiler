package toyc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SemanticTest {
    // OutputCapture class remains the same...
    static class OutputCapture {
        private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
        private final PrintStream originalOut = System.out;
        private final PrintStream originalErr = System.err;

        public OutputCapture() {
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

    /**
     * Helper method to get the absolute path of a test resource file.
     * This is the key change.
     * @param resourceName The name of the file inside src/test/resources/toyc/semantic/
     * @return The absolute path to the file.
     */
    private String getResourcePath(String resourceName) {
        // The path inside the resources folder
        String resourcePath = "/toyc/semantic/" + resourceName;
        URL resource = this.getClass().getResource(resourcePath);

        // Assert that the resource was found, otherwise the test setup is wrong.
        Assertions.assertNotNull(resource, "Test resource not found: " + resourcePath);

        // Return the absolute path for the Compiler to use.
        return resource.getPath();
    }

    @Test
    void testFuncUndef() throws IOException {
        OutputCapture capture = new OutputCapture();
        try {
            // Use the helper method to get the correct file path
            String filePath = getResourcePath("funcUnDef.toyc");
            Compiler.main(new String[]{filePath});
            assertTrue(capture.getError().contains("Error type 2 at Line 4: Undefined function 'func'"),
                    "funcUnDef.toyc should report undefined function call error. Error was: " + capture.getError());
        } finally {
            capture.restore();
        }
    }

    @Test
    void testVarUndef() throws IOException {
        OutputCapture capture = new OutputCapture();
        try {
            String filePath = getResourcePath("varUnDef.toyc");
            Compiler.main(new String[]{filePath});
            assertTrue(capture.getError().contains("Error type 1 at Line 2:  Undefined variable 'a'"),
                    "varUnDef.toyc should report duplicate parameter names or redefinition error. Error was: " + capture.getError());
        } finally {
            capture.restore();
        }
    }

    // Apply the same pattern for all other tests...

    @Test
    void testVarRedef() throws IOException {
        OutputCapture capture = new OutputCapture();
        try {
            String filePath = getResourcePath("varReDef.toyc");
            Compiler.main(new String[]{filePath});
            assertTrue(capture.getError().contains("Error type 3 at Line 3: Variable redefinition 'x'"),
                    "varReDef.toyc should report using function as variable error. Error was: " + capture.getError());
        } finally {
            capture.restore();
        }
    }

    @Test
    void testParRedef() throws IOException {
        OutputCapture capture = new OutputCapture();
        try {
            String filePath = getResourcePath("parReDef.toyc");
            Compiler.main(new String[]{filePath});
            assertTrue(capture.getError().contains("Error type 4 at Line 1: Parameter redefinition 'a'"),
                    "parReDef.toyc should report void function returning value error. Error was: " + capture.getError());
        } finally {
            capture.restore();
        }
    }

    @Test
    void testFuncRedef() throws IOException {
        OutputCapture capture = new OutputCapture();
        try {
            String filePath = getResourcePath("funcReDef.toyc");
            Compiler.main(new String[]{filePath});
            assertTrue(capture.getError().contains("Error type 5 at Line 3: Function redefinition 'main'"),
                    "funcReDef.toyc should report incorrect argument count error. Error was: " + capture.getError());
        } finally {
            capture.restore();
        }
    }

    @Test
    void testAssignMismatch() throws IOException {
        OutputCapture capture = new OutputCapture();
        try {
            String filePath = getResourcePath("Error type 1 at Line 2: Undefined variable 'hello'.");
            Compiler.main(new String[]{filePath});
            assertTrue(capture.getError().contains("variable used as function"),
                    "assTyMis.toyc should report variable used as function error. Error was: " + capture.getError());
        } finally {
            capture.restore();
        }
    }

    @Test
    void testOperMismatch() throws IOException {
        OutputCapture capture = new OutputCapture();
        try {
            String filePath = getResourcePath("oprTyMis.toyc");
            Compiler.main(new String[]{filePath});
            assertTrue(capture.getError().contains("Error type 1 at Line 2: Undefined variable 'world'"),
                    "oprTyMis.toyc should report assigning to function error. Error was: " + capture.getError());
        } finally {
            capture.restore();
        }
    }

    @Test
    void testRetrunMismatch() throws IOException {
        OutputCapture capture = new OutputCapture();
        try {
            String filePath = getResourcePath("retTyMis.toyc");
            Compiler.main(new String[]{filePath});
            assertTrue(capture.getError().contains("void function used in condition"),
                    "retTyMis.toyc should report void function used in condition error. Error was: " + capture.getError());
        } finally {
            capture.restore();
        }
    }

    @Test
    void testArgMismatch() throws IOException {
        OutputCapture capture = new OutputCapture();
        try {
            String filePath = getResourcePath("argMis.toyc");
            Compiler.main(new String[]{filePath});
            assertTrue(capture.getError().contains("Error type 9 at Line 6: Arguments mismatched 'sum'"),
                    "argMis.toyc should report misplaced continue/break error. Error was: " + capture.getError());
        } finally {
            capture.restore();
        }
    }

    @Test
    void testNonFunccall() throws IOException {
        OutputCapture capture = new OutputCapture();
        try {
            String filePath = getResourcePath("nonFuncCall.toyc");
            Compiler.main(new String[]{filePath});
            assertTrue(capture.getError().contains("Error type 10 at Line 3: Call of non-function 'a'"),
                    "nonFuncCall.toyc should report misplaced continue/break error. Error was: " + capture.getError());
        } finally {
            capture.restore();
        }
    }

    @Test
    void testAssignNonvar() throws IOException {
        OutputCapture capture = new OutputCapture();
        try {
            String filePath = getResourcePath("assNonVar.toyc");
            Compiler.main(new String[]{filePath});
            assertTrue(capture.getError().contains("Error type B at Line 2: mismatched input '=' expecting {'+', '-', '*', '/', '%', '==', '!=', '<', '>', '<=', '>=', '&&', '||', ';'}"),
                    "assNonVar.toyc should report misplaced continue/break error. Error was: " + capture.getError());
        } finally {
            capture.restore();
        }
    }
}