package rwperrott.stringtemplate.v4;

import lombok.NonNull;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

public enum NumberType {
  // Whole number
  BYTE(Byte.class, true, Byte.TYPE),
  INT(Integer.class, true, Integer.TYPE),
  SHORT(Short.class, true, Short.TYPE),
  LONG(Long.class, true, Long.TYPE),
  BIG_INTEGER(BigInteger.class, true, null),
  // Floating point numbers
  FLOAT(Float.class, false, Float.TYPE),
  DOUBLE(Double.class, false, Double.TYPE),
  BIG_DECIMAL(BigDecimal.class, false, null);

  private static final Map<Class<?>, NumberType> MAP;

  static {
    final NumberType[] values = values();
    final Map<Class<?>, NumberType> map = new HashMap<>(values.length);
    for (NumberType nt : values) {
      map.put(nt.cls, nt);
      if (nt.simpleType != null)
        map.put(nt.simpleType, nt);
    }
    MAP = map;
  }

  public static NumberType valueOf(@NonNull Object o) {
    Class<?> cls = (o instanceof Class) ? (Class<?>) o : o.getClass();
    return MAP.get(cls);
  }

  public final Class<?> cls;
  public final boolean whole;
  public final Class<?> simpleType;

  NumberType(@NonNull Class<?> cls, boolean whole, Class<?> simpleType) {
    this.cls = cls;
    this.whole = whole;
    this.simpleType = simpleType;
  }

  public boolean simple() {
    return simpleType != null;
  }
}
