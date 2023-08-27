package rwperrott.stringtemplate.v4;

import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.commons.text.WordUtils;
import org.stringtemplate.v4.StringRenderer;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Locale;

/**
 * Sub-string functions, with wrapping of negative offset and len values.
 * <br/>
 * Default register methods from String, org.apache.commons.lang3.StringUtils,
 *
 * @author rwperrott
 */
@SuppressWarnings({"unused", "SpellCheckingInspection"})
public final class StringFunctions {
  private static final String EMPTY = "";

  public static void registerAdapterFunctions() {
    registerRendererFunctions();

    // Functions returning other type objects.
    TypeFunctions.registerFunctionClasses(String.class,
                                          Long.class,
                                          Double.class,
                                          Byte.class,
                                          Short.class,
                                          Integer.class,
                                          Float.class
    );
  }

  /**
   * Function
   */
  public static void registerRendererFunctions() {
    TypeFunctions.registerFunctionClasses(String.class,
                                          StringFunctions.class,
                                          // String Utils first, because word based routines probably less useful.
                                          StringUtils.class,
                                          WordUtils.class,
                                          StringEscapeUtils.class
    );
  }

  /**
   * Returns Left most characters.
   *
   * @param value not null.
   * @param len   if less than zero, src length added e.g., effective len=3 for len=-1 and value length=4.
   * @return empty string, left part of value, or whole value.
   */
  public static String leftstr(@NonNull String value, int len) {
    final int n = value.length();
    if (len < 0)
      len += n;
    else if (len > n)
      len = n;
    return len <= 0 ? EMPTY : value.substring(0, len);
  }

  /**
   * Returns Right most characters.
   *
   * @param value not null.
   * @param len   if less than zero, src length added e.g., effective len=3 for len=-1 and value length=4.
   * @return empty string, right part of value, or whole value.
   */
  public static String rightstr(@NonNull String value, int len) {
    final int n = value.length();
    if (len < 0)
      len += n;
    else if (len > n)
      len = n;
    return len <= 0 ? EMPTY : value.substring(n - len);
  }

  /**
   * @param value  not null.
   * @param offset negative value allowed.
   * @param len    if less than zero, value length added e.g., effective len=3 for len=-1 and value length=4.
   * @return result empty string, part of value, or whole value.
   */
  public static String midstr(@NonNull String value, int offset, int len) {
    final int n = value.length();
    if (len < 0) {
      len += n;
    }
    int end = offset + len;
    if (end > n) {
      end = n;
    }
    if (offset < 0) { // Only fix here, because end calculate from parameter value.
      offset = 0;
    }
    return offset >= end ? EMPTY : value.substring(offset, end);
  }

  /**
   * @param value not null.
   * @param start if negative set to 0.
   * @return empty string, right part of value, or whole value.
   */
  public static String substr(@NonNull String value, int start) {
    if (start < 0) {
      start = 0;
    }
    return start >= value.length() ? EMPTY : value.substring(start);
  }

  /**
   * @param value not null.
   * @param start if negative set to 0.
   * @param end   if more than value length, reduced to value length.
   * @return empty string, part of value, or whole value.
   */
  public static String substr(@NonNull String value, int start, int end) {
    final int n = value.length();
    if (end > n) {
      end = n;
    }
    if (start < 0) {
      start = 0;
    }
    return end <= start ? EMPTY : value.substring(start, end);
  }

  /**
   * Calls StringUtils::capitalize.
   * <br/>
   * Provided here to ensure write class method called.
   */
  public static String stringCapitalize(@NonNull String value) {
    return StringUtils.capitalize(value);
  }

  /**
   * Calls WordUtils::capitalize.
   * <br/>
   * Provided here to ensure write class method called.
   */
  public static String wordCapitalize(@NonNull String value) {
    return WordUtils.capitalize(value);
  }

  /**
   * Calls URLEncoder.encode(v, "UTF-8")
   */
  public static String escapeXML(@NonNull String value) {
    try {
      return URLEncoder.encode(value, "UTF-8");
    } catch (UnsupportedEncodingException ex) {
      throw new InternalError(ex);
    }
  }

  /**
   * Calls original StringRenderer::escapeHTML method, even if dubious.
   */
  public static String escapeHTML(@NonNull String value) {
    return StringRenderer.escapeHTML(value);
  }

  /**
   * Calls String::toLowerCase
   */
  public static String lower(@NonNull String value) {
    return value.toLowerCase();
  }

  /**
   * Calls String::toLowerCase
   */
  public static String lower(@NonNull String value, Locale locale) {
    return value.toLowerCase(locale);
  }

  /**
   * Calls String::toUpperCase
   */
  public static String upper(@NonNull String value) {
    return value.toUpperCase();
  }

  /**
   * Calls String::toUpperCase
   */
  public static String upper(@NonNull String value, Locale locale) {
    return value.toUpperCase(locale);
  }

  private StringFunctions() {
  }
}
