package rwperrott.stringtemplate.v4;

import java.lang.reflect.Array;
import java.util.*;

import static java.util.Comparator.naturalOrder;

/**
 * Mainly exists to fix stupid lack of support for arrays by ST4!
 * <p>
 * Set could be useful to pass a set of options or flags, rather than bloating the templates list!
 *
 * @author rwperrott
 */
@SuppressWarnings("unused")
public class ObjectFunctions {
    public static void registerAdapterFunctions() {
        TypeFunctions.registerFunctionClasses(Object.class,
                                              ObjectFunctions.class);
    }

    /**
     * @param o   an array of objects or primitives
     * @param <T> comparable type
     *
     * @return a naturalOrder sorted list of objects, these will be box objects for an array of primitives
     */
    @SuppressWarnings("unchecked")
    public static <T extends Comparable<T>> List<T> toSortedList(Object o) {
        List<T> list = (List<T>) toList(o);
        list.sort(naturalOrder());
        return list;
    }

    /**
     * @param o an array of objects or primitives
     *
     * @return a list of objects, these will be box objects for an array of primitives
     */
    public static List<?> toList(Object o) {
        final Class<?> cls = o.getClass();
        if (!cls.isArray())
            throw new UnsupportedOperationException(cls + " is not an Array");
        final int n = Array.getLength(o);
        final List<Object> list = new ArrayList<>(n);
        for (int i = 0; i < n; i++)
            list.add(Array.get(o, i));
        return list;
    }

    /**
     * @param o an array of objects or primitives
     *
     * @return an ordered set of objects, these will be box objects for an array of primitives
     */
    public static Set<?> toSet(Object o) {
        if (o instanceof List)
            return new LinkedHashSet<>((List<?>) o);
        return new LinkedHashSet<>(toList(o));
    }

    /**
     * @param o an array of objects or primitives
     *
     * @return a naturalOrder sorted set of objects, these will be box objects for an array of primitives
     */
    public static Set<?> toSortedSet(Object o) {
        if (o instanceof List)
            return new TreeSet<>((List<?>) o);
        return new TreeSet<>(toList(o));
    }
}
