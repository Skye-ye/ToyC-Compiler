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
    void testError2() throws IOException {
        OutputCapture capture = new OutputCapture();
        try {
            String filePath = getResourcePath("error2.toyc");
            Compiler.main(new String[]{filePath});
            assertTrue(capture.getError().contains("Error type 5 at Line 2: Function redefinition 'func'"),
                    "error2.toyc should report duplicate parameter names or redefinition error. Error was: " + capture.getError());
        } finally {
            capture.restore();
        }
    }

    // Apply the same pattern for all other tests...

    @Test
    void testError3() throws IOException {
        OutputCapture capture = new OutputCapture();
        try {
            String filePath = getResourcePath("error3.toyc");
            Compiler.main(new String[]{filePath});
            assertTrue(capture.getError().contains("using function as variable"),
                    "error3.toyc should report using function as variable error. Error was: " + capture.getError());
        } finally {
            capture.restore();
        }
    }

    @Test
    void testError4() throws IOException {
        OutputCapture capture = new OutputCapture();
        try {
            String filePath = getResourcePath("error4.toyc");
            Compiler.main(new String[]{filePath});
            assertTrue(capture.getError().contains("void function returning value"),
                    "error4.toyc should report void function returning value error. Error was: " + capture.getError());
        } finally {
            capture.restore();
        }
    }

    @Test
    void testError5() throws IOException {
        OutputCapture capture = new OutputCapture();
        try {
            String filePath = getResourcePath("error5.toyc");
            Compiler.main(new String[]{filePath});
            assertTrue(capture.getError().contains("incorrect argument count"),
                    "error5.toyc should report incorrect argument count error. Error was: " + capture.getError());
        } finally {
            capture.restore();
        }
    }

    @Test
    void testError6() throws IOException {
        OutputCapture capture = new OutputCapture();
        try {
            String filePath = getResourcePath("error6.toyc");
            Compiler.main(new String[]{filePath});
            assertTrue(capture.getError().contains("variable used as function"),
                    "error6.toyc should report variable used as function error. Error was: " + capture.getError());
        } finally {
            capture.restore();
        }
    }

    @Test
    void testError7() throws IOException {
        OutputCapture capture = new OutputCapture();
        try {
            String filePath = getResourcePath("error7.toyc");
            Compiler.main(new String[]{filePath});
            assertTrue(capture.getError().contains("assigning to function"),
                    "error7.toyc should report assigning to function error. Error was: " + capture.getError());
        } finally {
            capture.restore();
        }
    }

    @Test
    void testError8() throws IOException {
        OutputCapture capture = new OutputCapture();
        try {
            String filePath = getResourcePath("error8.toyc");
            Compiler.main(new String[]{filePath});
            assertTrue(capture.getError().contains("void function used in condition"),
                    "error8.toyc should report void function used in condition error. Error was: " + capture.getError());
        } finally {
            capture.restore();
        }
    }

    @Test
    void testError9() throws IOException {
        OutputCapture capture = new OutputCapture();
        try {
            String filePath = getResourcePath("error9.toyc");
            Compiler.main(new String[]{filePath});
            assertTrue(capture.getError().contains("misplaced continue/break"),
                    "error9.toyc should report misplaced continue/break error. Error was: " + capture.getError());
        } finally {
            capture.restore();
        }
    }
}