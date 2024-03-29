package rwperrott.stringtemplate.v4;

import lombok.NonNull;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;
import static rwperrott.stringtemplate.v4.NumberFunctions.toByteExact;
import static rwperrott.stringtemplate.v4.NumberFunctions.toShortExact;

/**
 * Handles conversion of property id values to the correct parameter type values for the candidate method.
 * <br/>
 * Comparable by parameters types mapped to TypeAdapters, so allow sorting.
 * <br/>
 * UnaryOperator::apply(..) first attempts to convert Object v, then attempt to convert String v.
 *
 * @author rwperrott
 */
public final class TypeConverter implements Comparable<TypeConverter>, UnaryOperator<Object>, Predicate<Object> {
  static final TypeConverter[] NONE = {};
  /**
   * Used by ClassMembers, MemberInvoker
   */
  private static final Set<Class<?>> WHOLE_SIMPLE_NUMBERS;
  private static final Map<Class<?>, Class<?>> BOX_MAP;
  private static final Pattern BOOL = Pattern.compile("^((t|true)|f|false)$", Pattern.CASE_INSENSITIVE);
  private static final Map<Class<?>, TypeConverter> CONVERTER_MAP;
  private static final UnaryOperator<Object> UNSUPPORTED = v -> null;

  static {
    // Create WHOLE_SIMPLE_NUMBERS, for all whole simple number Types.
    final Set<Class<?>> wholeSimpleNumbers = new HashSet<>(9);
    wholeSimpleNumbers.add(Byte.TYPE);
    wholeSimpleNumbers.add(Byte.class);
    wholeSimpleNumbers.add(Short.TYPE);
    wholeSimpleNumbers.add(Short.class);
    wholeSimpleNumbers.add(Integer.TYPE);
    wholeSimpleNumbers.add(Integer.class);
    wholeSimpleNumbers.add(Long.TYPE);
    wholeSimpleNumbers.add(Long.class);
    WHOLE_SIMPLE_NUMBERS = wholeSimpleNumbers;

    // Create BOX_MAP, for all primitive Types.
    final Map<Class<?>, Class<?>> boxMap = new HashMap<>(9);
    boxMap.put(Void.TYPE, Void.class);
    boxMap.put(Boolean.TYPE, Boolean.class);
    boxMap.put(Byte.TYPE, Byte.class);
    boxMap.put(Short.TYPE, Short.class);
    boxMap.put(Character.TYPE, Character.class);
    boxMap.put(Integer.TYPE, Integer.class);
    boxMap.put(Long.TYPE, Long.class);
    boxMap.put(Float.TYPE, Float.class);
    boxMap.put(Double.TYPE, Double.class);
    BOX_MAP = boxMap;

    // Create ordered CONVERTER_MAP, for Default values.
    final Default[] defaults = Default.values();
    final Map<Class<?>, TypeConverter> converterMap = new LinkedHashMap<Class<?>, TypeConverter>(defaults.length) {
      @Override
      public TypeConverter get(@NonNull final Object key) {
        final Class<?> aClass = (key instanceof Class) ? (Class<?>) key : key.getClass();
        return CONVERTER_MAP.computeIfAbsent(aClass, from -> {
          from = box(from);
          for (final Map.Entry<Class<?>, TypeConverter> e : CONVERTER_MAP.entrySet())
            if (ClassMembers.isAssignableFrom(e.getKey(), from))
              return e.getValue();
          return new TypeConverter(CONVERTER_MAP.size(), from, UNSUPPORTED); // Allow instanceof matching
        });
      }
    };
    for (Default aDefault : defaults)
      registerPrivate(converterMap, aDefault.type, aDefault);
    CONVERTER_MAP = converterMap;
  }

  private static Number num(Object o) {
    return (Number) o;
  }

  private static BigDecimal bds(Object o) {
    return new BigDecimal(o.toString());
  }

  private static BigDecimal bdl(Number n) {
    return BigDecimal.valueOf(n.longValue());
  }

  private static BigDecimal bdd(Number n) {
    return BigDecimal.valueOf(n.doubleValue());
  }

  /**
   * Allow external code to add new TypeConverters, or replace placeholder entries, while retaining original order.
   */
  @SuppressWarnings({"unused", "SynchronizationOnLocalVariableOrMethodParameter"})
  public static void register(@NonNull Class<?> type,
                              @NonNull UnaryOperator<Object> converter) {
    final Map<Class<?>, TypeConverter> converterMap = CONVERTER_MAP;
    synchronized (converterMap) {
      registerPrivate(converterMap, type, converter);
    }
  }

  private static void registerPrivate(@NonNull final Map<Class<?>, TypeConverter> converterMap,
                                      @NonNull final Class<?> type,
                                      @NonNull final UnaryOperator<Object> converter) {
    converterMap.compute(type, (from, old) ->
      (null != old && old.converter != UNSUPPORTED)
      ? old : new TypeConverter(converterMap.size(), type, converter));
  }

