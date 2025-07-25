package toyc.util;

/**
 * Represents an operation that accepts three input arguments and returns no result.
 * This is the three-arity specialization of Consumer. Unlike most other
 * functional interfaces, TriConsumer is expected to operate via side-effects.
 * This is a functional interface whose functional method is
 * accept(Object, Object, Object).
 *
 * @param <T> the type of the first argument to the operation
 * @param <U> the type of the second argument to the operation
 * @param <V> the type of the third argument to the operation
 */
@FunctionalInterface
public interface TriConsumer<T, U, V> {

    /**
     * Applies this function to the given arguments.
     *
     * @param t the first input argument
     * @param u the second input argument
     * @param v the third input argument
     */
    void accept(T t, U u, V v);
}
