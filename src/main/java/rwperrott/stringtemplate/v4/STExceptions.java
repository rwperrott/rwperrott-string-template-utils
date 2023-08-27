package rwperrott.stringtemplate.v4;

import org.stringtemplate.v4.misc.STNoSuchPropertyException;

import java.util.concurrent.ExecutionException;

/**
 * Make Throwable and STException less annoying.
 *
 * @author rwperrott
 */
public final class STExceptions {
  /**
   * Fixes the annoying bad manners of anything "throwing" Throwable (e.g. reflect and invoke)!
   * <br/>
   * Throws all Errors and returns everything else as an Exception.
   * <br/>
   * Runtime Exceptions
   */
  public static Exception toException(Throwable t) {
    if (t instanceof Error)
      throw (Error) t; // Best to rethrow Errors, because maybe fatal.
    if (t instanceof Exception)
      return (Exception) t;
    // Just-in-case a not Error and not Exception sub-class of Throwable ever gets thrown!
    return new ExecutionException(t);
  }

  public static STNoSuchPropertyException noSuchPropertyInObject(Object o, String propertyName, Throwable cause) {
    throw new STNoSuchPropertyException(toException(cause), o, propertyName);
  }

  private STExceptions() {
  }
}
