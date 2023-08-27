package rwperrott.stringtemplate.v4;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import lombok.NonNull;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.ToIntFunction;

import static rwperrott.stringtemplate.v4.TypeConverter.box;

/**
 * Only stores boxed Class keys, and converts key into a box Class v. Tries direct lookup, the instanceOf like lookup.
 *
 * @author rwperrott
 */
final class TypeIndexMap extends Object2IntOpenHashMap<Class<?>> {

  @Override
  public void putAll(final Map<? extends Class<?>, ? extends Integer> m) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int put(final Class<?> aClass, final int v) {
    throw new UnsupportedOperationException();
  }

  private int computeInt0(Class<?> key) {
    key = box(key);
    for (final Entry<Class<?>> e : object2IntEntrySet())
      if (ClassMembers.isAssignableFrom(e.getKey(), key))
        return e.getIntValue(); // Cache hit
    return -1;
  }

  @Override
  public int getInt(@NonNull final Object key) {
    return super.computeIfAbsent(key instanceof Class ? (Class<?>) key : key.getClass(), this::computeInt0);
  }

  @Override
  public int putIfAbsent(final Class<?> aClass, final int v) {
    return super.putIfAbsent(box(aClass), v);
  }

  @SuppressWarnings("deprecation")
  @Override
  public int computeIntIfAbsent(final Class<?> aClass, final ToIntFunction<? super Class<?>> mappingFunction) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int computeIntIfPresent(final Class<?> aClass, final BiFunction<? super Class<?>, ? super Integer, ? extends Integer> remappingFunction) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int computeInt(final Class<?> aClass, final BiFunction<? super Class<?>, ? super Integer, ? extends Integer> remappingFunction) {
    throw new UnsupportedOperationException();
  }
}
