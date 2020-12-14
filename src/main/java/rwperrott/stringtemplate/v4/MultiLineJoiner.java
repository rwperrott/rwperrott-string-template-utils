package rwperrott.stringtemplate.v4;

import java.nio.InvalidMarkException;

import static java.util.Objects.requireNonNull;
import static rwperrott.stringtemplate.v4.Indent.*;

/**
 * A better StringJoiner, which supports nested joins and extension.
 * <p>
 * Delimiter is always ","
 *
 * @author infernoz
 */
public class MultiLineJoiner {
    protected final StringBuilder sb;
    protected final boolean multiline;
    protected int count;
    private String suffix;
    //
    private int mark;
    private int markCount;

    public MultiLineJoiner(final String prefix,
                           final String suffix,
                           final boolean multiline) {
        this(new StringBuilder(), prefix, suffix, multiline);
    }

    public MultiLineJoiner(final StringBuilder sb,
                           final String prefix,
                           final String suffix,
                           final boolean multiline) {
        this.sb = requireNonNull(sb, "sb");
        this.multiline = multiline;
        this.suffix = requireNonNull(suffix, "suffix");
        sb.append(requireNonNull(prefix, "prefix"));
        enter(multiline);
    }

    public MultiLineJoiner(final MultiLineJoiner mlj,
                           final String prefix,
                           final String suffix) {
        requireNonNull(mlj, "mlj");
        this.sb = mlj.sb;
        this.multiline = mlj.multiline;
        this.suffix = requireNonNull(suffix, "suffix");
        sb.append(requireNonNull(prefix, "prefix"));
        enter(multiline);
    }

    public StringBuilder sb() {
        if (null == suffix)
            throw new IllegalStateException("completed");
        return sb;
    }

    public void reset() {
        int m = mark;
        if (m < 0)
            throw new InvalidMarkException();
        sb.setLength(m);
        count = markCount;
    }

    public void addName(final String name) {
        if (null == suffix)
            throw new IllegalStateException("completed");
        mark();
        delimit();
        sb.append(name).append('=');
    }

    public void mark() {
        mark = sb.length();
        markCount = count;
    }

    public void delimit() {
        if (null == suffix)
            throw new IllegalStateException("completed");
        if (count++ > 0) {
            sb.append(multiline ? ",\r\n" : ", ");
            indent(sb, multiline);
        }
    }

    public void complete() {
        if (null != suffix) {
            complete0(sb, suffix, 0 == count, multiline);
            suffix = null;
        }
    }

    private static void complete0(
            StringBuilder sb,
            String suffix,
            boolean first,
            boolean multiline) {
        if (!first && multiline)
            sb.append("\r\n");
        //
        if (multiline) {
            exit();
            indent(sb);
        } else
            sb.append(' ');
        sb.append(suffix);
    }

    @Override
    public String toString() {
        return sb.toString();
    }
}
