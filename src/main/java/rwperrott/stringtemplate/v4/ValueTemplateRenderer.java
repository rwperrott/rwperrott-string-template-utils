package rwperrott.stringtemplate.v4;

import lombok.NonNull;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupString;
import org.stringtemplate.v4.compiler.STException;
import org.stringtemplate.v4.misc.STMessage;

import java.util.ArrayList;
import java.util.List;

import static rwperrott.stringtemplate.v4.STUtils.registerAllUtilsExtensions;
import static rwperrott.stringtemplate.v4.Utils.fmt;

/**
 * Useful for rendering simple templates with only a "v" attribute Used by Test
 *
 * @param <R>
 */
@SuppressWarnings("unused")
class ValueTemplateRenderer<R extends ValueTemplateRenderer<R>> implements STErrorConsumer {
    public final STContext ctx;
    private final String sourceName;
    private final List<String> properties = new ArrayList<>();
    private final List<String> wrappers = new ArrayList<>();
    protected STGroupString stg;
    protected String template;
    protected ST st;
    private Object value;
    private boolean failed;

    public ValueTemplateRenderer(final @NonNull String sourceName) {
        this.ctx = new STContext().allOptions();
        this.sourceName = sourceName;
    }

    public final void clear() {
        properties.clear();
        wrappers.clear();
        template = null;
        value = null;
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

    /**
     * Add a property name, which will later be added in <code>("property-name")</code> escaped format.
     *
     * @param property property, which can contain some reserved characters.
     *
     * @return this
     */
    public R p(final @NonNull String property) {
        // wrap properties in () and quotes to allow use of reserved chars.
        properties.add(property);
        return r();
    }

    /**
     * Set the value of the "v" attribute.
     *
     * @param value .
     *
     * @return this
     */
    public R v(@NonNull Object value) {
        this.value = value;
        return r();
    }

    public Object render() {
        if (null == value)
            throw new IllegalStateException("missing \"+VALUE_NAME+\" attribute");

        final Utils.Fmt fmt = fmt();
        final String templateStart = fmt()
                .format("%s(%s) ::= <%<", TEMPLATE_NAME, VALUE_NAME)
                .toString();
        // Add properties.
        fmt.append(VALUE_NAME);
        for (String property : properties) {
            fmt.append('.');
            fmt().concat("(\"", property, "\")");
        }
        // Wrappers template result.
        for (String s : wrappers)
            fmt.format(s, fmt.toString());
        // Add start and end of template.
        template = fmt
                .insert(0, templateStart)
                .append(">%>")
                .toString();

        Object r = null;
        Exception ex = null;
        try {
            stg = new STGroupString(null == sourceName
                                    ? Utils.toString1(template)
                                    : sourceName, template);
            stg.setListener(this);
            registerAllUtilsExtensions(stg);
            st = stg.getInstanceOf(TEMPLATE_NAME);
            if (null == st)
                throw new STException("failed to create st", null);
            st.add(VALUE_NAME, value);
            r = st.render();
        } catch (Exception e) {
            ex = e;
            failed = true;
        }
        if (failed) {
            throw new STException(fmt.format("failed to render template: ", template)
                                     .toString(),
                                  ex);
        }
        return r;
    }

    @Override
    public void accept(final String s, final STMessage msg) {
        failed = true;
        log(msg.toString(), msg.cause);
    }

    protected void log(final String msg, Throwable t) {
        System.err.println(msg);
        t.printStackTrace();
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
    private static final String VALUE_NAME = "value";
    private static final String TEMPLATE_NAME = "template";
}
