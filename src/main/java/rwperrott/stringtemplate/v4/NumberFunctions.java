package rwperrott.stringtemplate.v4;

import java.math.BigDecimal;
import java.math.BigInteger;

@SuppressWarnings("unused")
public class NumberFunctions {
    public static void registerNumberAdapterFunctions() {
        TypeFunctions.registerFunctionClasses(Byte.class, NumberFunctions.class, StrictMath.class);
        TypeFunctions.registerFunctionClasses(Short.class, NumberFunctions.class, StrictMath.class);
        TypeFunctions.registerFunctionClasses(Integer.class, NumberFunctions.class, StrictMath.class);
        TypeFunctions.registerFunctionClasses(Long.class, NumberFunctions.class, StrictMath.class);
        TypeFunctions.registerFunctionClasses(Float.class, NumberFunctions.class, StrictMath.class);
        TypeFunctions.registerFunctionClasses(Double.class, NumberFunctions.class, StrictMath.class);
        TypeFunctions.registerFunctionClasses(BigInteger.class);
        TypeFunctions.registerFunctionClasses(BigDecimal.class);
    }

    //
    // Aliases for Math functions
    //
    public static int add(int v1, int v2) {
        return Math.addExact(v1, v2);
    }

    public static long add(long v1, long v2) {
        return Math.addExact(v1, v2);
    }

    public static int dec(int v1) {
        return Math.decrementExact(v1);
    }

    public static long dec(long v1) {
        return Math.decrementExact(v1);
    }

    public static int inc(int v1) {
        return Math.incrementExact(v1);
    }

    public static long inc(long v1) {
        return Math.incrementExact(v1);
    }

    public static long sub(int v1, int v2) {
        return Math.subtractExact(v1, v2);
    }

    public static long sub(long v1, long v2) {
        return Math.subtractExact(v1, v2);
    }

    public static int mul(int v1, int v2) {
        return Math.multiplyExact(v1, v2);
    }

    public static long mul(long v1, long v2) {
        return Math.multiplyExact(v1, v2);
    }

    //
    // Added function
    //
    public static int div(int v1, int v2) {
        return v1 / v2;
    }

    public static long div(long v1, long v2) {
        return v1 / v2;
    }

    public static int mod(int v1, int v2) {
        return v1 % v2;
    }

    public static long mod(long v1, long v2) {
        return v1 % v2;
    }
}
