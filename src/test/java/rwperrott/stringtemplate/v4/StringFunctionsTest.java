package rwperrott.stringtemplate.v4;

import org.testng.annotations.Test;

import static org.testng.Assert.*;
import static rwperrott.stringtemplate.v4.StringFunctions.*;

public class StringFunctionsTest {
    @Test
    public void test_leftstr() {
        assertEquals("012", leftstr(S, 4), "leftstr(S, 4)");
        assertEquals("012", leftstr(S, 3), "leftstr(S, 3)");
        assertEquals("01", leftstr(S, 2), "leftstr(S, 2)");
        assertEquals("0", leftstr(S, 1), "leftstr(S, 1)");
        assertEquals("", leftstr(S, 0), "leftstr(S, 0)");
        assertEquals(leftstr(S, 2), leftstr(S, -1), "leftstr(S, -1)");
        assertEquals(leftstr(S, 1), leftstr(S, -2), "leftstr(S, -2)");
        assertEquals(leftstr(S, 0), leftstr(S, -3), "leftstr(S, -3)");
        assertEquals(leftstr(S, 0), leftstr(S, -4), "leftstr(S, -4)");
    }

    @Test
    public void test_rightstr() {
        assertEquals("012", rightstr(S, 4), "rightstr(S, 4)");
        assertEquals("012", rightstr(S, 3), "rightstr(S, 3)");
        assertEquals("12", rightstr(S, 2), "rightstr(S, 2)");
        assertEquals("2", rightstr(S, 1), "rightstr(S, 1)");
        assertEquals("", rightstr(S, 0), "rightstr(S, 0)");
        assertEquals(rightstr(S, 2), rightstr(S, -1), "rightstr(S, -2)");
        assertEquals(rightstr(S, 1), rightstr(S, -2), "rightstr(S, -2)");
        assertEquals(rightstr(S, 0), rightstr(S, -3), "rightstr(S, -3)");
        assertEquals(rightstr(S, 0), rightstr(S, -4), "rightstr(S, -4)");
    }

    /**
     * Indirectly tests substr too, so no need for a separate testSplit for it too.
     */
    @SuppressWarnings("unused")
    @Test
    public void test_midstr() {
        final int sLen = S.length();
        for (int o = 0; o < 5; o++) {
            for (int len = 0; len <= 5; len++) {
                final int end = Math.min(o + len, sLen);
                final String expected = o >= end ? "" : S.substring(o, end);
                try {
                    assertEquals(midstr(S, o, len),
                                 expected,
                                 String.format("o=%d, len=%d", o, len));
                } catch (AssertionError e) {
                    midstr(S, o, len);
                    throw e;
                }
            }
            for (int len = -5; len < 0; len++) {
                final int end = Math.min(o + len, sLen);
                final String expected = o >= end ? "" : S.substring(o, end);
                try {
                    assertEquals(midstr(S, o, len),
                                 expected,
                                 String.format("o=%d, len=%d", o, len));
                } catch (AssertionError e) {
                    midstr(S, o, len);
                    throw e;
                }
            }
        }
        // Negative offset
        for (int o = -5; o < 0; o++) {
            int start = 0;
            for (int len = 0; len <= 5; len++) {
                int end = Math.min(o + len, sLen);
                final String expected = start >= end ? "" : S.substring(start, end);
                try {
                    assertEquals(midstr(S, o, len),
                                 expected,
                                 String.format("o=%d, len=%d", o, len));
                } catch (AssertionError e) {
                    String r = midstr(S, o, len);
                    throw e;
                }
            }
            for (int len = -5; len < 0; len++) {
                int end = o + len;
                final String expected = start >= end ? "" : S.substring(start, end);
                try {
                    assertEquals(midstr(S, o, len),
                                 expected,
                                 String.format("o=%d, len=%d", o, len));
                } catch (AssertionError e) {
                    String r = midstr(S, o, len);
                    throw e;
                }
            }
        }
    }

    private static final String S = "012";
}
