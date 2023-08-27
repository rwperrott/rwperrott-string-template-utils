package rwperrott.stringtemplate.v4;

import lombok.NonNull;
import org.stringtemplate.v4.Interpreter;
import org.stringtemplate.v4.ModelAdaptor;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.misc.STNoSuchPropertyException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.function.UnaryOperator;

/**
 * Provides most/all the functionality to create powerful ModelAdapters. These can use an object's fields
 * and use parameterised instance/static methods, plus static methods in any other registered class.
 * <br/>
 * Uses cached unreflected MethodHandles to invoke Field getters and Methods, without the
 * redundant Security costs of calling invoke on Field or Method Member objects.
 *
 * @param <T> type adapted
 * @author rwperrott
 */
public abstract class AbstractInvokeAdaptor<T> implements ModelAdaptor<T> {

  /**
   * Calls apply(property) on ArgsAdaptor.
   */
  private static final ModelAdaptor<ArgsAdaptor> ARGS_ADAPTER_MODEL_ADAPTER =
    (interpreter, self, model, property, propertyName) -> model.apply(property);

  private static void registerArgsAdapterModelAdapter(final ST self) {
    // This has to be checked every damned time, because maybe a new STGroup,
    // so, it can't be cached!
    final STGroup stg = self.groupThatCreatedThisInstance;
    if (ARGS_ADAPTER_MODEL_ADAPTER != stg.getModelAdaptor(ArgsAdaptor.class))
      stg.registerModelAdaptor(ArgsAdaptor.class, ARGS_ADAPTER_MODEL_ADAPTER);
  }
  private final boolean onlyPublic;

  protected AbstractInvokeAdaptor(final boolean onlyPublic) {
    this.onlyPublic = onlyPublic;
  }

  /**
   *
   * @return result, or ArgsAdaptor for its own ModelAdapter.
   * @throws NullPointerException when cls is null.
   * @throws STNoSuchPropertyException when can't find property in cls.
   */
  @Override
  public final Object getProperty(final Interpreter interpreter,
                                  final ST self,
                                  final T model,
                                  final Object property,
                                  final String propertyName)
    throws STNoSuchPropertyException {
    Objects.requireNonNull(model, "o");

    // Set here just-in-case toAlias(propertyName) fails.
    String alias = propertyName;

    // Provided by sub-class
    try {
      final Class<?> cls = model.getClass();
      if (null == cls)
        throw new ClassNotFoundException();
      alias = toAlias(propertyName);
      if (null == alias)
        alias = propertyName;
      final MemberInvokers mis = TypeFunctions.get(cls, alias);
      if (mis.maxTypeConverterCount() == 0) {
        // A property, or a method with no parameters.
        final List<Object> args = Collections.emptyList();
        final MemberInvoker invoker = mis.find(onlyPublic, Object.class, args);
        if (null == invoker) {
          TypeFunctions.get(cls, alias);
          throw new IllegalArgumentException("No matching field, method, or static method found");
        }
        return invoker.invoke(model, args);
      }

      registerArgsAdapterModelAdapter(self);

      // Wraps model in an ArgsAdapter, which will do chained property parsing, via COMPOSITE_MODEL_ADAPTER.
      return new ArgsAdaptor(interpreter, self, model, alias, onlyPublic, Object.class, mis);
    } catch (Throwable t) {
        throw STExceptions.noSuchPropertyInObject(model, propertyName.equals(alias)
                                                         ? propertyName : propertyName + "/" + alias, t);
    }
  }

  /**
   * @param name
   * @return null or an alias name
   */
  protected String toAlias(String name) {
    return null;
  }

  /**
   * Only created if memberInvokers.maxTypeConverterCount() more than zero.
   * <br/>
   * Carries Interpreter and ST, so can resolve excess properties, via requested ModelAdapter.
   */
  private static final class ArgsAdaptor implements UnaryOperator<Object> {
    /**
     * Called by `invoke` to build STExceptions.noSuchPropertyInObject `property` String.
     * @param i start index in args
     * @param n end index in args
     */
    private static String join(final Object result,
                               final List<Object> args, int i, final int n) {
      final StringJoiner sj = new StringJoiner(".");
      add(sj, result);
      while (i < n)
        add(sj, args.get(i++));
      return sj.toString();
    }

    /**
     * Called by `join` to add a value as a String.
     */
    private static void add(final StringJoiner sj, final Object value) {
      if (value instanceof CharSequence)
        // Add ("string")
        sj.add(String.format("(\"%s\")", value));
      else
        // Add {type:"string"}
        sj.add(String.format("{%s:\"%s\"}", value.getClass().getSimpleName(), value));
    }

    private final Interpreter interpreter;
    private final ST self;
    private final boolean onlyPublic;
    private final Class<?> returnType;
    private final Object value;
    private final String propertyName;
    private final MemberInvokers memberInvokers;
    private final ArrayList<Object> args = new ArrayList<>();
    /**
     * Used by last `invoke`.
     */
    private MemberInvoker latestMatchingInvoker;

    @SuppressWarnings("SameParameterValue")
    private ArgsAdaptor(final Interpreter interpreter,
                        final ST self,
                        final Object value,
                        final String propertyName,
                        final boolean onlyPublic,
                        final Class<?> returnType,
                        final MemberInvokers memberInvokers) {
      this.interpreter = interpreter;
      this.self = self;
      this.onlyPublic = onlyPublic;
      this.returnType = returnType;
      this.value = value;
      this.propertyName = propertyName;
      this.memberInvokers = memberInvokers;

      // Allow for 0 to n args methods
      final MemberInvoker mi = memberInvokers.find(onlyPublic, returnType, args);
      if (null != mi)
        latestMatchingInvoker = mi; // The latest best match for a method/property name
    }

    /**
     * @return this or invoke() result
     */
    @Override // Implements UnaryOperator<Object>
    public Object apply(final @NonNull Object property) {
      args.add(property);
      // This must be done each time to find the best matching method without, or with any parameters.
      final MemberInvoker mi = memberInvokers.find(onlyPublic, returnType, args);
      if (null != mi) // Will be null for parameters
        latestMatchingInvoker = mi;
      return args.size() < memberInvokers.maxTypeConverterCount()
        ? this
        : invoke(); // No matches after this, so use latestMatchingInvoker
    }

    // Called by apply or toString.
    @SuppressWarnings({"rawtypes", "unchecked"})
    private Object invoke() {
      final int n = args.size();
      if (null == latestMatchingInvoker)
        throw STExceptions.noSuchPropertyInObject(value, join(propertyName, args, 0, n),
                                                  new IllegalArgumentException("No matching method found"));
      Object result = null;
      int i = 0;
      try {
        // Resolve this for property.
        result = latestMatchingInvoker.invoke(value, args);

        // Resolve excess properties:
        //    call getModelAdapter and getProperty, to part/fully resolving property.
        // Unresolved properties could be other ArgsAdaptor instances!
        final STGroup stg = self.groupThatCreatedThisInstance;
        i = latestMatchingInvoker.typeConverterCount();
        while (i < n) {
          final Object arg = args.get(i++);
          final ModelAdaptor ma = stg.getModelAdaptor(arg.getClass()); // Assume never null
          result = ma.getProperty(interpreter, self, result, arg, arg.toString());
        }
        return result;
      } catch (Throwable t) { // Catch failure of latestMatchingInvoker.invoke or later failure.
        throw STExceptions.noSuchPropertyInObject(value, join(result, args, i, n), t);
      }
    }

    /**
     * Must call invoke because maybe no more properties.
     */
    public String toString() {
      return invoke().toString();
    }
  }
}
