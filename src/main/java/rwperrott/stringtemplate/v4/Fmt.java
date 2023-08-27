package rwperrott.stringtemplate.v4;

import lombok.NonNull;
import lombok.experimental.Accessors;

import java.util.Formatter;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * <p>This replaces all over adhoc StringBuilders, StringJoiners, and
 * Formatters, and allows optional using of pooled instances.</p>
 * <p>Abstract not passed to delegates, to prevent premature closing.</p>
 * <p>Made a CharSequence, so can be used as a SearchAndReplace source</p>
 */
@SuppressWarnings("ALL")
public interface Fmt extends CharSequence, Appendable {
  static SoftPool<Base> POOL =
    SoftPool.<Base>builder(OfStringBuilder::new)
      .capacity(1000)
      .validator(Base::isOpen)
      .passivator(Base::clear)
      .destroyer(Base::close)
      .build();

  static Base toBase(@NonNull Appendable a) {
    if (a instanceof Base)
      return (Base) a;
    else if (a instanceof StringBuilder)
      return new OfStringBuilder((StringBuilder) a);
    else
      throw new IllegalStateException("Unsupported Appendable: " + a.getClass().getName());
  }

  static <R> R useF(boolean pooled,
                    @NonNull Function<Base, R> function) {
    if (pooled)
      try (CloseableSupplier<Base> p = POOL.get()) {
        return function.apply(p.get());
      }
    else
      return function.apply(new OfStringBuilder());
  }

  static String useC(boolean pooled,
                     @NonNull Consumer<Base> consumer) {
    return useF(pooled, f -> {
      consumer.accept(f);
      return f.toString();
    });
  }

  static <T> String useT(boolean pooled,
                         @NonNull T t,
                         @NonNull BiConsumer<Base, T> consumer) {
    return useF(pooled, f -> {
      consumer.accept(f, t);
      return f.toString();
    });
  }

  static String format(boolean pooled, String format, Object... args) {
    return useF(pooled, f -> f.format(format, args).toString());
  }

  static String joinSquare(boolean pooled,
                               @NonNull Consumer<Fmt> delegate) {
    return useF(pooled, f -> f.joinSquare(delegate).toString());
  }


  static <T> String toString(boolean pooled,
                             @NonNull T t,
                             @NonNull Consumer<Fmt> delegate) {
    return useF(pooled, f -> f.joinToString(t, delegate).toString());
  }

  static <T> String toString(boolean pooled,
                             @NonNull T t,
                             @NonNull BiConsumer<Fmt, T> delegate) {
    return useF(pooled, f -> f.joinToString(t, delegate).toString());
  }

  //
  // Interface
  //

  /**
   * Provided to abstract away all Joiner access to parent's Appendable and Formatter.
   */
  Fmt openParent();

  /**
   * Append a newLine without any delimiter.
   */
  Fmt newLine();

  /**
   * Append a char without any delimiter.
   */
  Fmt append(char c);

  /**
   * Append a String, possibly prefixed by a delimiter.
   */
  Fmt append(@NonNull String s);

  /**
   * Append a CharSequence, possibly prefixed by a delimiter.
   */
  Fmt append(@NonNull CharSequence s);

  /**
   * Append a bounded CharSequence, possibly prefixed by a delimiter.
   */
  Fmt append(@NonNull CharSequence s, int start, int end);

  Fmt appendClassName(@NonNull Object obj);

  Fmt appendClassSimpleName(@NonNull Object obj);

  /**
   * Append via Formatter a format String and args, possibly prefixed by a delimiter.
   */
  Fmt format(@NonNull String format, @NonNull Object... args);

  Fmt formatName(String name);

  Fmt formatIdentity(@NonNull Object obj);


  /**
   * Delegate adding more.
   *
   * @param delegate typically another method
   */
  Fmt delegate(@NonNull Consumer<Fmt> delegate);

  <T> Fmt delegate(@NonNull BiConsumer<Fmt, T> delegate,
                   @NonNull T object);

