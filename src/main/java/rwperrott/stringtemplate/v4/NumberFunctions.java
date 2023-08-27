package rwperrott.stringtemplate.v4;

import java.math.BigDecimal;
import java.math.BigInteger;


/**
 * @author rwperrott
 */
@SuppressWarnings("unused")
public class NumberFunctions {
  public static void registerNumberAdapterFunctions() {
    TypeFunctions.registerFunctionClasses(Byte.class, NumberFunctions.class, Math.class);
    TypeFunctions.registerFunctionClasses(Short.class, NumberFunctions.class, Math.class);
    TypeFunctions.registerFunctionClasses(Integer.class, NumberFunctions.class, Math.class);
    TypeFunctions.registerFunctionClasses(Long.class, NumberFunctions.class, Math.class);
    TypeFunctions.registerFunctionClasses(Float.class, NumberFunctions.class, Math.class);
    TypeFunctions.registerFunctionClasses(Double.class, NumberFunctions.class, Math.class);
    TypeFunctions.registerFunctionClasses(BigInteger.class);
    TypeFunctions.registerFunctionClasses(BigDecimal.class);
  }

  public static byte toByteExact(long value) {
    final byte r = (byte) value;
    if (value != r)
      throw new ArithmeticException("short overflow");
    return r;
  }

  public static short toShortExact(long value) {
    final short r = (short) value;
    if (value != r)
      throw new ArithmeticException("short overflow");
    return r;
  }

  /**
   * int add.
   */
  public static int add(int v1, int v2) {
    return v1 + v2;
  }

  /**
   * long add.
   */
  public static long add(long v1, long v2) {
    return v1 + v2;
  }

  /**
   * int AND.
   */
  public static int and(int v1, int v2) {
    return v1 & v2;
  }

  /**
   * long AND.
   */
  public static long and(long v1, long v2) {
    return v1 & v2;
  }

  /**
   * int AND.
   */
  public static int andNot(int v1, int v2) {
    return v1 & ~v2;
  }

  /**
   * long AND.
   */
  public static long andNot(long v1, long v2) {
    return v1 & ~v2;
  }


  /**
   * int divide.
   */
  public static int divide(int v1, int v2) {
    return v1 / v2;
  }

  /**
   * long divide.
   */
  public static long divide(long v1, long v2) {
    return v1 / v2;
  }

  /**
   * int negate.
   */
  public static int negate(int v) {
    return -v;
  }

  /**
   * long negate.
   */
  public static long negate(long v) {
    return -v;
  }

  /**
   * int NOT.
   */
  public static int not(int v) {
    return ~v;
  }

  /**
   * long NOT.
   */
  public static long not(long v) {
    return ~v;
  }

  /**
   * int OR.
   */
  public static int or(int v1, int v2) {
    return v1 | v2;
  }

  /**
   * long OR.
   */
  public static long or(long v1, long v2) {
    return v1 | v2;
  }

  /**
   * int Shift Left.
   */
  public static int shiftLeft(int v1, int v2) {
    return v1 << v2;
  }

  /**
   * long shift left.
   */
  public static long shiftLeft(long v1, int v2) {
    return v1 << v2;
  }

  /**
   * int shift Right.
   */
  public static int shiftRight(int v1, int v2) {
    return v1 >> v2;
  }

  /**
   * long shift right.
   */
  public static long shiftRight(long v1, int v2) {
    return v1 >> v2;
  }

  /**
   * Unsigned shift Right.
   */
  public static int shiftRightUnsigned(int v1, int v2) {
    return v1 >>> v2;
  }

  /**
   * long unsigned shift Right.
   */
  public static long shiftRightUnsigned(long v1, int v2) {
    return v1 >>> v2;
  }

  /**
   * int subtract.
   */
  public static int subtract(int v1, int v2) {
    return v1 - v2;
  }

  /**
   * long subtract.
   */
  public static long subtract(long v1, long v2) {
    return v1 - v2;
  }

  /**
   * int XOR.
   */
  public static int xor(int v1, int v2) {
    return v1 ^ v2;
  }

  /**
   * long XOR.
   */
  public static long xor(long v1, long v2) {
    return v1 ^ v2;
  }

  private NumberFunctions() {
  }
}
