package toyc;

import toyc.config.Options;
import toyc.ir.IRBuilder;
import toyc.language.Function;
import toyc.language.Program;
import toyc.frontend.cache.CachedIRBuilder;
import toyc.util.AbstractResultHolder;

import java.io.*;
import java.lang.reflect.Field;

/**
 * Manages the whole-program information of the program being analyzed.
 * Note that the setters of this class are protected: they are supposed
 * to be called (once) by the world builder, not analysis classes.
 */
public final class World extends AbstractResultHolder implements Serializable {

    /**
     * ZA WARUDO, i.e., the current world.
     */
    private static World theWorld;

    /**
     * Notes: This field is {@code transient} because it
     * should be set after deserialization.
     */
    private Options options;

    /**
     * Notes: add {@code transient} to wrap this {@link IRBuilder} using
     * {@link toyc.frontend.cache.CachedIRBuilder} in serialization.
     *
     * @see #writeObject(ObjectOutputStream)
     * @see #readObject(ObjectInputStream)
     */
    private transient IRBuilder irBuilder;

    private Program program;

    private Function mainFunction;

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

    public static void reset() {
        theWorld = null;
    }

    public Options getOptions() {
        return options;
    }

    public void setOptions(Options options) {
        checkAndSet("options", options);
    }

    public IRBuilder getIRBuilder() {
        return irBuilder;
    }

    public void setIRBuilder(IRBuilder irBuilder) {
        checkAndSet("irBuilder", irBuilder);
    }

    public Function getMainFunction() {
        return mainFunction;
    }

    public void setMainFunction(Function mainFunction) {
        checkAndSet("mainFunction", mainFunction);
    }

    public Program getProgram() {
        return program;
    }

    public void setProgram(Program program) {
        checkAndSet("program", program);
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

    @Serial
    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
        s.writeObject(new CachedIRBuilder(irBuilder, program));
    }

    @Serial
    private void readObject(ObjectInputStream s) throws IOException,
            ClassNotFoundException {
        s.defaultReadObject();
        setIRBuilder((IRBuilder) s.readObject());
    }
}
