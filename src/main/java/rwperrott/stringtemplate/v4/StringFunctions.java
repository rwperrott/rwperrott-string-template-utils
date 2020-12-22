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
 * <p>
 * Defaults registers methods from String, org.apache.commons.lang3.StringUtils,
 * <p>
 * I don't care about the StringTemplate developer's purity arguments, we need pragmatism like this, to make
 * StringTemplate a lot more usable.
 *
 * @author rwperrott
 */
@SuppressWarnings("unused")
public final class StringFunctions {
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
                                              // String Utils first, because word based routines are probably less useful.
                                              StringUtils.class,
                                              WordUtils.class,
                                              StringEscapeUtils.class
                                             );
    }

    /**
     * Return len character String from the start of v.
     *
     * @param value src
     * @param len   if less than zero, v length added
     *
     * @return "" if len less than or equal to zero; v if len more than or equal to zero.
     */
    public static String leftstr(@NonNull String value, int len) {
        final int n = value.length();
        if (len < 0) {
            len += n;
        } else if (len >= n)
            return value;
        return len <= 0 ? "" : value.substring(0, len);
    }

    /**
     * Return len character String from the end of v.
     *
     * @param value src
     * @param len   if less than zero, v length added
     *
     * @return "" if len less than or equal to zero; v if len more than or equal to zero.
     */
    public static String rightstr(@NonNull String value, int len) {
        final int n = value.length();
        if (len >= n)
            return value;
        if (len < 0)
            len += n;
        return len <= 0 ? "" : value.substring(n - len, n);
    }

    /**
     * @param value  src
     * @param offset can be negative
     * @param len    if negative result is ""
     *
     * @return result of substr(offset, offset + len);
     */
    public static String midstr(@NonNull String value, int offset, int len) {
        return substr(value, offset, offset + len);
    }

    /**
     * @param value src
     * @param start if negative set to 0
     * @param end   if more than v length, set to v length
     *
     * @return result, "" if start more than or equal to end
     */
    public static String substr(@NonNull String value, int start, int end) {
        if (start < 0)
            start = 0;
        final int n = value.length();
        if (end > n)
            end = n;
        return start >= end ? "" : value.substring(start, end);
    }

    /**
     * Calls StringUtils::capitalize.
     * <p>
     * Not done via alias to avoid clash with
     */
    public static String cap(String value) {
        return StringUtils.capitalize(value);
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
    public static String escapeHTML(String value) {
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

    /**
     * Calls WordUtils::capitalize to unmask it behind StringUtils::capitalize
     * <p>
     * Wouldn't be needed if the method naming had a "word" prefix, idiots!
     */
    public static String wordCapitalize(String value) {
        return WordUtils.capitalize(value);
    }
}
