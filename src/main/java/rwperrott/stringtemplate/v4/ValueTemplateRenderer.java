package rwperrott.stringtemplate.v4;

import lombok.NonNull;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupString;
import org.stringtemplate.v4.compiler.STException;
import org.stringtemplate.v4.misc.STMessage;

import java.util.*;

import static rwperrott.stringtemplate.v4.STUtils.applyAttributes;
import static rwperrott.stringtemplate.v4.STUtils.registerAllUtilsExtensions;
import static rwperrott.stringtemplate.v4.Utils.fmt;

/**
 * Useful for rendering simple templates with only a "v" attribute Used by Test
 *
 * @param <R>
 */
@SuppressWarnings("unused")
class ValueTemplateRenderer<R extends ValueTemplateRenderer<R>> implements STErrorConsumer {
    protected final Map<String, Object> attributes = new HashMap<>();
    public final STContext ctx;
    private final String sourceName;
    private final List<String> properties = new ArrayList<>();
    private final List<String> wrappers = new ArrayList<>();
    protected STGroupString stg;
    protected String template;
    protected ST st;
    private boolean failed;

    public ValueTemplateRenderer(final @NonNull String sourceName) {
        this.ctx = new STContext().allOptions();
        this.sourceName = sourceName;
    }

    protected void log(final String msg, Throwable t) {
        System.err.println(msg);
        t.printStackTrace();
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

    public final R w(final @NonNull String wrapper) {
        wrappers.add(wrapper);
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

    public R p(final @NonNull String property) {
        // wrap properties in () and quotes to allow use of reserved chars.
        properties.add(fmt().concat("(\"", property, "\")"));
        return r();
    }

    public R a(final @NonNull Map<String, Object> attributes) {
        attributes.putAll(Objects.requireNonNull(attributes, "attributes"));
        return r();
    }

    public R v(Object value) {
        return a("v", value);
    }

    public R a(final @NonNull String name, final Object value) {
        attributes.put(name, value);
        return r();
    }

    public Object render() {
        final Utils.Fmt fmt = fmt();
        fmt.append("v");
        for (String s : properties)
            fmt.append('.').append(s);
        for (String s : wrappers)
            fmt.format(s, fmt.toString());
        template = fmt
                .insert(0, "test(v) ::= <%<")
                .append(">%>")
                .toString();

        Object r = null;
        Exception ex = null;
        try {
            stg = new STGroupString(null == sourceName ? Utils.toString1(template) : sourceName, template);
            stg.setListener(this);
            registerAllUtilsExtensions(stg);
            st = stg.getInstanceOf("test");
            if (null == st)
                throw new STException("failed to create st", null);
            applyAttributes(st, attributes);
            r = st.render();
        } catch (Exception e) {
            ex = e;
            failed = true;
        }
        if (failed) {
            throw new STException(
                    fmt.format("failed to render:\n%s\nwith attributes=%s",
                                 template, attributes)
                         .toString(), ex);
        }
        return r;
    }

    @Override
    public void accept(final String s, final STMessage msg) {
        failed = true;
        log(msg.toString(), msg.cause);
    }

    // List functions
    public static final String length = "length(%s)";
    public static final String reverse = "reverse(%s)";
    public static final String first = "first(%s)";
    public static final String last = "last(%s)";
    public static final String trunc = "trunc(%s)";
    public static final String rest = "rest(%s)";
    public static final String strip = "strip(%s)";
    // String functions
    public static final String strlen = "strlen(%s)";
    public static final String trim = "trim(%s)";
}
