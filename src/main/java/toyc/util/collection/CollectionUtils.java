package toyc.util.collection;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

/**
 * Utility methods for {@link Collection}.
 * We name it CollectionUtils instead of Collections to avoid name collision
 * with {@link Collections}.
 */
public final class CollectionUtils {

    private CollectionUtils() {
    }

    /**
     * Iterates the elements in the specific collection, in the order they are
     * returned by the collection's iterator, and finds the first element
     * of given collection that satisfies the predicate. If not such element
     * is found, returns {@code null}.
     */
    @Nullable
    public static <T> T findFirst(Collection<? extends T> c,
                                  Predicate<? super T> p) {
        for (T e : c) {
            if (p.test(e)) {
                return e;
            }
        }
        return null;
    }

    /**
     * @return an arbitrary element of the given collection.
     */
    public static <T> T getOne(Collection<T> c) {
        return c.iterator().next();
    }

    /**
     * Creates a list of given collection, appends a specific element to
     * the list and returns it.
     */
    public static <T> List<T> append(Collection<? extends T> c, T e) {
        List<T> result = new ArrayList<>(c.size() + 1);
        result.addAll(c);
        result.add(e);
        return result;
    }

    /**
     * Maps each element in given collection to an integer and computes
     * the sum of the integers.
     */
    public static <T> long sum(Collection<? extends T> c, ToIntFunction<T> toInt) {
        long sum = 0;
        for (var e : c) {
            sum += toInt.applyAsInt(e);
        }
        return sum;
    }

    /**
     * Converts a collection to a string.
     * The elements in the collection are <b>sorted</b> by their
     * string representation (in alphabet order) in the resulting string.
     * This is particularly useful for comparing expected results
     * with the ones given by the analysis.
     */
    public static <T> String toString(Collection<T> c) {
        return Streams.toString(c.stream());
    }

    /**
     * Converts a collection to a set.
     */
    public static <T> Set<T> toSet(Collection<T> c) {
        if (c instanceof Set) {
            return Collections.unmodifiableSet((Set<T>) c);
        } else {
            return Collections.unmodifiableSet(Sets.newHybridSet(c));
        }
    }

    /**
     * Splits the given list into sub-lists by a boolean field.
     * @param list the list to be split
     * @param fieldExtractor a function that extracts a boolean field from each element
     * of the list. The list will be split whenever this field is true.
     * The last sub-list will contain all remaining elements.
     * @return a list of sub-lists, where each sub-list contains elements
     * until the boolean field is true for an element.
     * @param <T> the type of elements in the list
     */
    public static <T> List<List<T>> splitByBooleanField(List<T> list, Function<T, Boolean> fieldExtractor) {
        if (list.isEmpty()) {
            return new ArrayList<>();
        }

        List<List<T>> result = new ArrayList<>();
        List<T> currentGroup = new ArrayList<>();

        for (T element : list) {
            currentGroup.add(element);

            if (fieldExtractor.apply(element)) {
                // End current group and start new one
                result.add(new ArrayList<>(currentGroup));
                currentGroup.clear();
            }
        }

        // Add remaining elements if any
        if (!currentGroup.isEmpty()) {
            result.add(currentGroup);
        }

        return result;
    }
}
