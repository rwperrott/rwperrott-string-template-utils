package rwperrott.stringtemplate.v4;

import org.stringtemplate.v4.misc.STNoSuchPropertyException;

import java.util.concurrent.ExecutionException;

/**
 * Make Throwable and STException less annoying.
 *
 * @author rwperrott
 */
public final class STExceptions {
    private STExceptions() {
    }

    public static STNoSuchPropertyException noSuchPropertyInClass(Class<?> cls, String propertyName, Throwable cause) {
        throw new STNoSuchPropertyException(toException(cause), null, propertyName + " in " + cls.getName());
    }

    /**
     * Deals with the dogmatic idiocy of anything throwing Throwable (e.g. reflect and invoke). throw all Errors and
     * return everything else as an Exception.
     */
    public static Exception toException(Throwable t) {
        if (t instanceof Error)
            throw (Error) t; // Should only be caught when you really, really have to!
        if (t instanceof Exception)
            return (Exception) t;
        // Just-in-case some idiot throws a non-Error and no-Exception Throwable, which sadly is possible!
        return new ExecutionException(t); // Preferable to ambiguous RuntimeException
    }

    public static STNoSuchPropertyException noSuchPropertyInObject(Object o, String propertyName, Throwable cause) {
        throw new STNoSuchPropertyException(toException(cause), o, propertyName);
    }
}
