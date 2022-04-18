package rwperrott.stringtemplate.v4;

import java.util.Formatter;

/**
 * A container for a StringBuilder and Formatter.
 */
@SuppressWarnings("UnusedReturnValue")
public final class Fmt implements Appendable {
    public static final SoftPool<Fmt> POOL = SoftPool
            .builder(Fmt::new)
            .passivator(Fmt::passivate)
            .build();
    private final StringBuilder sb = new StringBuilder();
    private final Formatter f = new Formatter(sb);

    private Fmt() {
    }

    private boolean passivate() {
        sb.setLength(0);
        return true;
    }

    public String concat(Object... a) {
        // Catch probable misuse
        if (sb.length() > 0)
            throw new IllegalStateException("Not cleared");
        append(sb, a);
        return sb.toString();
    }

    private static void append(StringBuilder sb, Object[] a) {
        for (Object o : a)
            sb.append(o);
    }

    public Fmt append(final Object o) {
        sb.append(o);
        return this;
    }

    @Override
    public Fmt append(final CharSequence csq) {
        sb.append(csq);
        return this;
    }

    @Override
    public Fmt append(final CharSequence csq, final int start, final int end) {
        sb.append(csq, start, end);
        return this;
    }

    public Fmt append(char ch) {
        sb.append(ch);
        return this;
    }

    public Fmt append(String s) {
        sb.append(s);
        return this;
    }

    public Fmt format(String format, Object... args) {
        f.format(format, args);
        return this;
    }

    /**
     * Gets string, then sets buffer length to zero.
     *
     * @return .
     */
    @Override
    public String toString() {
        return sb.toString();
    }
}
