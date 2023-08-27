package rwperrott.stringtemplate.v4;

import lombok.NonNull;

import java.lang.reflect.Array;
import java.util.*;
import java.util.function.IntFunction;

/**
 * Mainly exists to fix stupid lack of support for arrays by ST4!
 * <br/>
 * <code>toSet</code> maybe useful to pass a set of options or flags, rather than bloating the templates list!
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
   * If not a List, tries to convert to a List.
   *
   * @param o an array of objects or primitives.
   * @return a list of objects, the box objects for an array of primitives.
   */
  public static <T> List<T> toList(@NonNull Object o) {
    return toCollection0(o, List.class, n -> 0 == n ? Collections.emptyList() : new ArrayList<>(n), false);
  }

  /**
   * If not a List, tries to convert to a List.
   *
   * @param o an array of objects or primitives.
   * @return a list of objects, the box objects for an array of primitives.
   */
  public static <T extends Comparable<T>> List<T> toSortedList(@NonNull Object o) {
    List<T> copy = toCollection0(o, List.class, n -> 0 == n ? Collections.emptyList() : new ArrayList<>(n), true);
    copy.sort(Comparator.naturalOrder());
    return copy;
  }

  /**
   * If not a Set, returns a HashSet.
   *
   * @param o Collection or array of objects or primitives.
   * @return a hash set of objects; box objects, if an array of primitives.
   */
  public static <T> Set<T> toSet(@NonNull Object o) {
    return toCollection0(o, Set.class, n -> 0 == n ? Collections.emptySet() : new HashSet<>(), false);
  }

  /**
   * If not a Set, returns a LinkedHashSet.
   *
   * @param o Collection or array of objects or primitives.
   * @return an ordered set of objects; box objects, if an array of primitives.
   */
  public static <T> Set<T> toOrderedSet(@NonNull Object o) {
    return toCollection0(o, Set.class, n -> 0 == n ? Collections.emptySet() : new LinkedHashSet<>(), false);
  }

  /**
   * If not a Set, returns a TreeHashSet.
   *
   * @param o Collection or array of objects or primitives.
   * @return a naturalOrder sorted set of objects; box objects, if an array of primitives.
   */
  public static <T extends Comparable<T>> Set<T> toSortedSet(@NonNull Object o) {
    return toCollection0(o, SortedSet.class, n -> 0 == n ? Collections.emptySet() : new TreeSet<>(), false);
  }

  /**
   * Converts Collection or Object/primitive array to a specified collection type, with optional copying.
   *
   * @param o       the source data; any kind of collection, object array, or primitive array.
   * @param type    the base collection type.
   * @param factory creates a new sized collection instance.
   * @param copy    set true when need original data protected from modification side effects.
   */
  @SuppressWarnings("unchecked")
  private static <T, C extends Collection<T>> C toCollection0(Object o, Class<?> type, IntFunction<C> factory, boolean copy) {
    final Class<?> cls = o.getClass();
    if (type.isAssignableFrom(cls) && !copy)
      return (C) o;
    if (o instanceof Collection) {
      final Collection<T> col = (Collection<T>) o;
      final int n = Array.getLength(o);
      final C to = factory.apply(n);
      if (n > 0)
        col.addAll((Collection<T>) o);
      return to;
    }
    if (!cls.isArray())
      throw new UnsupportedOperationException(cls + " is not an Array");
    final int n = Array.getLength(o);
    if (0 == n)
      return factory.apply(0);
    final List<T> list = cls.getComponentType().isPrimitive()
      ? new PrimitiveList<>(o) : Arrays.asList((T[]) o);
    if (type == List.class && !copy)
      return (C) list;
    final C to = factory.apply(n);
    to.addAll(list);
    return to;
  }

  private ObjectFunctions() {
  }
}
