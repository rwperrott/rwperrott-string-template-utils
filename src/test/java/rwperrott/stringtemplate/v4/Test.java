package rwperrott.stringtemplate.v4;

import lombok.NonNull;
import org.testng.Assert;

/**
 * @author rwperrott
 */
// Used by AbstractInvokeAdaptorTest
@SuppressWarnings("unused")
class Test extends ValueTemplateRenderer<Test> {

  public static Test test(String sourceName) {
    return new Test(sourceName);
  }

  private Test(String sourceName) {
    super(sourceName);
  }

  public final void assertEquals(Object expected) {
    Object r = render();
    Assert.assertEquals(r, expected, stg.sourceName);
  }

  @Override
  public Test v(final @NonNull Object value) {
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
}
