package toyc.codegen;

import toyc.language.Function;
import toyc.ir.IR;
import toyc.language.Program;

import java.lang.reflect.Field;

public interface AssemblyGenerator {


    private static TargetArchitecture.Architecture getArchitecture(Class<?> clazz) throws NoSuchFieldException, IllegalAccessException {
        Field archField = clazz.getField("ARCH");

        // Validate field
        if (!java.lang.reflect.Modifier.isStatic(archField.getModifiers()) ||
                !java.lang.reflect.Modifier.isFinal(archField.getModifiers()) ||
                !java.lang.reflect.Modifier.isPublic(archField.getModifiers())) {
            throw new IllegalStateException(clazz.getSimpleName() +
                    ".ARCH must be public static final");
        }

        return (TargetArchitecture.Architecture) archField.get(null);
    }

    /**
     * Generates assembly code from the given program
     *
     * @param program the program to generate assembly code for
     * @return the generated assembly code as a string
     */
    String generateProgramAssembly(Program program);

    /**
     * Generates assembly code from the given function
     *
     * @param function the function to generate assembly code for
     * @return the generated assembly code as a string
     */
    String generateFunctionAssembly(Function function);

    /**
     * Generates the prologue for a function in assembly code.
     *
     * @param ir the intermediate representation of the function
     * @return the assembly code for the function prologue
     */
    String generateFunctionAssembly(IR ir);

    /**
     * Returns the target architecture for which this generator is designed.
     * This method extracts the architecture from a static field named "ARCH"
     * in the implementing class.
     *
     * @return the target architecture
     */
    default TargetArchitecture.Architecture getArchitecture() {
        return extractArchFromStaticField();
    }

    /**
     * Helper method to extract ARCH field using reflection
     */
    private TargetArchitecture.Architecture extractArchFromStaticField() {
        Class<?> clazz = this.getClass();
        try {
            TargetArchitecture.Architecture arch = getArchitecture(clazz);

            if (arch == null) {
                throw new IllegalStateException(clazz.getSimpleName() + ".ARCH cannot be null");
            }

            return arch;

        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(clazz.getSimpleName() +
                    " must have a public static final ARCH field", e);
        } catch (Exception e) {
            throw new RuntimeException("Error accessing ARCH field in " + clazz.getSimpleName(), e);
        }
    }
}