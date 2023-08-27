package rwperrott.stringtemplate.v4;

import lombok.extern.slf4j.XSlf4j;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static rwperrott.stringtemplate.v4.StringFunctions.*;

/**
 * @author rwperrott
 */
@XSlf4j
public class StringFunctionsTest {
  private static final String S = "012";

  @Test
  public void leftstrTest() {
    assertEquals(leftstr(S, 4), "012");
    assertEquals(leftstr(S, 3), "012");
    assertEquals(leftstr(S, 2), "01");
    assertEquals(leftstr(S, 1), "0");
    assertEquals(leftstr(S, 0), "");
    assertEquals(leftstr(S, -1), "01");
    assertEquals(leftstr(S, -2), "0");
    assertEquals(leftstr(S, -3), "");
    assertEquals(leftstr(S, -4), "");
  }

  @Test
  public void rightstrTest() {
    assertEquals(rightstr(S, 4), "012");
    assertEquals(rightstr(S, 3), "012");
    assertEquals(rightstr(S, 2), "12");
    assertEquals(rightstr(S, 1), "2");
    assertEquals(rightstr(S, 0), "");
    assertEquals(rightstr(S, -1), "12");
    assertEquals(rightstr(S, -2), "2");
    assertEquals(rightstr(S, -3), "");
    assertEquals(rightstr(S, -4), "");
  }

  @Test
  public void midstrTest() {
    assertEquals(midstr(S, 4, 4), "");
    assertEquals(midstr(S, 4, 3), "");
    assertEquals(midstr(S, 4, 2), "");
    assertEquals(midstr(S, 4, 1), "");
    assertEquals(midstr(S, 4, 0), "");
    assertEquals(midstr(S, 4, -1), "");
    assertEquals(midstr(S, 4, -2), "");
    assertEquals(midstr(S, 4, -3), "");
    assertEquals(midstr(S, 4, -4), "");

    assertEquals(midstr(S, 3, 4), "");
    assertEquals(midstr(S, 3, 3), "");
    assertEquals(midstr(S, 3, 2), "");
    assertEquals(midstr(S, 3, 1), "");
    assertEquals(midstr(S, 3, 0), "");
    assertEquals(midstr(S, 3, -1), "");
    assertEquals(midstr(S, 3, -2), "");
    assertEquals(midstr(S, 3, -3), "");
    assertEquals(midstr(S, 3, -4), "");

    assertEquals(midstr(S, 2, 4), "2");
    assertEquals(midstr(S, 2, 3), "2");
    assertEquals(midstr(S, 2, 2), "2");
    assertEquals(midstr(S, 2, 1), "2");
    assertEquals(midstr(S, 2, 0), "");
    assertEquals(midstr(S, 2, -1), "2");
    assertEquals(midstr(S, 2, -2), "2");
    assertEquals(midstr(S, 2, -3), "");
    assertEquals(midstr(S, 2, -4), "");

    assertEquals(midstr(S, 1, 4), "12");
    assertEquals(midstr(S, 1, 3), "12");
    assertEquals(midstr(S, 1, 2), "12");
    assertEquals(midstr(S, 1, 1), "1");
    assertEquals(midstr(S, 1, 0), "");
    assertEquals(midstr(S, 1, -1), "12");
    assertEquals(midstr(S, 1, -2), "1");
    assertEquals(midstr(S, 1, -3), "");
    assertEquals(midstr(S, 1, -4), "");

    assertEquals(midstr(S, 0, 4), "012");
    assertEquals(midstr(S, 0, 3), "012");
    assertEquals(midstr(S, 0, 2), "01");
    assertEquals(midstr(S, 0, 1), "0");
    assertEquals(midstr(S, 0, 0), "");
    assertEquals(midstr(S, 0, -1), "01");
    assertEquals(midstr(S, 0, -2), "0");
    assertEquals(midstr(S, 0, -3), "");
    assertEquals(midstr(S, 0, -4), "");

    assertEquals(midstr(S, -1, 4), "012");
    assertEquals(midstr(S, -1, 3), "01");
    assertEquals(midstr(S, -1, 2), "0");
    assertEquals(midstr(S, -1, 1), "");
    assertEquals(midstr(S, -1, 0), "");
    assertEquals(midstr(S, -1, -1), "0");
    assertEquals(midstr(S, -1, -2), "");
    assertEquals(midstr(S, -1, -3), "");
    assertEquals(midstr(S, -1, -4), "");

    assertEquals(midstr(S, -2, 4), "01");
    assertEquals(midstr(S, -2, 3), "0");
    assertEquals(midstr(S, -2, 2), "");
    assertEquals(midstr(S, -2, 1), "");
    assertEquals(midstr(S, -2, 0), "");
    assertEquals(midstr(S, -2, -1), "");
    assertEquals(midstr(S, -2, -2), "");
    assertEquals(midstr(S, -2, -3), "");
    assertEquals(midstr(S, -2, -4), "");

    assertEquals(midstr(S, -3, 4), "0");
    assertEquals(midstr(S, -3, 3), "");
    assertEquals(midstr(S, -3, 2), "");
    assertEquals(midstr(S, -3, 1), "");
    assertEquals(midstr(S, -3, 0), "");
    assertEquals(midstr(S, -3, -1), "");
    assertEquals(midstr(S, -3, -2), "");
    assertEquals(midstr(S, -3, -3), "");
    assertEquals(midstr(S, -3, -4), "");

    assertEquals(midstr(S, -4, 4), "");
    assertEquals(midstr(S, -4, 3), "");
    assertEquals(midstr(S, -4, 2), "");
    assertEquals(midstr(S, -4, 1), "");
    assertEquals(midstr(S, -4, 0), "");
    assertEquals(midstr(S, -4, -1), "");
    assertEquals(midstr(S, -4, -2), "");
    assertEquals(midstr(S, -4, -3), "");
    assertEquals(midstr(S, -4, -4), "");
  }
}
