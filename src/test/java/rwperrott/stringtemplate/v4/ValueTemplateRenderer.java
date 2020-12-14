package rwperrott.stringtemplate.v4;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STErrorListener;
import org.stringtemplate.v4.STGroupString;
import org.stringtemplate.v4.compiler.STException;
import org.stringtemplate.v4.misc.STMessage;

import java.util.*;

import static java.lang.String.format;

/**
 * Useful for rendering simple templates based on a "v" attribute
 *
 * @param <R>
 */
@SuppressWarnings("unused")
class ValueTemplateRenderer<R extends ValueTemplateRenderer<R>> implements STErrorListener {
    private final String sourceName;
    private final List<String> properties = new ArrayList<>();
    private final List<String> wrappers = new ArrayList<>();
    protected String template;
    protected final Map<String, Object> attributes = new HashMap<>();
    protected STGroupString stg;
    protected ST st;
    private boolean failed;

    public ValueTemplateRenderer(final String sourceName) {
        this.sourceName = sourceName;
    }

    public final void clear() {
        properties.clear();
        wrappers.clear();
        template = null;
        attributes.clear();
        stg = null;
        st = null;
        failed = false;
    }

    public final R w(final String wrapper) {
        wrappers.add(Objects.requireNonNull(wrapper));
        return r();
    }

    @SuppressWarnings("unchecked")
    protected R r() {
        return (R) this;
    }

    public final String template() {
        return template;
    }

    public final STGroupString stg() {
        return stg;
    }

    public R p(String property) {
        Objects.requireNonNull(property, "property");
        properties.add("(\"" + property + "\")");
        return r();
    }

    public R a(final Map<String, Object> attributes) {
        attributes.putAll(Objects.requireNonNull(attributes, "attributes"));
        return r();
    }

    public R v(Object value) {
        return a("v", value);
    }

    public R a(final String name, final Object value) {
        attributes.put(name, value);
        return r();
    }

    public Object render() {
        StringBuilder sb = new StringBuilder(1024);
        {
            sb.append("v");
            for (String s : properties)
                sb.append('.').append(s);
            Formatter fmt = new Formatter(sb);
            for (String s : wrappers) {
                String t = sb.toString();
                sb.setLength(0);
                fmt.format(s, t);
            }
            sb.insert(0, "test(v) ::= <%<");
            sb.append(">%>");
        }
        template = sb.toString();
        Object r = null;
        Exception ex = null;
        try {
            stg = new STGroupString(null == sourceName ? toString1(template) : sourceName, template);
            stg.setListener(this);
            stg.registerModelAdaptor(Object.class, objectAdapter);
            stg.registerModelAdaptor(String.class, stringAdapter);
            stg.registerModelAdaptor(Integer.class, numberAdapter);
            st = stg.getInstanceOf("test");
            if (null == st)
                throw new STException("failed to create st", null);
            attributes.forEach(st::add);
            r = st.render();
        } catch (Exception e) {
            ex = e;
            failed = true;
        }
        if (failed) {
            throw new STException(format("failed to render:\n%s\nwith attributes=%s",
                                         template, attributes), ex);
        }
        return r;
    }

    // Bloated, because no Array support in Objects.toString, WTF!
    public static String toString1(Object o) {
        final Class<?> eClass = o.getClass();
        if (o.getClass().isArray()) {
            if (eClass == byte[].class)
                return Arrays.toString((byte[]) o);
            else if (eClass == short[].class)
                return Arrays.toString((short[]) o);
            else if (eClass == int[].class)
                return Arrays.toString((int[]) o);
            else if (eClass == long[].class)
                return Arrays.toString((long[]) o);
            else if (eClass == char[].class)
                return Arrays.toString((char[]) o);
            else if (eClass == float[].class)
                return Arrays.toString((float[]) o);
            else if (eClass == double[].class)
                return Arrays.toString((double[]) o);
            else if (eClass == boolean[].class)
                return Arrays.toString((boolean[]) o);
            else { // element is an array of object references
                return Arrays.toString((Object[]) o);
            }
        }
        if (CharSequence.class.isAssignableFrom(eClass))
            return format("\"%s\"", o.toString());
        return o.toString();
    }

    @Override
    public final void compileTimeError(final STMessage msg) {
        log0(msg);
    }

    private void log0(final STMessage msg) {
        failed = true;
        log(msg);
    }

    protected void log(final STMessage msg) {
        System.err.println(msg);
    }

    @Override
    public final void runTimeError(final STMessage msg) {
        log0(msg);
    }

    @Override
    public final void IOError(final STMessage msg) {
        log0(msg);
    }

    @Override
    public final void internalError(final STMessage msg) {
        log0(msg);
    }

    // List functions
    public static final String first = "first(%s)";
    public static final String length = "length(%s)";
    public static final String strlen = "strlen(%s)";
    public static final String last = "last(%s)";
    public static final String rest = "rest(%s)";
    public static final String reverse = "reverse(%s)";
    public static final String trunc = "trunc(%s)";
    public static final String strip = "strip(%s)";
    public static final String trim = "trim(%s)";
    //
    // Adaptors
    public static final AbstractInvokeAdaptor<Object> objectAdapter = new ObjectInvokeAdaptor();
    public static final AbstractInvokeAdaptor<String> stringAdapter = new StringInvokeAdaptor();
    public static final AbstractInvokeAdaptor<Number> numberAdapter = new NumberInvokeAdaptor();

    public static Object[] toStringN(Object... a) {
        for (int i = 0, n = a.length; i < n; i++)
            a[i] = toString1(a[i]);
        return a;
    }

}
