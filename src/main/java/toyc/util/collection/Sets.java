package toyc.util.collection;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Static utility methods for {@link Set}.
 */
public final class Sets {

    private Sets() {
    }

    // Factory methods for sets and maps
    public static <E> Set<E> newSet() {
        return new HashSet<>();
    }

    public static <E> Set<E> newSet(Collection<? extends E> set) {
        return new HashSet<>(set);
    }

    public static <E> Set<E> newLinkedSet() {
        return new LinkedHashSet<>();
    }

    public static <E> Set<E> newSet(int initialCapacity) {
        if (initialCapacity <= ArraySet.DEFAULT_CAPACITY) {
            return newSmallSet();
        } else {
            return newSet();
        }
    }

    public static <E extends Comparable<E>> TreeSet<E> newOrderedSet() {
        return new TreeSet<>();
    }

    public static <E> TreeSet<E> newOrderedSet(Comparator<? super E> comparator) {
        return new TreeSet<>(comparator);
    }

    public static <E> Set<E> newSmallSet() {
        return new ArraySet<>();
    }

    public static <E> Set<E> newHybridSet() {
        return new HybridHashSet<>();
    }

    public static <E> Set<E> newHybridSet(Collection<E> c) {
        return new HybridHashSet<>(c);
    }

    public static <E> Set<E> newHybridOrderedSet() {
        return new HybridLinkedHashSet<>();
    }

    public static <E> Set<E> newConcurrentSet() {
        return ConcurrentHashMap.newKeySet();
    }
}
