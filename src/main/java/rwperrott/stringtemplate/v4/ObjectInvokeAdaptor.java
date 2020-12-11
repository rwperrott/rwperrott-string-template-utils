package rwperrott.stringtemplate.v4;

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
