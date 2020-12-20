package rwperrott.stringtemplate.v4;

import lombok.NonNull;
import org.stringtemplate.v4.STGroup;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Provided because StringTemplate annoyingly doesn't directly support arithmetic and doesn't support numeric
 * conditionals!
 * <p>
 * This should be useful for variable array lengths, where the array length or offset length needs to change a templates
 * behaviour to remove the stupid need for multiple, even a matrix of, similar templates!
 * <p>
 * I don't care about the StringTemplate developer's purity arguments, we need pragmatism like this, to make
 * StringTemplate a lot more usable.
 */
@SuppressWarnings("unused")
public final class NumberInvokeAdaptor extends AbstractInvokeAdaptor<Number> {
    public NumberInvokeAdaptor() {
        super(true); // Probably not a good idea to access non-public Members of a Number
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

    @Override
    protected String toAlias(final String name) {
        switch (name) {
            case "decrement":
                return "dec"; // to StringFunctions::escapeURL
            case "divide":
                return "div"; // to StringFunctions::escapeURL
            case "increment":
                return "inc"; // to StringFunctions::escapeURL
            case "modulus":
                return "mod"; // to StringFunctions::escapeURL
            case "multiply":
                return "mul"; // to StringFunctions::escapeURL
            case "negate":
                return "neg"; // to StringFunctions::escapeURL
            case "subtract":
                return "sub"; // to StringFunctions::escapeURL
            default:
                return name;
        }
    }

    static {
        NumberFunctions.registerNumberAdapterFunctions();
    }
}
