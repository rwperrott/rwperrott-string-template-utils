package rwperrott.stringtemplate.v4;

import org.testng.annotations.Test;

import static rwperrott.stringtemplate.v4.Test.test;
import static rwperrott.stringtemplate.v4.ValueTemplateRenderer.*;

/**
 * @author rwperrott
 */
@SuppressWarnings("unused")
public class AbstractInvokeAdaptorTest {

    @Test
    void testObjectAdapter() {
        // Test split toList
        test("o1").v("1,2,3").p("split").p(",").p("toList").assertEquals("123");
        test("o2").v("1,2,3").p("split").p(",").p("toList").w(first).assertEquals("1");
        // Test array toList
        test("o3").v(new String[]{"1", "2", "3"}).p("toList").assertEquals("123");
        test("o4").v(new String[]{"1", "2", "3"}).p("toList").w(first).assertEquals("1");
        test("o5").v(new String[]{"1", "2", "3"}).p("toList").w(last).assertEquals("3");
        test("o6").v(new String[]{"1", "2", "3"}).p("toList").w(rest).assertEquals("23");
        test("o7").v(new String[]{"1", "2", "3"}).p("toList").w(trunc).assertEquals("12");
        // Test array toSortedList
        test("o8").v(new String[]{"2", "1", "3"}).p("toSortedList").assertEquals("123");
        System.out.println("testObjectAdapter Passed");
    }

    @Test
    void testNumberAdapter() {
        test("n1").v(123).p("add").p("1").assertEquals("124");
        test("n2").v(123).p("inc").assertEquals("124");
        test("n3").v(123).p("sub").p("1").assertEquals("122");
        test("n4").v(123).p("dec").assertEquals("122");
        test("n5").v(123).p("mul").p("2").assertEquals("246");
        test("n6").v(123).p("div").p("2").assertEquals("61");
        test("n7").v(123).p("mul").p("2").p("add").p("1").p("div").p("2").assertEquals("123");
        test("n8").v(123).p("mul").p("2").p("add").p("1").p("div").p("2").assertEquals("123");
        test("n9").v(123).p("compare").p("2").assertEquals("1");
        test("n10").v(123).p("compare").p("123").assertEquals("0");
        test("n11").v(123).p("compare").p("124").assertEquals("-1");
        test("n12").v(123).p("equals").p("2").assertEquals("false");
        // This revealed the match all, never convert, bug for equals(Object), which caused erroneous false result.
        test("n13").v(123).p("equals").p("123").assertEquals("true");
        test("n14").v(123).p("equals").p("124").assertEquals("false");
        System.out.println("testNumberAdapter Passed");
    }

    @Test
    public void testStringAdapter() {
        // String
        test("s1").v("ABC").p("toLowerCase").assertEquals("abc");
        // StringUtils
        test("s2").v("abc").p("capitalize").assertEquals("Abc");
        // StringFunctions
        test("s3").v("abc").p("upper").assertEquals("ABC");
        // Integer
        test("s4").v("123").p("parseInt").assertEquals("123");

        // String with parameters
        test("s5").v("ABC").p("substring").p("1").assertEquals("BC");
        test("s6").v("ABC").p("substring").p("1").p("1").assertEquals("");
        //
        System.out.println("testStringAdapter Passed");
    }
}
