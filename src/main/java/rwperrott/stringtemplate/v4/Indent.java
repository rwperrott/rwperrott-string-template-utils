package rwperrott.stringtemplate.v4;

import static java.lang.ThreadLocal.withInitial;

/**
 * @author rwperrott
 */
@SuppressWarnings({"UnusedReturnValue", "unused"})
public class Indent {
    private int depth;
    private static final ThreadLocal<Indent> THREAD_LOCAL = withInitial(Indent::new);

    public static void increment() {
        THREAD_LOCAL.get().depth++;
    }

    public static void decrement() {
        THREAD_LOCAL.get().depth--;
    }

    public static StringBuilder appendTo(StringBuilder sb) {
        int d = THREAD_LOCAL.get().depth;
        while (d-- > 0)
            sb.append("  ");
        return sb;
    }
}