  static TypeConverter toTypeConverter(@NonNull Class<?> parameterType) {
    final TypeConverter tc = CONVERTER_MAP.get(parameterType);
    if (UNSUPPORTED == tc)
      throw new IllegalArgumentException(parameterType.getName());
    return tc;
  }

  /**
   * Used by ClassMembers and MemberInvoker
   */
  @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
  static TypeConverter[] toTypeConverters(@NonNull Class<?>[] parameterTypes) {
    final int n = parameterTypes.length;
    if (n == 0)
      return TypeConverter.NONE;
    //
    final TypeConverter[] typeConverters = new TypeConverter[n];
    final Map<Class<?>, TypeConverter> converterMap = CONVERTER_MAP;
    synchronized (converterMap) {
      for (int i = 0; i < n; i++) {
        final TypeConverter tc = converterMap.get(parameterTypes[i]);
        if (UNSUPPORTED == tc)
          return null;
        typeConverters[i] = tc;
      }
    }
    return typeConverters;
  }

  /**
   * Tests if type a simple whole number.
   * <p/>
   * Also used by TypeConverter
   */
  static boolean isWholeSimpleNumber(Class<?> type) {
    return WHOLE_SIMPLE_NUMBERS.contains(type);
  }

  /**
   * Converts type to boxed type, when a primitive type.
   * <p/>
   * Also used by ClassMembers and TypeIndexMap
   */
  static Class<?> box(Class<?> cls) {
    return cls.isPrimitive() ? BOX_MAP.get(cls) : cls;
  }

  /**
   * ClassMembers constructor uses this to generate TypeConverter array and to populate valueIndexOf map.
   *
   * @param parameterTypes an array of parameters to index
   * @param valueIndexOf   a map to accept type indexes
   * @return the resulting TypeConverter array for all the parameter types.
   */
  @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
  static TypeConverter[] toTypeConverters(@NonNull Class<?>[] parameterTypes, @NonNull TypeIndexMap valueIndexOf) {
    final int n = parameterTypes.length;
    if (n == 0)
      return TypeConverter.NONE;
    final TypeConverter[] typeConverters = new TypeConverter[n];
    final Map<Class<?>, TypeConverter> converterMap = CONVERTER_MAP;
    synchronized (converterMap) {
      for (int i = 0; i < n; i++) {
        final Class<?> type = parameterTypes[i];
        valueIndexOf.putIfAbsent(type, i);
        final TypeConverter tc = converterMap.get(type);
        typeConverters[i] = tc;
      }
    }
    return typeConverters;
  }

  /**
   * Attempts to convert args to correct types for candidate method.
   * <br/>
   * Used by MemberInvoker::convert
   *
   * @return true if all conversions successful.
   */
  static boolean convert(@NonNull final List<Object> args,
                         @NonNull final TypeConverter[] typeAdapters,
                         int extrasLen) {
    final int n = args.size();
    if (n != typeAdapters.length)
      return false;
    //
    int i = 0;
    try {
      for (int tn = n - extrasLen; i < tn; i++) {
        Object arg = typeAdapters[i].apply(args.get(i));
        if (arg == null)
          return false;
        args.set(i, arg);
      }
      // Check the extra arguments in args, without conversion.
      while (extrasLen-- > 0) {
        if (!typeAdapters[i].test(args.get(i)))
          return false;
        i++;
      }
      return true; // All values compatible
    } catch (NullPointerException e) {
      throw new IllegalStateException(
        format("Bug at i=%d, for %s, typeAdapters %s and extrasLen=%d",
               i, args, Arrays.toString(typeAdapters), extrasLen), e);
    } catch (Exception e) {
      return false;
    }
  }

  private final int compareValue; // Used to store order v
  private final Class<?> type;
  private final UnaryOperator<Object> converter;

  private TypeConverter(final int compareValue,
                        @NonNull final Class<?> type,
                        @NonNull final UnaryOperator<Object> converter) {
    this.compareValue = compareValue;
    this.type = type;
    this.converter = converter;
  }

  @Override
  public int hashCode() {
    return compareValue;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    final TypeConverter that = (TypeConverter) o;
    return compareValue == that.compareValue;
  }

  @Override
  public int compareTo(@NonNull final TypeConverter o) {
    return Integer.compare(compareValue, o.compareValue);
  }

  @Override
  public String toString() {
    return "TypeConverter{type=" + type + "}";
  }

  /**
   * For testing/converting args.
   *
   * @param o arg the object to test/convert.
   * @return the converted object.
   */
  @Override
  public Object apply(Object o) {
    if (null == o)
      return null;
    // Will handle most matches, with no type conversion
    if (ClassMembers.isAssignableFrom(type, o.getClass()))
      return o;
    // Explicitly convert char[] to String, because toString() useless!
    if (o.getClass() == char[].class)
      o = new String((char[]) o);
    return converter.apply(o);
  }

