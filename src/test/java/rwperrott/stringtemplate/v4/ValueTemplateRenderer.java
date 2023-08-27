package rwperrott.stringtemplate.v4;

import lombok.NonNull;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupString;
import org.stringtemplate.v4.compiler.STException;
import org.stringtemplate.v4.misc.STMessage;

import java.util.ArrayList;
import java.util.List;

import static rwperrott.stringtemplate.v4.STUtils.appendProperty;
import static rwperrott.stringtemplate.v4.STUtils.registerAllUtilsExtensions;

/**
 * Useful for rendering simple templates with only a value attribute for testing.
 *
 * @param <R>
 * @author rwperrott
 */

@SuppressWarnings("unused")
class ValueTemplateRenderer<R extends ValueTemplateRenderer<R>> implements STErrorConsumer {
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
   * Add a property name to a list, each later used in <code>("property-name")</code> escaped format.
   *
   * @param propertyName may contain some reserved characters.
   * @return this
   */
  public R p(final @NonNull String propertyName) {
    // wrap propertyNames in () and quotes to allow inclusion of reserved chars.
    propertyNames.add(propertyName);
    return r();
  }

  /**
   * Set the value attribute.
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
    try (CloseableSupplier<Fmt.Base> bSupplier = Fmt.POOL.get();
         CloseableSupplier<Fmt.Base> b2Supplier = Fmt.POOL.get()) {
      // Build body of template
      Fmt.Base b = bSupplier.get();
      b.append(VALUE_NAME);
      for (String propertyName : propertyNames)
        appendProperty(b, propertyName);

      // Wrap body, by swapping b and b2, to avoid pointlessly creating String objects.
      Fmt.Base b2 = b2Supplier.get();
      for (String wrapper : wrappers) {
        b2.format(wrapper, b);
        final Fmt.Base priorB = b;
        b = b2;
        b2 = priorB;
        b2.clear();
      }

      // Join start, signature, body and end of template.
      final Fmt body = b;
      templateText = Fmt.useF(true, f ->
        f
          .format("%s(%s) ::= ", TEMPLATE_NAME, VALUE_NAME)
          .append("<%<")
          .append(body)
          .append(">%>")
          .toString()
      );
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
      throw new STException(Fmt.useF(true,
                                     f -> f.format("failed to render template: %s", templateText).toString()), ex);
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
}
