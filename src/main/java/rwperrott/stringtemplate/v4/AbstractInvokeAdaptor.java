package rwperrott.stringtemplate.v4;

import org.stringtemplate.v4.Interpreter;
import org.stringtemplate.v4.ModelAdaptor;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.misc.STNoSuchPropertyException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;
import java.util.function.UnaryOperator;

/**
 * This provides most/all the functionality to create fra more powerful ModelAdapters which can use an objects fields
 * and make parameterised use of it's instance/static methods and static methods of other registered class.
 * <p>
 * Uses unreflected and cached HethodHandles to invoke Field getters and Methods, which is a lot faster than the
 * redundant Security costs of calling invoke on Field or Method objects.
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

            final Composite r = new Composite(interp, self, model, propertyName, onlyPublic, Object.class, mis);

            // Ensure that COMPOSITE_ADAPTER is registered to handle Composite objects.
            final STGroup stg = self.groupThatCreatedThisInstance;
            if (COMPOSITE_ADAPTER != stg.getModelAdaptor(Composite.class))
                stg.registerModelAdaptor(Composite.class, COMPOSITE_ADAPTER);

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
     * Caries Interpreter and ST, so can resolve excess properties.
     */
    private static final class Composite implements UnaryOperator<Object> {
        private final Interpreter interp;
        private final ST self;
        private final boolean onlyPublic;
        private final Class<?> returnType;
        private final Object value;
        private final String propertyName;
        private final MemberInvokers memberInvokers;
        private final List<Object> args = new ArrayList<>();
        private MemberInvoker invoker;

        public Composite(final Interpreter interp,
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
            final MemberInvoker test = memberInvokers.find(onlyPublic, returnType, args);
            if (null != test)
                invoker = test;
        }

        @Override
        public Object apply(final Object o) {
            args.add(o);
            MemberInvoker test = memberInvokers.find(onlyPublic, returnType, args);
            if (null != test)
                invoker = test; // The latest best match
            return args.size() < memberInvokers.maxTypeConverterCount()
                   ? this
                   : invoke();
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        Object invoke() {
            Object r = null;
            int i = 0;
            final int n = args.size();
            try {
                if (null == invoker)
                    throw new IllegalArgumentException("No suitable field, method, or static method found");
                r = invoker.invoke(value, args);
                // Resolve each excess property, using the appropriate model adapter.
                final STGroup stg = self.groupThatCreatedThisInstance;
                i = invoker.typeConverterCount();
                while (i < n) {
                    final Object arg = args.get(i++);
                    final ModelAdaptor ma = stg.getModelAdaptor(arg.getClass());
                    if (null == ma)
                        throw new IllegalStateException("No ModelAdapter for " + arg.getClass().getTypeName());
                    r = ma.getProperty(interp, self, r, arg, arg.toString());
                }
                return r;
            } catch (Throwable t) {
                // Must use a copy of args to avoid damage by toString(), during debug!
                final StringJoiner sj = new StringJoiner(".");
                if (i == 0) {
                    add(sj, propertyName);
                    args.forEach(arg -> add(sj, arg));
                } else {
                    add(sj, r);
                    args.subList(i, n).forEach(arg -> add(sj, arg));
                }
                throw STExceptions.noSuchPropertyInObject(value, sj.toString(), t);
            }
        }

        private static void add(StringJoiner sj, Object o) {
            if (o instanceof CharSequence)
                sj.add(o.toString());
            else
                sj.add(String.format("%s{%s}", o.getClass().getSimpleName(), o));
        }

        /**
         * Must call invoke, in case no more properties after last one.
         */
        public String toString() {
            return invoke().toString();
        }
    }

    private static final ModelAdaptor<Composite> COMPOSITE_ADAPTER =
            (interp, self, model, property, propertyName) -> model.apply(property);
}
