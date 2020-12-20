package rwperrott.stringtemplate.v4;

import lombok.NonNull;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.*;
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
public interface MemberInvoker extends Comparable<MemberInvoker> {
    TypeConverter[] typeConverters();

    boolean isReturnTypeInstanceOf(Class<?> type);

    boolean isAccessible(boolean onlyPublic);

    @SuppressWarnings("unused")
    boolean isStatic();

    default boolean convert(final List<Object> args, int extrasLen) {
        return TypeConverter.convert(args, typeConverters(), extrasLen);
    }

    int typeConverterCount();

    Object invoke(final Object value, final List<Object> args) throws Throwable;

    abstract class Abstract<M extends Member> implements MemberInvoker {

        private final int h;
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
                           final @NonNull M member,
                           final @NonNull MethodHandle methodHandle,
                           final @NonNull TypeConverter[] typeConverters) {
            this.h = Arrays.hashCode(typeConverters);
            this.boxedReturnType = boxedReturnType;
            this.member = member;
            this.methodHandle = methodHandle;
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

        public final boolean convert(final List<Object> args, int extrasLen) {
            return TypeConverter.convert(args, typeConverters(), extrasLen);
        }

        /**
         * used by ForValueType::invoke
         */
        final Object invoke(final int valueIndex, final Object value, List<Object> srcArgs) throws Throwable {
            final int argsLength = typeConverters().length;
            final Object[] args = new Object[argsLength];
            for (int i = 0, j = 0; i < argsLength; i++)
                args[i] = (i == valueIndex) ? value : srcArgs.get(j++);
            return methodHandle.invokeWithArguments(args);
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

        @Override
        public String toString() {
            return member.toString();
        }
    }

    abstract class AbstractForValueType<M extends Member> implements MemberInvoker {
        private final int h;

        final Abstract<M> parent;
        final int valueIndex;
        final TypeConverter[] typeConverters;

        public AbstractForValueType(final Abstract<M> parent, final int valueIndex, final TypeConverter[] typeConverters) {
            this.h = Arrays.hashCode(typeConverters);
            this.parent = parent;
            this.valueIndex = valueIndex;
            this.typeConverters = typeConverters;
        }

        @Override
        public final TypeConverter[] typeConverters() {
            return typeConverters;
        }

        @Override
        public final boolean isReturnTypeInstanceOf(final Class<?> type) {
            return parent.isReturnTypeInstanceOf(type);
        }

        @Override
        public final boolean isAccessible(final boolean onlyPublic) {
            return parent.isAccessible(onlyPublic);
        }

        @SuppressWarnings("unused")
        @Override
        public final boolean isStatic() {
            return parent.isStatic();
        }

        @Override
        public final boolean convert(final List<Object> args, final int extrasLen) {
            return TypeConverter.convert(args, typeConverters(), extrasLen);
        }

        @Override
        public final int typeConverterCount() {
            return typeConverters.length;
        }

        @Override
        public final Object invoke(final Object value, final List<Object> args) throws Throwable {
            return parent.invoke(valueIndex, value, args);
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
    }

    final class ForField extends Abstract<Field> {

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


        @Override
        public String toString() {
            ToStringBuilder t = new ToStringBuilder("MemberInvoker.ForField",true);
            t.add("field", member);
            t.complete();
            return t.toString();
        }
    }

    class ForMethod extends Abstract<Method> {
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

        @Override
        public String toString() {
            ToStringBuilder t = new ToStringBuilder("MemberInvoker.ForMethod",true);
            t.add("method", member);
            t.add("typeConverters", typeConverters);
            t.complete();
            return t.toString();
        }
    }

    interface WithValueType extends MemberInvoker {
        MemberInvoker forValueType(final Class<?> cls);
    }

    class ForStaticMethod extends ForMethod implements WithValueType {
        final TypeIndexMap valueIndexOf;

        private ForStaticMethod(final Class<?> boxedReturnType,
                                final Method method,
                                final MethodHandle methodHandle,
                                final TypeConverter[] typeAdapters,
                                final TypeIndexMap valueIndexOf) {
            super(boxedReturnType, method, methodHandle, typeAdapters);
            this.valueIndexOf = requireNonNull(valueIndexOf, "typeIndexOf");
        }

        public MemberInvoker forValueType(final Class<?> cls) {
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
            return new ForValueType(this, valueIndex, to);
        }

        @Override
        public String toString() {
            ToStringBuilder t = new ToStringBuilder("MemberInvoker.ForStaticMethod",true);
            t.add("method", member);
            t.add("valueIndexOf",valueIndexOf);
            t.complete();
            return t.toString();
        }

        public static final class ForValueType extends AbstractForValueType<Method> {
            private ForValueType(final ForStaticMethod parent,
                                 final int valueIndex,
                                 final TypeConverter[] typeConverters) {
                super(parent, valueIndex, typeConverters);
            }

            @Override
            public String toString() {
                ToStringBuilder t = new ToStringBuilder("MemberInvoker.ForStaticMethod.ForValueType",true);
                t.add("parent", parent);
                t.add("valueIndex",valueIndex);
                t.add("typeConverters",typeConverters);
                t.complete();
                return t.toString();
            }
        }
    }

    class ForConstructor extends Abstract<Constructor<?>> implements WithValueType {
        /**
         * Of parameterTypes convertable from String, used for keying and matching.
         */
        protected final TypeConverter[] typeConverters;
        final TypeIndexMap valueIndexOf;

        private ForConstructor(final Class<?> cls,
                                final Constructor<?> constructor,
                                final MethodHandle methodHandle,
                                final TypeConverter[] typeConverters,
                                final TypeIndexMap valueIndexOf) {
            super(cls, constructor, methodHandle, typeConverters);
            this.typeConverters = requireNonNull(typeConverters, "typeAdapters");
            this.valueIndexOf = requireNonNull(valueIndexOf, "typeIndexOf");
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

        public MemberInvoker forValueType(final Class<?> cls) {
            if (null == valueIndexOf)
                return null;
            final int valueIndex = valueIndexOf.getInt(cls);
            if (-1 == valueIndex)
                return null;
            //
            final TypeConverter[] from = ForConstructor.this.typeConverters;
            final int fromN = from.length;
            final TypeConverter[] to = new TypeConverter[fromN - 1];
            for (int fromI = 0, toI = 0; fromI < fromN; fromI++) {
                if (fromI == valueIndex)
                    continue;
                to[toI++] = from[fromI];
            }
            return new ForValueType(this, valueIndex, to);
        }

        @Override
        public String toString() {
            ToStringBuilder t = new ToStringBuilder("MemberInvoker.ForConstructor",true);
            t.add("constructor", member);
            t.add("valueIndexOf",valueIndexOf);
            t.complete();
            return t.toString();
        }

        public static final class ForValueType extends AbstractForValueType<Constructor<?>> {
            private ForValueType(final ForConstructor parent,
                                 final int valueIndex,
                                 final TypeConverter[] typeConverters) {
                super(parent, valueIndex, typeConverters);
            }

            @Override
            public String toString() {
                ToStringBuilder t = new ToStringBuilder("MemberInvoker.ForConstructor.ForValueType",true);
                t.add("parent", parent);
                t.add("valueIndex",valueIndex);
                t.add("typeConverters",typeConverters);
                t.complete();
                return t.toString();
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

    static MemberInvoker forConstructor(final Class<?> boxedReturnType,
                                         final Constructor<?> member,
                                         final MethodHandle methodHandle,
                                         final TypeConverter[] typeAdapters,
                                         final TypeIndexMap valueIndexOf) {
        return new ForConstructor(boxedReturnType, member, methodHandle, typeAdapters, valueIndexOf);
    }
}
