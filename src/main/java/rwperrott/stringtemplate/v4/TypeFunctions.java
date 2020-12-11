package rwperrott.stringtemplate.v4;

import it.unimi.dsi.fastutil.objects.Object2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;

import java.util.*;
import lombok.NonNull;

/**
 * Cache a Map of instance and static methods for each valueType, via filtered ClassCaches.
 */
public final class TypeFunctions {

    private TypeFunctions() {
    }

    /**
     * Passed to ClassMembers, to provide a safe way for ClassMembers to inject
     * Type functions into ByName, without making the source values public.
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
        private ByName(final Class<?> valueType) {
            dejaVu.add(valueType);
            this.valueType = valueType;
            ClassMembers.of(valueType).addTo(this);
            // Shouldn't need to do this!
            final Class<?> superType = valueType.getSuperclass();
            if (null != superType) {
                ByName parent = FOR_TYPE.computeIfAbsent(superType, ByName::new);
                final ObjectSet<MemberInvoker> unique = new ObjectOpenHashSet<>();
                mergeInstanceInvokers(unique, parent);
            }
        }

        @Override
        public Class<?> valueType() {
            return valueType;
        }

        @Override
        public final void mergeInstanceInvokers(@NonNull final ObjectSet<MemberInvoker> unique,
                                                @NonNull final Map<String, MemberInvokersImpl> instanceInvokers) {
            instanceInvokers.forEach((name, v) -> {
                final MemberInvokersImpl to =
                        computeIfAbsent(name, MemberInvokersImpl::new);
                final int toSize = to.size();
                if (0 == toSize) {
                    to.ensureCapacity(instanceInvokers.size());
                    to.addAll(v);
                } else {
                    v.stream()
                     .filter(mi -> !unique.contains(mi))
                     .forEach(unique::add);
                    final int newSize = unique.size();
                    if (newSize != toSize) {
                        to.clear();
                        to.ensureCapacity(newSize);
                        to.addAll(unique);
                    }
                    unique.clear();
                }
            });
        }

        @Override
        public void mergeStaticInvokers(final ObjectSet<MemberInvoker> unique,
                                           final Map<String, MemberInvokersImpl> staticInvokers) {
            staticInvokers.forEach((name, v) -> {
                final MemberInvokersImpl to =
                        computeIfAbsent(name, MemberInvokersImpl::new);
                final int toSize = to.size();
                if (0 == toSize) {
                    to.ensureCapacity(staticInvokers.size());
                    v.functionStream(valueType)
                     .forEach(to::add);
                } else {
                    unique.addAll(to);
                    v.functionStream(valueType)
                     .filter(mi -> !unique.contains(mi))
                     .forEach(to::add);
                    final int newSize = unique.size();
                    if (newSize != toSize) {
                        to.clear();
                        to.ensureCapacity(newSize);
                        to.addAll(unique);
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

    private static final Map<Class<?>, ByName> FOR_TYPE = new HashMap<>();

    /**
     * First a any static methods from type class.
     *
     * @param valueType          the main parameter type of the static function methods
     * @param functionClasses the classes containing the static function methods
     */
    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    public static void registerFunctionClasses(Class<?> valueType, Class<?>... functionClasses) {
        Objects.requireNonNull(valueType, "valueType");
        final ByName byName = get(valueType);
        if (null != functionClasses)
            synchronized (byName) {
                for (Class<?> functionClass : functionClasses)
                    byName.register(functionClass);
            }
    }

    private static ByName get(Class<?> valueType) {
        synchronized (FOR_TYPE) {
            return FOR_TYPE.computeIfAbsent(valueType, ByName::new);
        }
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    public static MemberInvokers get(Class<?> valueType, String memberName) {
        final ByName byName = get(valueType);
        synchronized (byName) {
            final MemberInvokersImpl mis= byName.get(memberName);
            if (null == mis)
                return MemberInvokers.NONE;
            mis.sort();
            return mis;
        }
    }
}
