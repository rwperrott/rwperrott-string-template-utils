package rwperrott.stringtemplate.v4;

import lombok.NonNull;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupString;
import org.stringtemplate.v4.compiler.STException;
import org.stringtemplate.v4.misc.STMessage;

import java.util.ArrayList;
import java.util.List;

import static rwperrott.stringtemplate.v4.STUtils.*;

/**
 * Useful for rendering simple templates with only a "v" attribute Used by Test
 *
 * @param <R>
 *
 * @author rwperrott
 */

@SuppressWarnings("unused")
class ValueTemplateRenderer<R extends ValueTemplateRenderer<R>> implements STErrorConsumer {
    public final STContext ctx;
    private final String sourceName;
    private final List<String> propertyNames = new ArrayList<>();
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
        propertyNames.clear();
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
     * @param propertyName may contain some reserved characters.
     *
     * @return this
     */
    public R p(final @NonNull String propertyName) {
        // wrap propertyNames in () and quotes to allow use of reserved chars.
        propertyNames.add(propertyName);
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

        final String templateText;
        {
            final Fmt t = FMT_POOL.get();
            try {
                appendTemplateSignature(t, TEMPLATE_NAME, VALUE_NAME);
                t.append("<%<");
                // Not final, to allow reference swapping
                Fmt b = FMT_POOL.get(), other = null;
                try {
                    b.append(VALUE_NAME);
                    //
                    for (String propertyName : propertyNames)
                        appendProperty(b, propertyName);
                    //
                    if (!wrappers.isEmpty()) {
                        // Wrap b, by swapping b and other, to avoid pointlessly creating a String objects.
                        for (String wrapper : wrappers) {
                            other = FMT_POOL.apply(other);
                            other.format(wrapper, b);
                            final Fmt priorB = b;
                            b = other;
                            other = priorB;
                        }
                    }
                    // Add start and end of template.
                    templateText = t
                            .sb()
                            .append(b)
                            .append(">%>")
                            .toString();
                } finally {
                    FMT_POOL.accept(b);
                    FMT_POOL.accept(other);
                }
            } finally {
                FMT_POOL.accept(t);
            }
        }

        Object r = null;
        Exception ex = null;
        try {
            stg = new STGroupString(null == sourceName
                                    ? STUtils.toString1(templateText)
                                    : sourceName, templateText);
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
            throw new STException(FMT_POOL.use(fmt -> fmt.concat("failed to render template: ", templateText)), ex);
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
        if (null != t)
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