  <T> Fmt delegate(String fieldName,
                   T value,
                   boolean skipIfValueNull,
                   @NonNull BiConsumer<Fmt, T> delegate);

  /**
   * Add sequence like StringJoiner, but much better!
   */
  Fmt join(@NonNull String delimiter,
           @NonNull String prefix,
           @NonNull String suffix,
           @NonNull Consumer<Fmt> delegate);

  Fmt join(@NonNull String delimiter,
           @NonNull Consumer<Fmt> delegate);

  Fmt join(@NonNull Consumer<Fmt> delegate);

  Fmt joinSquare(@NonNull Consumer<Fmt> delegate);

  Fmt joinCurly(@NonNull Consumer<Fmt> delegate);

  <T> Fmt joinToString(@NonNull T t,
                       @NonNull Consumer<Fmt> delegate);

  <T> Fmt join(@NonNull T t,
               @NonNull String delimiter,
               @NonNull String prefix,
               @NonNull String suffix,
               @NonNull BiConsumer<Fmt, T> delegate);

  <T> Fmt join(@NonNull T t,
               @NonNull String delimiter,
               @NonNull BiConsumer<Fmt, T> delegate);

  <T> Fmt join(@NonNull T t,
               @NonNull BiConsumer<Fmt, T> delegate);

  <T> Fmt joinSquare(@NonNull T t,
                     @NonNull BiConsumer<Fmt, T> delegate);

  <T> Fmt joinCurly(@NonNull T t,
                    @NonNull BiConsumer<Fmt, T> delegate);

  <T> Fmt joinToString(@NonNull T t,
                       @NonNull BiConsumer<Fmt, T> delegate);


  abstract class Abstract<F extends Abstract<F>> implements Fmt, UncheckedCloseable {
    protected abstract Base clearablePatent();

    public abstract boolean isOpen();

    @Override
    public final F newLine() {
      openParent().format("%n");
      return (F) this;
    }

    @Override
    public final F appendClassName(@NonNull Object obj) {
      openParent().append(obj.getClass().getName());
      return (F) this;
    }

    @Override
    public final F appendClassSimpleName(@NonNull Object obj) {
      openParent().append(obj.getClass().getSimpleName());
      return (F) this;
    }

    @Override
    public final F formatName(String name) {
      return (F) format("%s=", name);
    }

    @Override
    public final F formatIdentity(@NonNull Object obj) {
      return (F) format("identity=%X", System.identityHashCode(obj));
    }

    @Override
    public final F delegate(@NonNull Consumer<Fmt> delegate) {
      delegate.accept(this);
      return (F) this;
    }

    @Override
    public final <T> F delegate(@NonNull BiConsumer<Fmt, T> delegate, @NonNull T object) {
      delegate.accept(this, object);
      return (F) this;
    }

    @Override
    public final <T> F delegate(String fieldName,
                                T value,
                                boolean skipIfValueNull,
                                @NonNull BiConsumer<Fmt, T> delegate) {
      if (skipIfValueNull && null == value)
        return (F) this;
      if (null != fieldName)
        formatName(fieldName);

      if (value == null)
        openParent().append("null");
      else
        delegate.accept(this, value);
      return (F) this;
    }

    private final F join0(String delimiter,
                          String prefix,
                          String suffix,
                          Consumer<Fmt> delegate) {
      if (null != prefix)
        openParent().append(prefix);
      try (Joiner j = new Joiner(clearablePatent(), delimiter, suffix)) {
        delegate.accept(j);
      }
      return (F) this;
    }

    @Override
    public final F join(@NonNull String delimiter,
                        @NonNull String prefix,
                        @NonNull String suffix,
                        @NonNull Consumer<Fmt> delegate) {
      return join0(delimiter, prefix, suffix, delegate);
    }

    @Override
    public final F join(@NonNull String delimiter,
                        @NonNull Consumer<Fmt> delegate) {
      return join0(delimiter, null, null, delegate);
    }

