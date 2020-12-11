package rwperrott.stringtemplate.v4;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.ToIntFunction;
import lombok.NonNull;

import static rwperrott.stringtemplate.v4.TypeConverter.box;

/**
 * Only stores boxed Class keys, and converts key into a box Class v.
 * Tries direct lookup, the instanceOf like lookup.
 */
final class TypeIndexMap extends Object2IntOpenHashMap<Class<?>> {

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

    @Override
    public void putAll(final Map<? extends Class<?>, ? extends Integer> m) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int put(final Class<?> aClass, final int v) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int putIfAbsent(final Class<?> aClass, final int v) {
        return super.putIfAbsent(box(aClass), v);
    }

    @Override
    public int getInt(@NonNull final Object key) {
        final Class<?> aClass = (key instanceof Class) ? (Class<?>)key : key.getClass();
        return super.computeIntIfAbsent(aClass, from-> {
            from = box(from);
            for (final Entry<Class<?>> e : object2IntEntrySet())
                if (ClassMembers.isAssignableFrom(e.getKey(), from))
                    return e.getIntValue(); // Cache hit
            return -1;
        });
    }
}
