package rwperrott.stringtemplate.v4;

import org.testng.Assert;

import java.util.Map;

/**
 * Used by AbstractInvokeAdaptorTest
 */
@SuppressWarnings("unused")
class Test extends ValueTemplateRenderer<Test> {

    private Test(String sourceName) {
        super(sourceName);
    }

    public final void assertEquals(Object expected) {
        Object r = render();
        Assert.assertEquals(r, expected, stg.sourceName);
    }

    @Override
    public Test v(final Object value) {
        try {
            return super.v(value);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public Object render() {
        try {
            return super.render();
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    public static Test test() {
        return new Test(null);
    }

    public static Test test(String sourceName) {
        return new Test(sourceName);
    }
}
