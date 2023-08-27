package rwperrott.stringtemplate.v4;

import lombok.NonNull;
import org.stringtemplate.v4.STGroup;

/**
 * Default registration of String fields and methods, and static methods from the following classes: StringFunctions,
 * org.apache.commons.lang3.StringUtils, org.apache.commons.text.WordUtils, org.apache.commons.text.StringEscapeUtils
 * <br/>
 * Aliases:
 * <ul>
 *     <li>cap        -> StringFunctions.stringCapitalize</li>
 *     <li>capitalize -> StringFunctions.stringCapitalize</li>
 *     <li>wordCap    -> StringFunctions.wordCapitalise</li>
 *     <li>url-encode -> StringFunctions.escapeURL</li>
 *     <li>urlEncode  -> StringFunctions.escapeURL</li>
 *     <li>xml-encode -> StringFunctions.escapeHTML</li>
 *     <li>xmlEncode  -> StringFunctions.escapeHTML</li>
 * </ul>
 *
 * @author rwperrott
 */
public final class StringInvokeAdaptor extends AbstractInvokeAdaptor<String> {
  static {
    StringFunctions.registerAdapterFunctions();
  }

  public static void register(final @NonNull STGroup stGroup) {
    stGroup.registerRenderer(String.class, new StringInvokeRenderer());
  }

  public StringInvokeAdaptor() {
    super(true); // Probably not a good idea to try to access non-public Members of String.
  }

  @Override
  protected String toAlias(final String name) {
    switch (name) {
      // StringFunctions methods
      case "cap":
      case "capitalize":
        return "stringCapitalize";
      case "wordCap":
        return "wordCapitalise";
      case "url-encode":
      case "urlEncode":
        return "escapeURL";
      case "xml-encode":
      case "xmlEncode":
        return "escapeHTML";
      default:
        return name;
    }
  }
}