    @Override
    public final F join(@NonNull Consumer<Fmt> delegate) {
      return join0(",", null, null, delegate);
    }

    @Override
    public final F joinSquare(@NonNull Consumer<Fmt> delegate) {
      return join0(",", "[", "]", delegate);
    }

    @Override
    public final F joinCurly(@NonNull Consumer<Fmt> delegate) {
      return join(",", "{", "}", delegate);
    }

    @Override
    public final <T> F joinToString(@NonNull T t,
                                    @NonNull Consumer<Fmt> delegate) {

      return appendClassSimpleName(t).joinCurly(delegate);
    }

    private final <T> F join0(@NonNull T t,
                              String delimiter,
                              String prefix,
                              String suffix,
                              BiConsumer<Fmt, T> delegate) {
      if (null != prefix)
        openParent().append(prefix);
      try (Joiner j = new Joiner(clearablePatent(), delimiter, suffix)) {
        delegate.accept(j, t);
      }
      return (F) this;
    }

    @Override
    public final <T> F join(@NonNull T t,
                            @NonNull String delimiter,
                            @NonNull String prefix,
                            @NonNull String suffix,
                            @NonNull BiConsumer<Fmt, T> delegate) {
      return join0(t, delimiter, prefix, suffix, delegate);
    }

    @Override
    public final <T> F join(@NonNull T t,
                            @NonNull String delimiter,
                            @NonNull BiConsumer<Fmt, T> delegate) {
      return join0(t, delimiter, null, null, delegate);
    }

    @Override
    public final <T> F join(@NonNull T t,
                            @NonNull BiConsumer<Fmt, T> delegate) {
      return join0(t, ",", null, null, delegate);
    }

    @Override
    public final <T> F joinSquare(@NonNull T t,
                                  @NonNull BiConsumer<Fmt, T> delegate) {
      return join0(t, ",", "[", "]", delegate);
    }

    @Override
    public final <T> F joinCurly(@NonNull T t,
                                 @NonNull BiConsumer<Fmt, T> delegate) {
      return join0(t, ",", "{", "}", delegate);
    }

    @Override
    public final <T> F joinToString(@NonNull T t,
                                    @NonNull BiConsumer<Fmt, T> delegate) {
      return appendClassSimpleName(t).joinCurly(t, delegate);
    }
  }

  abstract class Base extends Abstract<Base> {
    abstract Base setLength(int length);

    public abstract Base clear();
  }

  @SuppressWarnings({"UnusedReturnValue", "SpellCheckingInspection", "unused"})
  @Accessors(fluent = true)
  final class OfStringBuilder extends Base {
    private static <T> void startJoin(Fmt fmt,
                                      T obj) {
      final Class<?> cls = obj instanceof Class ? (Class<?>) obj : obj.getClass();
      fmt.format("%s{", cls.getSimpleName());
    }

    /**
     * Getter provides custom use; most uses are handled by the methods.
     */
    private StringBuilder sb;
    /**
     * Getter provides custom use; most uses are handled by the methods.
     */
    private Formatter fmt;

    private OfStringBuilder(StringBuilder sb) {
      this.sb = sb;
      this.fmt = new Formatter(sb);
    }

    private OfStringBuilder() {
      this(new StringBuilder());
    }

    @Override
    public boolean isOpen() {
      return sb != null;
    }

    @Override
    public Fmt openParent() {
      return this;
    }

    @Override
    protected Base clearablePatent() {
      return this;
    }

    @Override
    public void close() {
      fmt = null;
      sb = null;
    }

    @Override
    public Base clear() {
      sb.setLength(0);
      return this;
    }

    @Override
    public int length() {
      return sb.length();
    }

    @Override
    public char charAt(int index) {
      return sb.charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
      return sb.subSequence(start, end);
    }

    @Override
    public Base setLength(int newLength) {
      sb.setLength(newLength);
      return this;
    }

    @SuppressWarnings("unused")
    @Override
    public Base append(char c) {
      sb.append(c);
      return this;
    }

