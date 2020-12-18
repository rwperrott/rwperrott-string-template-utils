package rwperrott.stringtemplate.v4;

import lombok.NonNull;
import org.stringtemplate.v4.Interpreter;
import org.stringtemplate.v4.ModelAdaptor;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.misc.STNoSuchPropertyException;

import java.util.*;
import java.util.concurrent.locks.StampedLock;
import java.util.function.UnaryOperator;

/**
 * This provides most/all the functionality to create powerful ModelAdapters which can use an objects fields
 * and make parameterised use of it's instance/static methods, plus static methods in any other registered class.
 * <p>
 * Uses unreflected and cached MethodHandles to invoke Field getters and Methods, which is a lot faster than the
 * redundant Security costs of calling invoke on Field or Method Member objects.
 *
 * @param <T> type to be adapted
 */
public class AbstractInvokeAdaptor<T> implements ModelAdaptor<T> {
    private final boolean onlyPublic;

    protected AbstractInvokeAdaptor(final boolean onlyPublic) {
        this.onlyPublic = onlyPublic;
    }

    @Override
    public final Object getProperty(Interpreter interp, ST self, T model, Object property, String propertyName)
            throws STNoSuchPropertyException {
        if (model == null)
            throw new NullPointerException("o");

        final Class<?> cls = model.getClass();
        if (property == null)
            throw STExceptions.noSuchPropertyInClass(cls, propertyName, null);

        propertyName = toAlias(propertyName);

        try {
            final MemberInvokers mis = TypeFunctions.get(cls, propertyName);
            if (mis.maxTypeConverterCount() == 0) {
                final List<Object> args = Collections.emptyList();
                final MemberInvoker invoker = mis.find(onlyPublic, Object.class, args);
                if (null == invoker) {
                    TypeFunctions.get(cls, propertyName);
                    throw new IllegalArgumentException("No matching field, method, or static method found");
                }
                return invoker.invoke(model, args);
            }

            final ArgsAdaptor r = new ArgsAdaptor(interp, self, model, propertyName, onlyPublic, Object.class, mis);

            // Ensure that COMPOSITE_ADAPTER is registered to handle ArgsAdaptor objects.
            final STGroup stg = self.groupThatCreatedThisInstance;
            if (COMPOSITE_ADAPTER != stg.getModelAdaptor(ArgsAdaptor.class))
                stg.registerModelAdaptor(ArgsAdaptor.class, COMPOSITE_ADAPTER);

            return r;
        } catch (Throwable t) {
            throw STExceptions.noSuchPropertyInObject(model, propertyName, t);
        }
    }

    protected String toAlias(String name) {
        return name;
    }

    /**
     * Only created if memberInvokers.maxTypeConverterCount() more than zero.
     * <p>
     * Carries Interpreter and ST, so can resolve excess properties, via requested ModelAdapter.
     */
    private static final class ArgsAdaptor implements UnaryOperator<Object> {
        private final Interpreter interp;
        private final ST self;
        private final boolean onlyPublic;
        private final Class<?> returnType;
        private final Object value;
        private final String propertyName;
        private final MemberInvokers memberInvokers;
        // Lock for args and latestMatchingInvoker, especially against debugger!
        private final StampedLock debugLock = new StampedLock();
        private final List<Object> args = new ArrayList<>();
        /**
         * Saved, so can be invoke last
         */
        private MemberInvoker latestMatchingInvoker;
        private String string;

        private ArgsAdaptor(final Interpreter interp,
                            final ST self,
                            final Object value,
                            final String propertyName,
                            final boolean onlyPublic,
                            final Class<?> returnType,
                            final MemberInvokers memberInvokers) {
            this.interp = interp;
            this.self = self;
            this.onlyPublic = onlyPublic;
            this.returnType = returnType;
            this.value = value;
            this.propertyName = propertyName;
            this.memberInvokers = memberInvokers;

            // Allow for 0..n args methods
            final MemberInvoker mi = memberInvokers.find(onlyPublic, returnType, args);
            if (null != mi)
                latestMatchingInvoker = mi; // The latest best match
        }

        @Override
        public Object apply(final @NonNull Object o) {
            // Protect args and latestMatchingInvoker.
            long stamp = debugLock.writeLock();
            try {
                args.add(o);
                final MemberInvoker mi = memberInvokers.find(onlyPublic, returnType, args);
                if (null != mi)
                    latestMatchingInvoker = mi;
                return args.size() < memberInvokers.maxTypeConverterCount()
                       ? this
                       : invoke(); // No matches after this, so use latestMatchingInvoker
            } finally {
                debugLock.unlockWrite(stamp);
            }
        }

        // Called by apply or toString.
        @SuppressWarnings({"rawtypes", "unchecked"})
        private Object invoke() {
            final long stamp = debugLock.readLock();
            try {
                int i = 0, n = args.size();
                if (null == latestMatchingInvoker)
                    throw STExceptions.noSuchPropertyInObject(value,join(propertyName, args, i, n),
                                                              new IllegalArgumentException("No matching member found"));
                //
                Object result = null;
                try {
                    // Some of the properties found a longest match method, so can resolve model and some properties to an object result.
                    result = latestMatchingInvoker.invoke(value, args);

                    // For each excess property:
                    //    call getModelAdapter and getProperty, to part/fully resolving property.
                    // Part resolved properties can be ArgsAdaptor instances.
                    final STGroup stg = self.groupThatCreatedThisInstance;
                    i = latestMatchingInvoker.typeConverterCount();
                    while (i < n) {
                        final Object arg = args.get(i++);
                        final ModelAdaptor ma = stg.getModelAdaptor(arg.getClass()); // Assume never can be null
                        result = ma.getProperty(interp, self, result, arg, arg.toString());
                    }
                    return result;
                } catch (Throwable t) { // Catch failure of latestMatchingInvoker.invoke or later failure.
                    throw STExceptions.noSuchPropertyInObject(value, join(result, args, i, n), t);
                }
            } finally {
                debugLock.unlockRead(stamp);
            }
        }

        private static String join(final Object result,
                                   final List<Object> args, int i, int n) {
            final StringJoiner sj = new StringJoiner(".");
            add(sj, result);
            while (i < n)
                add(sj, args.get(i++));
            return sj.toString();
        }

        // Used to build noSuchPropertyInObject property String
        private static void add(StringJoiner sj, Object o) {
            if (o instanceof CharSequence)
                // Add ("string")
                sj.add(String.format("(\"%s\")", o));
            else
                // Add {type:"string"}
                sj.add(String.format("{%s:\"%s\"}", o.getClass().getSimpleName(), o));
        }

        /**
         * Must call invoke, in case no more properties after last one.
         */
        public String toString() {
            long stamp = debugLock.tryReadLock();
            if (0 == stamp)
                return "locked:" + string; // Only for debug to see a stale value, to prevent prematurely invoke call.
            try {
                return string = invoke().toString();
            } finally {
                debugLock.unlockRead(stamp);
            }
        }
    }

    /*
     * model is a ArgsAdaptor, a UnaryOperator.
     * apply returns the same model object, or the final Object result.
     */
    private static final ModelAdaptor<ArgsAdaptor> COMPOSITE_ADAPTER =
            (interp, self, model, property, propertyName) -> model.apply(property);
}
