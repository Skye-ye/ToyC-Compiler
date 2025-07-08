package toyc;

import toyc.ir.ToyCIRBuilder;
import toyc.language.Function;
import toyc.util.AbstractResultHolder;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages the whole-program information of the program being analyzed.
 * Note that the setters of this class are protected: they are supposed
 * to be called (once) by the world builder, not analysis classes.
 */
public final class World extends AbstractResultHolder {

    /**
     * The current world.
     */
    private static World theWorld;

    /**
     * The callbacks that will be invoked at resetting.
     * This is useful to clear class-level caches.
     */
    private static final List<Runnable> resetCallbacks = new ArrayList<>();

    private ToyCIRBuilder irBuilder;

    private Function mainFunction;

    private String sourceFileName;

    private final String baseOutputDir = System.getProperty("toyc.outputDir", "output");

    /**
     * Sets current world to {@code world}.
     */
    public static void set(World world) {
        theWorld = world;
    }

    /**
     * @return the current {@code World} instance.
     */
    public static World get() {
        return theWorld;
    }

    public static void registerResetCallback(Runnable callback) {
        resetCallbacks.add(callback);
    }

    public static void reset() {
        theWorld = null;
        resetCallbacks.forEach(Runnable::run);
    }

    public ToyCIRBuilder getIRBuilder() {
        return irBuilder;
    }

    public void setIRBuilder(ToyCIRBuilder irBuilder) {
        checkAndSet("irBuilder", irBuilder);
    }

    public Function getMainFunction() {
        return mainFunction;
    }

    public void setMainFunction(Function mainFunction) {
        checkAndSet("mainFunction", mainFunction);
    }

    public void setSourceFileName(String sourceFileName) {
        checkAndSet("sourceFileName", sourceFileName);
    }

    public String getSourceFileName() {
        return sourceFileName;
    }

    public File getOutputDir() {
        File outputDir;
        if (sourceFileName != null) {
            // Extract base name without extension
            String baseName = sourceFileName.replaceFirst("[.][^.]+$", "");
            outputDir = new File(baseOutputDir, baseName);
        } else {
            outputDir = new File(baseOutputDir);
        }
        
        // Create the directory if it doesn't exist
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        
        return outputDir;
    }

    /**
     * Sets value for specified field (by {@code fieldName}).
     * Ensures that the specified field is set at most once.
     */
    private void checkAndSet(String fieldName, Object value) {
        try {
            Field field = World.class.getDeclaredField(fieldName);
            if (field.get(this) != null) {
                throw new IllegalStateException(
                        "World." + fieldName + " already set");
            }
            field.set(this, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set World." + fieldName);
        }
    }
}
