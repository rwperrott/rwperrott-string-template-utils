package rwperrott.stringtemplate.v4;

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
