package rwperrott.stringtemplate.v4;

import org.stringtemplate.v4.ST;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * Does a lot more than primitive StringJoiner class!
 *
 * @author rwperrott
 */
public final class ToStringBuilder extends MultiLineJoiner {
  public ToStringBuilder(final String className,
                         final boolean multiline) {
    this(new StringBuilder(), className, multiline);
  }

  public ToStringBuilder(final StringBuilder sb,
                         final String className,
                         final boolean multiline) {
    super(sb, className + " { ", "}", multiline);
    requireNonNull(className, "className");
    sb.append(className).append(" { ");
  }

  public ToStringBuilder(final MultiLineJoiner mlj,
                         final String className) {
    this(mlj.sb(), className, mlj.multiline);
  }

  public void add(final String name, final Object value) {
    if (null == value) {
      return;
    }
    addName(name);
    sb.append('=');
    if (!Append.toString(this, value))
      reset();
  }

  @Override
  public String toString() {
    return sb.toString();
  }

  private enum Append implements MultilineAppender {
    STRING(CharSequence.class) {
      @Override
      public boolean appendTo(final MultiLineJoiner mlj, final Object o) {
        if (null == o) {
          return false;
        }
        mlj.sb().append('\"').append(o).append('\"');
        return true;
      }
    },
    COLLECTION(Collection.class) {
      @Override
      public boolean appendTo(final MultiLineJoiner mlj, final Object o) {
        final Collection<?> col = (Collection<?>) o;
        if (col.isEmpty()) {
          mlj.sb().append("[]");
          return false;
        }
        final MultiLineJoiner j = new MultiLineJoiner(mlj, "[", "]");
        col.forEach(v -> {
          j.delimit();
          Append.toString(mlj, v);
        });
        j.complete();
        return true;
      }
    },
    MAP(Map.class) {
      @Override
      public boolean appendTo(final MultiLineJoiner mlj, final Object o) {
        final Map<?, ?> map = (Map<?, ?>) o;
        if (map.isEmpty()) {
          mlj.sb().append("{}");
          return false;
        }
        final MultiLineJoiner j = new MultiLineJoiner(mlj, "{", "}");
        map.forEach((k, v) -> {
          j.addName(k.toString());
          Append.toString(j, v);
        });
        j.complete();
        return true;
      }
    },
    ST(ST.class) {
      @Override
      public boolean appendTo(final MultiLineJoiner mlj, final Object o) {
        final ST st = (ST) o;
        final ToStringBuilder ts = new ToStringBuilder(mlj, "ST");
        ts.add("name", st.getName());
        ts.add("attributes", st.getAttributes());
        ts.complete();
        return true;
      }
    },
    OBJECT(Object.class) {
      @Override
      public boolean appendTo(final MultiLineJoiner mlj, final Object o) {
        final boolean isNull = null == o;
        mlj.sb().append(isNull ? "null" : o);
        return !isNull;
      }
    };

    private static final Map<Class<?>, MultilineAppender> APPENDER_MAP;

    static {
      final Append[] appends = values();
      final STGroupType[] stGroupTypes = STGroupType.values();
      final Map<Class<?>, MultilineAppender> map = new LinkedHashMap<>(appends.length + stGroupTypes.length);
      for (Append e : appends) {
        if (e != OBJECT) {
          map.put(e.cls, e);
        }
      }
      for (STGroupType e : stGroupTypes) {
        map.put(e.stGroupClass, e);
      }
      APPENDER_MAP = map;
    }

    public static boolean toString(final MultiLineJoiner mlj, final Object o) {
      if (null == o) {
        return OBJECT.appendTo(mlj, null);
      }
      final Map<Class<?>, MultilineAppender> appenderMap = APPENDER_MAP;
      final MultilineAppender appender;
      synchronized (appenderMap) {
        appender = appenderMap.computeIfAbsent(o.getClass(), k -> {
          for (Map.Entry<Class<?>, MultilineAppender> e : appenderMap.entrySet()) {
            if (e.getKey().isAssignableFrom(k)) {
              return e.getValue();
            }
          }
          return OBJECT;
        });
      }
      return appender.appendTo(mlj, o);
    }

    public final Class<?> cls;

    Append(Class<?> cls) {
      this.cls = cls;
    }
  }
}
