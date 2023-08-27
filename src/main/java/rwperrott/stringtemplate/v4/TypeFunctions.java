package rwperrott.stringtemplate.v4;

import it.unimi.dsi.fastutil.objects.Object2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import lombok.NonNull;

import java.util.*;

/**
 * Cache a Map of instance and static methods for each valueType, via filtered ClassCaches.
 *
 * @author rwperrott
 */
public final class TypeFunctions {

  private static final Map<Class<?>, ByName> FOR_TYPE = new HashMap<>();

  /**
   * @param valueType       the main parameter type for the static function methods
   * @param functionClasses the classes containing the static function methods
   */
  @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
  public static void registerFunctionClasses(final @NonNull Class<?> valueType, Class<?>... functionClasses) {
    final ByName byName = get(valueType);
    if (null != functionClasses)
      synchronized (byName) {
        for (Class<?> functionClass : functionClasses)
          byName.register(functionClass);
      }
  }

  private static ByName get0(Class<?> valueType) {
    // Have to split get and put to avoid ConcurrentModificationException.
    ByName byName = FOR_TYPE.get(valueType);
    if (null != byName)
      return byName;
    ByName superInstance = null;
    final Class<?> superType = valueType.getSuperclass();
    if (null != superType) {
      superInstance = get0(superType);
    }
    byName = new ByName(valueType, superInstance);
    FOR_TYPE.put(valueType, byName);
    return byName;
  }

  private static ByName get(Class<?> valueType) {
    synchronized (FOR_TYPE) {
      return get0(valueType);
    }
  }

  @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
  public static MemberInvokers get(Class<?> valueType, String memberName) {
    final ByName byName = get(valueType);
    synchronized (byName) {
      final MemberInvokersImpl mis = byName.get(memberName);
      if (null == mis)
        return MemberInvokers.NONE;
      mis.sort();
      return mis;
    }
  }

  private TypeFunctions() {
  }

  /**
   * Passed to ClassMembers, to provide a safe way for ClassMembers to inject Type functions into ByName, without
   * making the source values public.
   */
  interface FunctionsMap {
    Class<?> valueType();

    void mergeInstanceInvokers(final ObjectSet<MemberInvoker> unique,
                               final Map<String, MemberInvokersImpl> instanceInvokers);

    void mergeStaticInvokers(final ObjectSet<MemberInvoker> unique,
                             final Map<String, MemberInvokersImpl> staticInvokers);
  }

  private static class ByName extends Object2ObjectAVLTreeMap<String, MemberInvokersImpl>
    implements FunctionsMap {
    private final Set<Class<?>> dejaVu = Collections.synchronizedSet(new HashSet<>());

    private final Class<?> valueType;

    @SuppressWarnings("LeakingThisInConstructor")
    private ByName(final Class<?> valueType, final ByName superInstance) {
      dejaVu.add(valueType);
      this.valueType = valueType;
      ClassMembers.of(valueType).addTo(this);
      if (null != superInstance) {
        mergeInstanceInvokers(new ObjectOpenHashSet<>(), superInstance);
      }
    }

    @Override
    public Class<?> valueType() {
      return valueType;
    }

    /**
     * Only public to allow injection by ClassMembers method.
     *
     * @param unique           a temporary Set to used to stop overwrite of existing entries in the internal map.
     * @param instanceInvokers the map to scan
     */
    @Override
    public final void mergeInstanceInvokers(@NonNull final ObjectSet<MemberInvoker> unique,
                                            @NonNull final Map<String, MemberInvokersImpl> instanceInvokers) {
      instanceInvokers.forEach((name, v) -> {
        final MemberInvokersImpl to = computeIfAbsent(name, MemberInvokersImpl::new);
        final int toSize = to.size();
        if (0 == toSize) {
          to.ensureCapacity(instanceInvokers.size());
          v.forEach(to);
        } else {
          v.stream()
            .filter(mi -> !unique.contains(mi))
            .forEach(unique::add);
          final int newSize = unique.size();
          if (newSize != toSize) {
            to.clear();
            to.ensureCapacity(newSize);
            unique.forEach(to);
          }
          unique.clear();
        }
      });
    }

    /**
     * Only public to allow injection by ClassMembers method.
     *
     * @param unique         a temporary Set to used to stop overwrite of existing entries in the internal map.
     * @param staticInvokers the map to scan
     */
    @Override
    public void mergeStaticInvokers(final ObjectSet<MemberInvoker> unique,
                                    final Map<String, MemberInvokersImpl> staticInvokers) {
      staticInvokers.forEach((name, v) -> {
        final MemberInvokersImpl to =
          computeIfAbsent(name, MemberInvokersImpl::new);
        final int toSize = to.size();
        if (0 == toSize) {
          to.ensureCapacity(staticInvokers.size());
          v.functionStream(valueType).forEach(to);
        } else {
          to.forEach(unique::add);
          v.functionStream(valueType)
            .filter(mi -> !unique.contains(mi))
            .forEach(to);
          final int newSize = unique.size();
          if (newSize != toSize) {
            to.clear();
            to.ensureCapacity(newSize);
            unique.forEach(to);
          }
          unique.clear();
        }
      });
    }

    private void register(final Class<?> functionClass) {
      if (null != functionClass && dejaVu.add(functionClass))
        ClassMembers.of(functionClass).addTo(this);
    }
  }
}
