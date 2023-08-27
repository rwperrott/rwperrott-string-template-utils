package rwperrott.stringtemplate.v4;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectRBTreeMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import lombok.NonNull;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import static rwperrott.stringtemplate.v4.TypeConverter.box;

/**
 * Keyed by method/field name
 * <br/>
 * Instances only obtainable for a static cache.
 *
 * @author rwperrott
 */
public final class ClassMembers {
  /*
   * A cache to reduce the cost of lookup; even caches failures.
   */
  private static final Map<Class<?>, ClassMembers> cache = new HashMap<>();

  static {
    // Preload some classes
    Class<?>[] classes = {Class.class, Object.class, String.class, Number.class};
    for (Class<?> cls : classes)
      cache.put(cls, new ClassMembers(cls));
  }

  public static ClassMembers of(final @NonNull Class<?> cls) {
    synchronized (cache) {
      return cache.computeIfAbsent(cls, ClassMembers::new);
    }
  }

  // Used by TypeConverter and TypeIndexMap
  static boolean isAssignableFrom(Class<?> type, Class<?> from) {
    type = box(type); //
    from = box(from);
    if (type == from)
      return true;
    return !Modifier.isFinal(type.getModifiers()) && type.isAssignableFrom(from);
  }

  public final Class<?> cls; // Included to allow detection of duplicates.
  private final Map<String, MemberInvokersImpl> instanceInvokers;
  private final Map<String, MemberInvokersImpl> staticInvokers;

  private ClassMembers(final Class<?> cls) {
    this.cls = cls;
    final MethodHandles.Lookup lookup = MethodHandles.lookup();
    // Collect in name ordered Red Black maps.
    final Map<String, MemberInvokersImpl> staticInvokers0 = new Object2ObjectRBTreeMap<>();
    final Map<String, MemberInvokersImpl> instanceInvokers0 = new Object2ObjectRBTreeMap<>();
    acceptFields(lookup, instanceInvokers0);
    acceptMethods(lookup, staticInvokers0, instanceInvokers0);
    acceptConstructors(lookup, staticInvokers0);

    // Sort and index lists for ordered search sequence, which should help direct use and may speed up indirect use.
    staticInvokers0.values().forEach(MemberInvokersImpl::sort);
    instanceInvokers0.values().forEach(MemberInvokersImpl::sort);

    // Convert maps to linked hash maps, to retain name order, but with faster lookup.
    this.instanceInvokers = new Object2ObjectLinkedOpenHashMap<>(instanceInvokers0);
    this.staticInvokers = new Object2ObjectLinkedOpenHashMap<>(staticInvokers0);
  }

  void addTo(TypeFunctions.FunctionsMap toMap) {
    // Used to block adding of duplicate MemberInvokers.
    final ObjectSet<MemberInvoker> unique = new ObjectOpenHashSet<>();
    if (toMap.valueType() == cls) {
      toMap.mergeInstanceInvokers(unique, instanceInvokers);
    }
    //
    toMap.mergeStaticInvokers(unique, staticInvokers);
  }

  private void acceptFields(final MethodHandles.Lookup lookup,
                            final Map<String, MemberInvokersImpl> instanceInvokers) {
    for (final Field f : cls.getFields()) {
      // Ignore static fields, because irrelevant.
      if (Modifier.isStatic(f.getModifiers()))
        continue;
      //
      final MethodHandle mh;
      try {
        mh = lookup.unreflectGetter(f);
      } catch (IllegalAccessException e) {
        continue;
      }
      instanceInvokers
        .computeIfAbsent(f.getName(), MemberInvokersImpl::new)
        .accept(MemberInvoker.forField(box(f.getType()), f, mh));
    }
  }

  private void acceptMethods(final MethodHandles.Lookup lookup,
                             final Map<String, MemberInvokersImpl> staticInvokers0,
                             final Map<String, MemberInvokersImpl> instanceInvokers0) {
    for (final Method method : cls.getMethods()) {
      try {
        final Class<?> returnType = box(method.getReturnType());
        if (returnType == Void.class)
          continue;
        //
        final Class<?>[] parameterTypes = method.getParameterTypes();
        if (Modifier.isStatic(method.getModifiers())) {
          if (method.getParameterCount() != 0) // Must have at least one method for source type!
            acceptStaticMethod(lookup, staticInvokers0, method, returnType, parameterTypes);
        } else {
          acceptInstanceMethod(lookup, instanceInvokers0, method, returnType, parameterTypes);
        }
      } catch (IllegalAccessException ignore) {
      }
    }
  }

  private void acceptStaticMethod(final MethodHandles.Lookup lookup,
                                  final Map<String, MemberInvokersImpl> staticInvokers0,
                                  final Method method,
                                  final Class<?> returnType,
                                  final Class<?>[] parameterTypes) throws IllegalAccessException {
    final TypeIndexMap valueIndexOf = new TypeIndexMap();
    final TypeConverter[] typeConverters = TypeConverter.toTypeConverters(parameterTypes, valueIndexOf);
    if (null != typeConverters) {
      staticInvokers0
        .computeIfAbsent(method.getName(), MemberInvokersImpl::new)
        .accept(MemberInvoker.forStaticMethod(returnType, method, lookup.unreflect(method), typeConverters, valueIndexOf));
    }
  }

  private void acceptInstanceMethod(final MethodHandles.Lookup lookup,
                                    final Map<String, MemberInvokersImpl> instanceInvokers0,
                                    final Method method,
                                    final Class<?> returnType,
                                    final Class<?>[] parameterTypes) throws IllegalAccessException {
    final TypeConverter[] typeConverters = TypeConverter.toTypeConverters(parameterTypes);
    if (null != typeConverters) {
      // Fix for match all, never convert, bug for equals(Object), which caused erroneous false results.
      if (typeConverters.length == 1 && parameterTypes[0] == Object.class && method.getName().equals("equals"))
        typeConverters[0] = TypeConverter.toTypeConverter(cls);
      instanceInvokers0
        .computeIfAbsent(method.getName(), MemberInvokersImpl::new)
        .accept(MemberInvoker.forMethod(returnType, method, lookup.unreflect(method), typeConverters));
    }
  }

  private void acceptConstructors(final MethodHandles.Lookup lookup,
                                  final Map<String, MemberInvokersImpl> staticInvokers) {
    final String simpleName = cls.getSimpleName();
    for (final Constructor<?> c : cls.getConstructors()) {
      if (0 == c.getParameterCount())
        continue;
      //
      try {
        final TypeIndexMap valueIndexOf = new TypeIndexMap();
        final TypeConverter[] typeConverters = TypeConverter.toTypeConverters(c.getParameterTypes(), valueIndexOf);
        if (null != typeConverters) {
          final MethodHandle mh = lookup.unreflectConstructor(c);
          staticInvokers
            .computeIfAbsent(simpleName, MemberInvokersImpl::new)
            .accept(MemberInvoker.forConstructor(cls, c, mh, typeConverters, valueIndexOf));
        }
      } catch (IllegalAccessException ignore) {
      }
    }

  }
}
