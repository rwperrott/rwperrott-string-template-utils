package rwperrott.stringtemplate.v4;

import lombok.NonNull;
import org.stringtemplate.v4.STGroup;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Provided because StringTemplate annoyingly doesn't directly support arithmetic and doesn't support numeric
 * conditionals!
 * <br/>
 * Probably useful for variable array lengths, where the array length or offset length needs to change a template's
 * behaviour to remove the stupid need for multiple, even a matrix of, similar templates!
 * <br/>
 * Math Aliases:
 * <ul>
 *     <li>addX -> addExact</li>
 *     <li>decX -> decrementExact</li>
 *     <li>intX -> toIntExact</li>
 *     <li>mulX -> multiplyExact</li>
 *     <li>negX -> negateExact</li>
 *     <li>subX -> subtractExact</li>
 * </ul>
 * NumberFunction and BigInteger Aliases:
 * <ul>
 *     <li>dec -> decrement</li>
 *     <li>div -> divide</li>
 *     <li>mod -> modulus</li>
 *     <li>mul -> multiply</li>
 *     <li>neg -> negate</li>
 *     <li>shl -> shiftLeft</li>
 *     <li>shr -> shiftRight</li>
 *     <li>shru -> shiftRightUnsigned</li>
 *     <li>sub -> subtract</li>
 * </ul>
 *
 * @author rwperrott
 */
@SuppressWarnings("unused")
public final class NumberInvokeAdaptor extends AbstractInvokeAdaptor<Number> {
  static {
    NumberFunctions.registerNumberAdapterFunctions();
  }

  public static void register(final @NonNull STGroup stGroup) {
    final NumberInvokeAdaptor a = new NumberInvokeAdaptor();
    stGroup.registerModelAdaptor(Number.class, a);
    stGroup.registerModelAdaptor(Byte.class, a);
    stGroup.registerModelAdaptor(Short.class, a);
    stGroup.registerModelAdaptor(Integer.class, a);
    stGroup.registerModelAdaptor(Long.class, a);
    stGroup.registerModelAdaptor(Float.class, a);
    stGroup.registerModelAdaptor(Double.class, a);
    stGroup.registerModelAdaptor(BigInteger.class, a);
    stGroup.registerModelAdaptor(BigDecimal.class, a);
  }

  public NumberInvokeAdaptor() {
    super(true); // Probably not a good idea to access non-public Members of a Number.
  }

  @Override
  protected String toAlias(final String name) {
    switch (name) {
      // java.lang.Math methods
      case "addX":
        return "addExact";
      case "incX":
      case "inc":
      case "increment":
        return "incrementExact";
      case "decX":
      case "dec":
      case "decrement":
        return "decrementExact";
      case "intX":
        return "toIntExact";
      case "mulX":
      case "mul":
        return "multiplyExact";
      case "negX":
        return "negateExact";
      case "subX":
        return "subtractExact";
      // NumberFunction methods
      case "div":
        return "divide";
      case "neg":
        return "negate";
      case "shl":
        return "shiftLeft";
      case "shr":
        return "shiftRight";
      case "shru":
        return "shiftRightUnsigned";
      case "sub":
        return "subtract";
      default:
        return name;
    }
  }
}
