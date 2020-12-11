package rwperrott.stringtemplate.v4;

/**
 * Provided because StringTemplate annoyingly doesn't directly support arithmetic and doesn't support numeric
 * conditionals!
 *
 * This should be useful for variable array lengths, where the array length or offset length needs to change a templates
 * behaviour to remove the stupid need for multiple, even a matrix of, similar templates!
 *
 * I don't care about the StringTemplate developer's purity arguments, we need pragmatism like this, to make
 * StringTemplate a lot more usable.
 */
@SuppressWarnings("unused")
public final class NumberInvokeAdaptor extends AbstractInvokeAdaptor<Number> {
    public NumberInvokeAdaptor() {
        super(true); // Probably not a good idea to access non-public Members of a Number
    }

    static {
        NumberFunctions.registerNumberAdapterFunctions();
    }
}