  /**
   * For testing extra args only.
   *
   * @param o extra arg object
   * @return true if instanceof type
   */
  @Override
  public boolean test(final Object o) {
    return null != o && ClassMembers.isAssignableFrom(type, o.getClass());
  }

  /**
   * Hidden defaults. Only need to try value datatype translation, because caller already tested for instanceOf.
   */
  private enum Default implements UnaryOperator<Object> {
    CHAR(Character.TYPE) {
      private final BigInteger MAX_VALUE = BigInteger.valueOf(0xFFFFL);

      @Override
      public Object apply(@NonNull Object o) {
        final CharSequence cs = o instanceof CharSequence ? (CharSequence) o : o.toString();
        if (cs.length() != 1)
          throw new IllegalArgumentException(format("\"%s\" not one character long", o));
        return cs.charAt(0);
      }
    },
    LONG(Long.TYPE) {
      @Override
      public Object apply(@NonNull Object o) {
        final NumberType nt = NumberType.valueOf(o);
        if (nt == null)
          return bds(o).longValueExact();
        switch (nt) {
          case BIG_INTEGER:
            return ((BigInteger) o).longValueExact();
          case BIG_DECIMAL:
            return ((BigDecimal) o).longValueExact();
          case FLOAT:
          case DOUBLE:
            return bdl(num(o)).longValueExact();
          default:
            return nt.simple()
                   ? num(o).longValue()
                   : bdd(num(o)).longValueExact();
        }
      }
    },
    INT(Integer.TYPE) {
      @Override
      public Object apply(@NonNull Object o) {
        final NumberType nt = NumberType.valueOf(o);
        if (nt == null)
          return bds(o).intValueExact();
        switch (nt) {
          case BIG_INTEGER:
            return ((BigInteger) o).intValueExact();
          case BIG_DECIMAL:
            return ((BigDecimal) o).intValueExact();
          case FLOAT:
          case DOUBLE:
            return bdl(num(o)).intValueExact();
          default:
            return nt.simple()
                   ? num(o).intValue()
                   : bdd(num(o)).intValueExact();
        }
      }
    },
    SHORT(Short.TYPE) {
      @Override
      public Object apply(@NonNull Object o) {
        return toShortExact(((Integer) INT.apply(o)));
      }
    },
    BYTE(Byte.TYPE) {
      @Override
      public Object apply(@NonNull Object o) {
        return toByteExact(((Integer) INT.apply(o)));
      }
    },
    DOUBLE(Double.TYPE) {
      @Override
      public Object apply(@NonNull Object o) {
        if (o instanceof Number) {
          return ((Number) o).doubleValue();
        }
        return Double.parseDouble(o.toString());
      }
    },
    FLOAT(Float.TYPE) {
      @Override
      public Object apply(@NonNull Object o) {
        if (o instanceof Number) {
          return ((Number) o).floatValue();
        }
        return Float.parseFloat(o.toString());
      }
    },
    BOOLEAN(Boolean.TYPE) {
      @Override
      public Object apply(@NonNull Object o) {
        if (o instanceof Number) {
          if (isWholeSimpleNumber(o.getClass()))
            return ((Number) o).longValue() != 0L;
          if (o instanceof BigInteger)
            return ((BigInteger) o).signum() != 0;
          if (o instanceof BigDecimal)
            return ((BigDecimal) o).signum() != 0;
          double v = ((Number) o).doubleValue();
          return Double.isNaN(v) || v != 0d;
        }
        final Matcher m = BOOL.matcher(o.toString());
        if (m.matches())
          return m.start(2) < m.end(2);
        return Long.parseLong(o.toString()) != 0;
      }
    },
    BIGINTEGER(BigInteger.class) {
      @Override
      public Object apply(@NonNull Object o) {
     if (o instanceof Number && (isWholeSimpleNumber(o.getClass())))
          return BigInteger.valueOf(((Number) o).longValue());
        String s = o.toString();
        if (s.indexOf('.') >= 0)
          return new BigDecimal(s).toBigIntegerExact();
        else
          return new BigInteger(o.toString());
      }
    },
    BIGDECIMAL(BigDecimal.class) {
      @Override
      public Object apply(@NonNull Object o) {
        return new BigDecimal(o.toString());
      }
    },
    STRING(String.class) {
      @Override
      public Object apply(@NonNull Object o) {
        return o.toString();
      }
    },
    CHAR_SEQUENCE(CharSequence.class) {
      @Override
      public Object apply(@NonNull Object o) {
        return o.toString();
      }
    },
    CHAR_ARRAY(char[].class) {
      @Override
      public Object apply(@NonNull Object o) {
        if (o instanceof Character)
          return new char[]{(Character) o};
        return o.toString().toCharArray();
      }
    },
    LOCALE(Locale.class) {
      @Override
      public Object apply(@NonNull Object o) {
        return new Locale(o.toString());
      }
    };

    private final Class<?> type;

    Default(Class<?> type) {
      this.type = box(type);
    }
  }
}
