package rwperrott.stringtemplate.v4;

import org.stringtemplate.v4.AttributeRenderer;
import org.testng.annotations.Test;

import java.util.Locale;

import static org.testng.Assert.assertEquals;

/**
 * @author rwperrott
 */
public class StringInvokeRendererTest {
    @Test
    public void test() {
        // String object
        assertEquals(get("ABC", "toLowerCase"), "abc");
        // StringUtils
        assertEquals(get("abc", "capitalize"), "Abc");
        // StringRenderer
        assertEquals(get("abc", "cap"), "Abc");
    }

    private Object get(String value, String formatString) {
        return r.toString(value, formatString, locale);
    }
    private static final AttributeRenderer<String> r = new StringInvokeRenderer();
    private static final Locale locale = Locale.getDefault();
}
