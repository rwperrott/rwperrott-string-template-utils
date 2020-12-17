package rwperrott.stringtemplate.v4;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectRBTreeMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static rwperrott.stringtemplate.v4.TypeConverter.box;

/**
 * Keyed by method/field name
 * <p>
 * Instances can only be obtained for a static cache.
 */
public final class ClassMembers {
    public final Class<?> cls; // Included to that duplicates can be detected
    private final Map<String, MemberInvokersImpl> instanceInvokers;
    private final Map<String, MemberInvokersImpl> staticInvokers;

    void addTo(TypeFunctions.FunctionsMap toMap) {
        // Used to block adding of duplicate MemberInvokers.
        final ObjectSet<MemberInvoker> unique = new ObjectOpenHashSet<>();
        if (toMap.valueType() == cls) {
            toMap.mergeInstanceInvokers(unique, instanceInvokers);
        }
        //
        toMap.mergeStaticInvokers(unique, staticInvokers);
    }

    /*
     * A cache to reduce the cost of subsequent lookups; even failures are cached.
     */
    private static final Map<Class<?>, ClassMembers> cache = new HashMap<>();

    static {
        // Preload some classes
        Class<?>[] classes = {Class.class, Object.class, String.class, Number.class};
        for (Class<?> cls : classes)
            cache.put(cls, new ClassMembers(cls));
    }

    public static ClassMembers of(final Class<?> cls) {
        Objects.requireNonNull(cls, "cls");
        synchronized (cache) {
            return cache.computeIfAbsent(cls, ClassMembers::new);
        }
    }

    private ClassMembers(final Class<?> cls) {
        this.cls = cls;
        final MethodHandles.Lookup lookup = MethodHandles.lookup();
        // Collect in name ordered Red Black maps.
        final Map<String, MemberInvokersImpl> instanceInvokers_ = new Object2ObjectRBTreeMap<>();
        final Map<String, MemberInvokersImpl> staticInvokers_ = new Object2ObjectRBTreeMap<>();
        for (final Field f : cls.getDeclaredFields()) {
            final MethodHandle mh;
            try {
                mh = lookup.unreflectGetter(f);
            } catch (IllegalAccessException e) {
                continue;
            }
            (Modifier.isStatic(f.getModifiers()) ? staticInvokers_ : instanceInvokers_)
                    .computeIfAbsent(f.getName(), MemberInvokersImpl::new)
                    .add(MemberInvoker.forField(box(f.getType()), f, mh));
        }
        //
        for (final Method m : cls.getDeclaredMethods()) {
            try {
                final Class<?> returnType = box(m.getReturnType());
                if (returnType == Void.class)
                    continue;
                //
                final boolean isStatic = Modifier.isStatic(m.getModifiers());
                if (isStatic && 0 == m.getParameterCount())
                    continue;
                //
                final Class<?>[] parameterTypes = m.getParameterTypes();
                if (isStatic) {
                    final TypeIndexMap valueIndexOf = new TypeIndexMap();
                    final TypeConverter[] typeConverters =
                            TypeConverter.toTypeConverters(parameterTypes, valueIndexOf);
                    if (null != typeConverters) {
                        final MethodHandle mh = lookup.unreflect(m);
                        staticInvokers_
                                .computeIfAbsent(m.getName(), MemberInvokersImpl::new)
                                .add(MemberInvoker.forStaticMethod(returnType, m, mh, typeConverters, valueIndexOf));
                    }
                } else {
                    final TypeConverter[] typeConverters =
                            TypeConverter.toTypeConverters(parameterTypes);
                    if (null != typeConverters) {
                        // Fix for match all, never convert, bug for equals(Object), which caused erroneous false results.
                        if (typeConverters.length == 1 && parameterTypes[0] == Object.class && m.getName().equals("equals"))
                            typeConverters[0] = TypeConverter.toTypeConverter(cls);
                        final MethodHandle mh = lookup.unreflect(m);
                        instanceInvokers_
                                .computeIfAbsent(m.getName(), MemberInvokersImpl::new)
                                .add(MemberInvoker.forMethod(returnType, m, mh, typeConverters));
                    }
                }
            } catch (IllegalAccessException ignore) {
            }
        }
        //
        final String simpleName = cls.getSimpleName();
        for (final Constructor<?> c : cls.getDeclaredConstructors()) {
            if (0 == c.getParameterCount())
                continue;
            try {
                final TypeIndexMap valueIndexOf = new TypeIndexMap();
                final TypeConverter[] typeConverters =
                        TypeConverter.toTypeConverters(c.getParameterTypes(), valueIndexOf);
                if (null != typeConverters) {
                    final MethodHandle mh = lookup.unreflectConstructor(c);
                    staticInvokers_
                            .computeIfAbsent(simpleName, MemberInvokersImpl::new)
                            .add(MemberInvoker.forConstructor(cls, c, mh, typeConverters, valueIndexOf));
                }
            } catch (IllegalAccessException ignore) {
            }
        }

        // Sort and index lists for ordered search sequence, which should help direct use and may speed-up indirect use.
        staticInvokers_.values().forEach(MemberInvokersImpl::sort);
        instanceInvokers_.values().forEach(MemberInvokersImpl::sort);

        // Convert maps to linked hash maps, to retain name order, but with faster lookup.
        this.instanceInvokers = new Object2ObjectLinkedOpenHashMap<>(instanceInvokers_);
        this.staticInvokers = new Object2ObjectLinkedOpenHashMap<>(staticInvokers_);
    }

    static boolean isAssignableFrom(Class<?> type, Class<?> from) {
        type = box(type); //
        from = box(from);
        if (type == from)
            return true;
        return !Modifier.isFinal(type.getModifiers()) && type.isAssignableFrom(from);
    }
}
