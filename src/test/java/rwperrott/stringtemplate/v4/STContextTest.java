package rwperrott.stringtemplate.v4;

import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupString;
import org.testng.annotations.Test;

import static java.lang.String.format;
import static org.testng.Assert.fail;

/**
 * @author rwperrott
 */
public class STContextTest {

    private static STContext newSTContext(boolean allOptions) {
        STContext ctx = new STContext();
        if (allOptions)
            ctx.allOptions();
        return ctx;
    }

    private static STGroup newSTGroup() {
        return new STGroupString("test", "hello(you) ::= <%hello <you>%>");
    }

    @Test
    public void testRegisterRenderer() {
        final Class<?> type = String.class;
        final Class<?> to = StringInvokeRenderer.class;
        testRegisterRenderer(false, type.getSimpleName(), to.getName(), false);
        testRegisterRenderer(false, type.getSimpleName(), to.getSimpleName(), false);
        testRegisterRenderer(false, type.getName(), to.getName(), true);
        testRegisterRenderer(false, type.getName(), to.getSimpleName(), false);
        //
        testRegisterRenderer(true, type.getSimpleName(), to.getName(), true);
        testRegisterRenderer(true, type.getSimpleName(), to.getSimpleName(), true);
        testRegisterRenderer(true, type.getName(), to.getName(), true);
        testRegisterRenderer(true, type.getName(), to.getSimpleName(), true);
    }

    @Test
    public void testRegisterModelAdapter() {
        final Class<?> type = String.class;
        final Class<?> to = StringInvokeAdaptor.class;
        testRegisterModelAdapter(false, type.getSimpleName(), to.getName(), false);
        testRegisterModelAdapter(false, type.getSimpleName(), to.getSimpleName(), false);
        testRegisterModelAdapter(false, type.getName(), to.getName(), true);
        testRegisterModelAdapter(false, type.getName(), to.getSimpleName(), false);
        //
        testRegisterModelAdapter(true, type.getSimpleName(), to.getName(), true);
        testRegisterModelAdapter(true, type.getSimpleName(), to.getSimpleName(), true);
        testRegisterModelAdapter(true, type.getName(), to.getName(), true);
        testRegisterModelAdapter(true, type.getName(), to.getSimpleName(), true);
    }

    private void testRegisterRenderer(final boolean addDefaults,
                                      final String attributeType,
                                      final String rendererClassName,
                                      final boolean expectedSuccess) {
        try (STContext ctx = newSTContext(addDefaults)) {
            STGroup stg = newSTGroup();
            ctx.registerRenderer(stg, attributeType, rendererClassName);
            if (!expectedSuccess)
                fail(format("unexpectedly success, for registerRenderer, for %s and %s", attributeType, rendererClassName));
        } catch (Exception e) {
            if (expectedSuccess)
                fail(format("unexpectedly failure, for registerRenderer, for %s and %s", attributeType, rendererClassName), e);
        }
    }

    private void testRegisterModelAdapter(final boolean addDefaults,
                                          final String attributeType,
                                          final String modelAdapterClassName,
                                          final boolean expectedSuccess) {
        try (STContext ctx = newSTContext(addDefaults)) {
            STGroup stg = newSTGroup();
            ctx.registerModelAdaptor(stg, attributeType, modelAdapterClassName);
            if (!expectedSuccess)
                fail(format("unexpectedly success, for registerRenderer, for %s and %s", attributeType, modelAdapterClassName));
        } catch (Exception e) {
            if (expectedSuccess)
                fail(format("unexpectedly failure, for registerRenderer, for %s and %s", attributeType, modelAdapterClassName), e);
        }
    }

}
