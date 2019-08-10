package koopa.core.util;

import java.util.Iterator;

public class Iterables {

    /**
     * Turns an {@linkplain Iterator} into an {@linkplain Iterable}.
     */
    public static <T> Iterable<T> each(Iterator<T> iterator) {
        return () -> iterator;
    }
}