    @SuppressWarnings("unused")
    @Override
    public Base append(@NonNull String s) {
      sb.append(s);
      return this;
    }

    @SuppressWarnings("unused")
    @Override
    public Base append(@NonNull CharSequence s) {
      sb.append(s);
      return this;
    }

    @Override
    public Base append(@NonNull CharSequence s, int start, int end) {
      sb.append(s, start, end);
      return this;
    }

    @Override
    public Base format(@NonNull String format, @NonNull Object... args) {
      fmt.format(format, args);
      return this;
    }

    @Override
    public String toString() {
      synchronized (this) { // Block debugger toString race-conditions
        return sb.toString();
      }
    }
  }


  /**
   * A non-static Inner class makes this easy,
   * however deep the joiners go.
   */
  final class Joiner extends Abstract<Joiner> {
    private final Base parent;
    private final int start;
    /**
     * The delimiter for sequence mode.
     */
    private final String delimiter;
    /**
     * The suffix for sequence mode.
     */
    private final String suffix;
    /**
     * If not -1, then closed.
     */
    private int count;

    private Joiner(Base parent, String delimiter, String suffix) {
      this.parent = parent;
      this.start = parent.length();
      this.delimiter = delimiter;
      this.suffix = suffix;
    }

    @Override
    public boolean isOpen() {
      return count != -1;
    }

    @Override
    public Fmt openParent() {
      return parent;
    }

    @Override
    protected Base clearablePatent() {
      return parent;
    }

    /**
     * Use auto-close to add the suffix.
     */
    @Override
    public void close() {
      if (count == -1)
        return;
      if (null != suffix)
        parent.append(suffix);
      count = -1;
    }

    private void delimit() {
      if (count == -1)
        throw new IllegalStateException("closed");
      if (++count > 1)
        parent.append(delimiter);
    }

    @Override
    public int length() {
      return parent.length() - start;
    }

    @Override
    public char charAt(int index) {
      return parent.charAt(start + index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
      final int p = start;
      return parent.subSequence(p + start, p + end);
    }

    @Override
    public Joiner append(char c) {
      delimit();
      parent.append(c);
      return this;
    }

    @Override
    public Joiner append(@NonNull String s) {
      delimit();
      parent.append(s);
      return this;
    }

    @Override
    public Joiner append(@NonNull CharSequence s) {
      delimit();
      parent.append(s);
      return this;
    }

    @Override
    public Joiner append(@NonNull CharSequence s,
                         int start,
                         int end) {
      delimit();
      parent.append(s, start, end);
      return this;
    }

    @Override
    public Joiner format(@NonNull String format,
                         @NonNull Object... args) {
      delimit();
      parent.format(format, args);
      return this;
    }

    /**
     * Only really useful for debugging.
     */
    @Override
    public String toString() {
      synchronized (parent) { // Block debugger toString race-conditions
        if (count == -1 || suffix == null)
          return parent.toString();
        final int priorLength = length();
        parent.append(suffix);
        final String r = parent.toString();
        parent.setLength(priorLength);
        return r;
      }
    }
  }


  /*
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  final class Pool {
    private static final Predicate<Base> CLEARER = f -> {
      f.clear();
      return true;
    };
    private static final SoftPool<Base> STORE =
      SoftPool.<Base>builder(OfStringBuilder::new)
        .capacity(1000)
        .passivator(CLEARER)
        .destroyer(Base::close)
        .build();
    private static final ClassLoader CLASS_LOADER = Base.class.getClassLoader();
    private static final Class<?>[] INTERFACES = {Base.class};

    private static Abstract<?> of() {
      return (Base) Proxy.newProxyInstance(CLASS_LOADER, INTERFACES,
                                                new Facade(STORE.get()));
    }


    @AllArgsConstructor
    private static final class Facade implements InvocationHandler {
      private Base instance;

      @Override
      public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (args.length == 0 && method.getName().equals("close")) {
          STORE.accept(instance);
          instance = null;
          return null;
        }
        return method.invoke(instance, args);
      }
    }
  }
  */
}
