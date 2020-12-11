package rwperrott.stringtemplate.v4;

import static java.lang.ThreadLocal.withInitial;

public class Indent {
    private int depth;
    private static final ThreadLocal<Indent> THREAD_LOCAL = withInitial(Indent::new);

    public static void enter() {
        THREAD_LOCAL.get().depth++;
    }

    public static StringBuilder indent(StringBuilder sb) {
        int d = THREAD_LOCAL.get().depth;
        while (d-- > 0)
            sb.append("  ");
        return sb;
    }
    public static void exit() {
        THREAD_LOCAL.get().depth--;
    }

    
    public static void enter(boolean multiline) {
        if (multiline)
            enter();
    }

    public static void exit(boolean multiline) {
        if (multiline)
            exit();
    }

    public static StringBuilder indent(StringBuilder sb, boolean multiline) {
        return multiline ? indent(sb) : sb;
    }

}
