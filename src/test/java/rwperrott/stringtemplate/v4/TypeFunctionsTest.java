package rwperrott.stringtemplate.v4;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class TypeFunctionsTest {
    @Test
    public void test() {
        try {
            new StringInvokeRenderer();
            new ObjectInvokeAdaptor();
            new StringInvokeAdaptor();
        } catch (Throwable t) {
            t.printStackTrace();
            fail("failed",t);
        }
    }
}