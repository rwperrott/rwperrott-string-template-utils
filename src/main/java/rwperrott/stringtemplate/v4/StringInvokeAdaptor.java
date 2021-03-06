package rwperrott.stringtemplate.v4;

import lombok.NonNull;
import org.stringtemplate.v4.STGroup;

/**
 * Default registration of String fields and methods, and static methods from the following classes: StringFunctions,
 * org.apache.commons.lang3.StringUtils, org.apache.commons.text.WordUtils, org.apache.commons.text.StringEscapeUtils
 *
 * @author rwperrott
 */
public final class StringInvokeAdaptor extends AbstractInvokeAdaptor<String> {
    public StringInvokeAdaptor() {
        super(true); // Probably not a good idea to try to access non-public Members of String.
    }

    public static void register(final @NonNull STGroup stGroup) {
        stGroup.registerRenderer(String.class, new StringInvokeRenderer());
    }

    @Override
    protected String toAlias(final String name) {
        switch (name) {
            case "url-encode":
                return "escapeURL"; // to StringFunctions::escapeURL
            case "xml-encode":
                return "escapeHTML"; // to StringFunctions::escapeHTML
            default:
                return name;
        }
    }

    static {
        StringFunctions.registerAdapterFunctions();
    }
}
