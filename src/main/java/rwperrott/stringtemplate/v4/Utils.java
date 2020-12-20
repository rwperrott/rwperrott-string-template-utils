package rwperrott.stringtemplate.v4;

import lombok.NonNull;

import java.util.*;

import static java.lang.String.format;
import static java.lang.ThreadLocal.withInitial;

@SuppressWarnings("unused")
public class Utils {
    /**
     * A wrapper for a StringBuilder and a Formatter object.
     */
    @SuppressWarnings("UnusedReturnValue")
    public static final class Fmt {
        private final StringBuilder sb = new StringBuilder();
        private final Formatter f = new Formatter(sb);
        private Fmt() {
        }

        public Fmt clear() {
            sb.setLength(0);
            return this;
        }

        public String concat(Object... a) {
            sb.setLength(0);
            for (Object o : a)
                sb.append(o);
            return sb.toString();
        }

        public Fmt insert(int i, String s) {
            sb.insert(i, s);
            return this;
        }

        public Fmt append(String s) {
            sb.append(s);
            return this;
        }

        public Fmt append(char ch) {
            sb.append(ch);
            return this;
        }

        public Fmt format(String format, Object ... args) {
            f.format(format, args);
            return this;
        }

        /**
         * Gets string, then sets buffer length to zero.
         * @return .
         */
        @Override
        public String toString() {
            final String s = sb.toString();
            sb.setLength(0);
            return s;
        }
    }

    private static final ThreadLocal<Fmt> fmts = withInitial(Fmt::new);

    public static Fmt fmt() {
        return fmts.get().clear();
    }

    /**
     * Convert and array of Object to an array of Strings, using toSting1
     *
     * @param a .
     *
     * @return .
     */
    public static Object[] toStringN(Object... a) {
        for (int i = 0, n = a.length; i < n; i++)
            a[i] = toString1(a[i]);
        return a;
    }

    /**
     * Make up for deficiencies in Objects and Arrays classes, and quote CharSequences.
     *
     * @param o .
     *
     * @return .
     */
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
            else
                return Arrays.toString((Object[]) o);
        }
        if (CharSequence.class.isAssignableFrom(eClass))
            return format("\"%s\"", o.toString());
        return o.toString();
    }

    /**
     * @param array a sequence of key, value pairs
     *
     * @return a LinkedHashMap, to preserve sequence
     */
    public static Map<String, String> toMap(final @NonNull String... array) {
        final int n = array.length;
        if ((n & 1) == 1)
            throw new IllegalArgumentException("list is an odd size list");
        return toMap0(Arrays.asList(array), n >> 1);
    }

    private static Map<String, String> toMap0(final @NonNull List<String> list, final int mapSize) {
        final Map<String, String> map = new LinkedHashMap<>(mapSize);
        String name = null;
        for (String s : list) {
            if (null == name) {
                name = s;
                continue;
            }
            map.put(name, s);
            name = null;
        }
        return map;
    }

    /**
     * @param list a sequence of key, value pairs
     *
     * @return a LinkedHashMap, to preserve sequence
     */
    public static Map<String, String> toMap(final @NonNull List<String> list) {
        final int n = list.size();
        if ((n & 1) == 1)
            throw new IllegalArgumentException("list is an odd size list");
        return toMap0(list, n >> 1);
    }
}
