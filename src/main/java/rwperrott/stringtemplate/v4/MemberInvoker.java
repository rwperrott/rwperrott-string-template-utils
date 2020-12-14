package rwperrott.stringtemplate.v4;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Handle both Class and Object members, for StringArgs use.
 * <p>
 * Supports all Fields, but only supports Methods with parameter types supported by ArgTypes.
 * <p>
 * Only compares parameters, because should already be group by name, return type should already have been filtered
 * out.
 */
public abstract class MemberInvoker implements Comparable<MemberInvoker> {

    private final int h;

    @SuppressWarnings("unused")
    protected MemberInvoker(final TypeConverter[] typeConverters) {
        this.h = Arrays.hashCode(typeConverters());
    }

    /**
     * Hidden, so don't have to copy it, used by compareTo
     */
    protected abstract TypeConverter[] typeConverters();

    public abstract boolean isReturnTypeInstanceOf(Class<?> type);

    public abstract boolean isAccessible(boolean onlyPublic);

    @SuppressWarnings("unused")
    abstract boolean isStatic();

    public boolean convert(final List<Object> args, int extrasLen) {
        return TypeConverter.convert(args, typeConverters(), extrasLen);
    }

    @Override
    public final int hashCode() {
        return h;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof MemberInvoker))
            return false;
        final MemberInvoker that = (MemberInvoker) o;
        return Arrays.equals(typeConverters(), that.typeConverters());
    }

    public final int compareTo(MemberInvoker other) {
        final TypeConverter[] t1 = typeConverters();
        final TypeConverter[] t2 = other.typeConverters();
        if (t1 == t2)
            return 0; // Fast exit for Fields and zero non-v parameter methods.
        int r = Integer.compare(t1.length, t2.length);
        if (r != 0)
            return r;
        for (int i = 0, n = t1.length; i < n; i++) {
            r = t1[i].compareTo(t2[i]);
            if (r != 0)
                return r;
        }
        return 0;
    }

    public abstract int typeConverterCount();

    public abstract Object invoke(final Object value, final List<Object> args) throws Throwable;

    private static abstract class Abstract<M extends Member> extends MemberInvoker {
        /**
         * Metadata unreflected to MethodHandle.
         */
        protected final M member;
        /**
         * Much cheaper than invoking a Method or Field
         */
        protected final MethodHandle methodHandle;
        /**
         * Boxed type, to simplify matching returnType
         */
        private final Class<?> boxedReturnType;

        protected Abstract(final Class<?> boxedReturnType,
                           final M member,
                           final MethodHandle methodHandle,
                           final TypeConverter[] typeConverters) {
            super(typeConverters);
            this.boxedReturnType = boxedReturnType;
            this.member = requireNonNull(member, "field");
            this.methodHandle = requireNonNull(methodHandle, "methodHandle");
        }

        public final boolean isReturnTypeInstanceOf(Class<?> type) {
            return type == boxedReturnType || type.isAssignableFrom(boxedReturnType);
        }

        public final boolean isAccessible(boolean onlyPublic) {
            return !onlyPublic || Modifier.isPublic(member.getModifiers());
        }

        public final boolean isStatic() {
            return Modifier.isStatic(member.getModifiers());
        }

        @Override
        public String toString() {
            return member.toString();
        }
    }

    static final class ForField extends Abstract<Field> {

        private ForField(final Class<?> boxedReturnType,
                         final Field member,
                         final MethodHandle methodHandle) {
            super(boxedReturnType, member, methodHandle, TypeConverter.NONE);
        }

        @Override
        public TypeConverter[] typeConverters() {
            return TypeConverter.NONE;
        }

        @Override
        public int typeConverterCount() {
            return 0;
        }

        public Object invoke(final Object value, List<Object> args) throws Throwable {
            return isStatic()
                   ? methodHandle.invokeExact()
                   : methodHandle.bindTo(value).invokeExact();
        }
    }

    static class ForMethod extends Abstract<Method> {
        /**
         * Of parameterTypes convertable from String, used for keying and matching.
         */
        protected final TypeConverter[] typeConverters;

        private ForMethod(final Class<?> boxedReturnType,
                          final Method method,
                          final MethodHandle methodHandle,
                          final TypeConverter[] typeConverters) {
            super(boxedReturnType, method, methodHandle, typeConverters);
            this.typeConverters = requireNonNull(typeConverters, "typeAdapters");
        }

        @Override
        public final TypeConverter[] typeConverters() {
            return typeConverters;
        }

        @Override
        public final int typeConverterCount() {
            return typeConverters.length;
        }

        public final Object invoke(final Object value, final List<Object> srcArgs) throws Throwable {
            MethodHandle mh = methodHandle;
            if (!isStatic())
                mh = mh.bindTo(value);
            //
            final int argsLength = typeConverters.length;
            if (argsLength == 0)
                return mh.invoke();
            final Object[] args = new Object[argsLength];
            for (int i = 0; i < argsLength; i++)
                args[i] = srcArgs.get(i);
            return mh.invokeWithArguments(args);
        }
    }

    static class ForStaticMethod extends ForMethod {
        final TypeIndexMap valueIndexOf;

        private ForStaticMethod(final Class<?> boxedReturnType,
                                final Method method,
                                final MethodHandle methodHandle,
                                final TypeConverter[] typeAdapters,
                                final TypeIndexMap valueIndexOf) {
            super(boxedReturnType, method, methodHandle, typeAdapters);
            this.valueIndexOf = requireNonNull(valueIndexOf, "typeIndexOf");
        }

        ForValueType forValueType(final Class<?> cls) {
            if (null == valueIndexOf)
                return null;
            final int valueIndex = valueIndexOf.getInt(cls);
            if (-1 == valueIndex)
                return null;
            //
            final TypeConverter[] from = ForStaticMethod.this.typeConverters;
            final int fromN = from.length;
            final TypeConverter[] to = new TypeConverter[fromN - 1];
            for (int fromI = 0, toI = 0; fromI < fromN; fromI++) {
                if (fromI == valueIndex)
                    continue;
                to[toI++] = from[fromI];
            }
            return new ForValueType(valueIndex, to);
        }

        @Override
        public String toString() {
            return "StaticMethod{" +
                   "member=" + member +
                   "valueIndexOf=" + valueIndexOf +
                   '}';
        }

        /**
         * used by ForValueType::invoke
         */
        protected final Object invoke(final int valueIndex, final Object value, List<Object> srcArgs) throws Throwable {
            final int argsLength = typeConverters.length;
            final Object[] args = new Object[argsLength];
            for (int i = 0, j = 0; i < argsLength; i++)
                args[i] = (i == valueIndex) ? value : srcArgs.get(j++);
            return methodHandle.invokeWithArguments(args);
        }

        public final class ForValueType extends MemberInvoker {
            final int valueIndex;
            final TypeConverter[] otherTypeConverters;

            private ForValueType(final int valueIndex, final TypeConverter[] otherTypeConverters) {
                super(otherTypeConverters);
                this.valueIndex = valueIndex;
                this.otherTypeConverters = otherTypeConverters;
            }

            @Override
            public boolean isReturnTypeInstanceOf(final Class<?> type) {
                return ForStaticMethod.this.isReturnTypeInstanceOf(type);
            }

            @Override
            public boolean isAccessible(final boolean onlyPublic) {
                return ForStaticMethod.this.isAccessible(onlyPublic);
            }

            @Override
            public boolean isStatic() {
                return ForStaticMethod.this.isStatic();
            }

            @Override
            protected TypeConverter[] typeConverters() {
                return otherTypeConverters;
            }

            @Override
            public int typeConverterCount() {
                return otherTypeConverters.length;
            }

            public final Object invoke(final Object value, List<Object> args) throws Throwable {
                return ForStaticMethod.this.invoke(valueIndex, value, args);
            }

            @Override
            public String toString() {
                return "StaticMethod.ForValueType{" +
                       "member=" + member +
                       "valueIndex=" + valueIndex +
                       '}';
            }
        }
    }

    static MemberInvoker forField(final Class<?> boxedReturnType,
                                  final Field member,
                                  final MethodHandle methodHandle) {
        return new ForField(boxedReturnType, member, methodHandle);
    }

    static MemberInvoker forMethod(final Class<?> boxedReturnType,
                                   final Method member,
                                   final MethodHandle methodHandle,
                                   final TypeConverter[] typeAdapters) {
        return new ForMethod(boxedReturnType, member, methodHandle, typeAdapters);
    }

    static MemberInvoker forStaticMethod(final Class<?> boxedReturnType,
                                         final Method member,
                                         final MethodHandle methodHandle,
                                         final TypeConverter[] typeAdapters,
                                         final TypeIndexMap valueIndexOf) {
        return new ForStaticMethod(boxedReturnType, member, methodHandle, typeAdapters, valueIndexOf);
    }
}
