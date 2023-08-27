package rwperrott.stringtemplate.v4;

import lombok.NonNull;
import org.stringtemplate.v4.AttributeRenderer;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.StringRenderer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * A more power StringRender, which can use compatible instance fields or methods, or methods accepting a string and
 * possibly a Locale parameter.
 * <br/>
 * The current function classes tried: rwperrott.st4.StringFunctions, org.apache.commons.lang3.StringUtils methods
 * and java.lang.String.
 *
 * @author rwperrott
 */
public final class StringInvokeRenderer implements AttributeRenderer<String> {

  // Have to use a static instance for default use, because already extends a different class.
  public static final StringRenderer DEFAULT_RENDERER = new StringRenderer();

  static {
    StringFunctions.registerRendererFunctions();
  }

  public static void register(final @NonNull STGroup stGroup) {
    stGroup.registerRenderer(String.class, new StringInvokeRenderer());
  }

  @Override
  public String toString(final String value, final String formatString, final Locale locale) {
    if (null != formatString) {
      final String alias = toAlias(formatString);
      final MemberInvokers mis = TypeFunctions.get(value.getClass(), alias);
      try {
        List<Object> args;
        MemberInvoker mi;
        if (null != locale) {
          // Try with Locale, when locale not null
          args = new ArrayList<>(1);
          args.add(locale);
          mi = mis.find(true, String.class, args, 1);
          if (mi != null)
            return (String) mi.invoke(value, args);
        }
        // Try without Locale
        args = Collections.emptyList();
        mi = mis.find(true, String.class, args);
        if (null != mi)
          return (String) mi.invoke(value, args);
      } catch (Throwable throwable) {
        throw STExceptions.noSuchPropertyInObject(value, formatString, throwable);
      }
    }

    return DEFAULT_RENDERER.toString(value, formatString, locale);
  }

  private String toAlias(final String name) {
    switch (name) {
      case "cap":
        return "capitalize"; // to org.apache.commons.lang3.StringUtils::capitalize
      case "lower":
        return "toLowerCase"; // to String.toLowerCase
      case "upper":
        return "toUpperCase"; // to String.toUpperCase
      case "url-encode":
        return "escapeURL"; // to StringFunctions::escapeURL
      case "xml-encode":
        return "escapeHTML"; // to StringFunctions::escapeHTML
      default:
        return name;
    }
  }
}
