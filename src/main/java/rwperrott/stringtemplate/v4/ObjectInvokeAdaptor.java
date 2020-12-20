package rwperrott.stringtemplate.v4;

import lombok.NonNull;
import org.stringtemplate.v4.STGroup;

/**
 * An alternative to ObjectAdapter
 */
@SuppressWarnings("unused")
public final class ObjectInvokeAdaptor extends AbstractInvokeAdaptor<Object> {
    public ObjectInvokeAdaptor(final boolean onlyPublic) {
        super(onlyPublic);
    }

    /**
     * onlyPublic = true
     */
    public ObjectInvokeAdaptor() {
        super(true);
    }

    public static void register(final @NonNull STGroup stGroup) {
        stGroup.registerModelAdaptor(Object.class, new ObjectInvokeAdaptor());
    }

    @Override
    protected String toAlias(final String name) {
        switch (name) {
            case "asList":
                return "toList";
            case "asSortedList":
                return "toSortedList";
            case "asSet":
                return "toSet";
            case "asSortedSet":
                return "toSortedSet";
            default:
                return name;
        }
    }

    static {
        ObjectFunctions.registerAdapterFunctions();
    }
}
