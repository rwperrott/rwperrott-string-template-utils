package rwperrott.stringtemplate.v4;

import lombok.NonNull;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.StringRenderer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.isRegularFile;
import static java.nio.file.Paths.get;
import static java.util.regex.Pattern.*;
import static org.stringtemplate.v4.STGroup.GROUP_FILE_EXTENSION;
import static rwperrott.stringtemplate.v4.STGroupType.*;

/**
 * @author rwperrott
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public class STUtils {
  public static final char QUOTE = '\"';
  public static final char ROUND_START = '(';
  public static final char ROUND_END = ')';
  private static final Pattern TEMPLATE_PATTERN = compile("^([a-z][^( :]*)\\([^ :)]*\\) *::= *.*$", CASE_INSENSITIVE | MULTILINE);

  /**
   * Used by plugin
   */
  @SuppressWarnings({"UseSpecificCatch", "unused"})
  public static TypeAndURL resolveTypeAndURL(final @NonNull String source,
                                             final @NonNull Path defaultDir) throws IOException {
    final STGroupType type;
    // Check contains a template String
    if (STUtils.templateMatcher(source).find()) {
      return new TypeAndURL(STRING, null);
    }

    // Requires that source a valid URL or a filesystem path!
    Path path;
    if (".".equals(source)) {
      path = defaultDir;
      type = DIRECTORY;
    } else {
      type = source.endsWith(GROUP_FILE_EXTENSION)
        ? FILE
        : DIRECTORY;
      // Try direct conversion to a URL
      try {
        return new TypeAndURL(type, new URI(source).normalize().toURL());
      } catch (Exception ignore) {
      }

      // Resolve relative filesystem path
      path = get(source);
      if (!path.isAbsolute()) {
        path = defaultDir.resolve(path);
      }
    }

    // Validate filesystem paths
    path = path.toRealPath();
    if (!exists(path)) {
      throw new FileNotFoundException(path.toString());
    }
    if (type == FILE && !isRegularFile(path)) {
      throw new IOException(source + " is not a file");
    }

    // Convert to URL, for STGroup creation
    return new TypeAndURL(type, path.toUri().toURL());
  }

  /**
   * Used to identify a source string as a template, and to identity and extract names for start line number mapping.
   * Matcher Group 1: the name of the template.
   */
  public static Matcher templateMatcher(final @NonNull CharSequence cs) {
    return TEMPLATE_PATTERN.matcher(Objects.requireNonNull(cs, "cs"));
  }

  @SuppressWarnings("unchecked")
  public static <V> Map<String, V> validateAttributes(final @NonNull Map<?, ?> map,
                                                      final @NonNull String label,
                                                      final int checkDepth) {
    map.forEach((k, v) -> {
      if (k.getClass() != String.class)
        throw new IllegalArgumentException(format("non-String key %s:%s in %s",
                                                  k.getClass().getName(), k, label));
      if (checkDepth > 0 && v instanceof Map) {
        validateAttributes((Map<?, ?>) v, label, checkDepth - 1);
      }
    });
    return (Map<String, V>) map;
  }

  public static void clearAttributes(final @NonNull ST st) {
    removeAttributes(st, st.getAttributes());
  }

  public static void removeAttributes(final @NonNull ST st, Map<String, ?> attributes) {
    if (null != attributes)
      attributes.forEach((k, v) -> st.remove(k));
  }

  public static void applyAttributes(final @NonNull ST st, Map<String, ?> attributes) {
    if (null != attributes)
      attributes.forEach((k, v) -> {
        if (null == v)
          st.remove(k);
        else
          st.add(k, v);
      });
  }

  public static void registerAllStringTemplateExtensions(final @NonNull STGroup stGroup) {
    stGroup.registerRenderer(String.class, new StringRenderer());
  }

  public static void registerAllUtilsExtensions(final @NonNull STGroup stGroup) {
    StringInvokeRenderer.register(stGroup);
    ObjectInvokeAdaptor.register(stGroup);
    StringInvokeAdaptor.register(stGroup);
    NumberInvokeAdaptor.register(stGroup);
  }

  /**
   * Convert and array of Object to an array of Strings, using toString1.
   *
   * @param a .
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
      return format("\"%s\"", o);
    return o.toString();
  }

  /**
   * @param array a sequence of key, value pairs
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
   * @return a LinkedHashMap, to preserve sequence
   */
  public static Map<String, String> toMap(final @NonNull List<String> list) {
    final int n = list.size();
    if ((n & 1) == 1)
      throw new IllegalArgumentException("list is an odd size list");
    return toMap0(list, n >> 1);
  }

  public static void appendProperty(@NonNull final Appendable a,
                                    @NonNull final Object property) {
    final String s = property.toString();
    try {
      if (isPlainProperty(s))
        a.append('.')
          .append(s);
      else
        a.append('.')
          .append(STUtils.ROUND_START)
          .append(STUtils.QUOTE)
          .append(s)
          .append(STUtils.QUOTE)
          .append(STUtils.ROUND_END);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static boolean isPlainProperty(String s) {
    int i = s.length();
    if (Character.isJavaIdentifierStart(s.charAt(0))) {
      while (--i > 0)
        if (!Character.isJavaIdentifierPart(s.charAt(i)))
          return false;
    } else {
      while (--i >= 0)
        if (!Character.isDigit(s.charAt(i)))
          return false;
    }

    return true;
  }

  private STUtils() {
  }

  /**
   * Used by plugin
   */
  public static class TypeAndURL {

    public final STGroupType type;
    public final URL url;

    private TypeAndURL(STGroupType type, URL url) {
      this.type = type;
      this.url = url;
    }

  }

}
