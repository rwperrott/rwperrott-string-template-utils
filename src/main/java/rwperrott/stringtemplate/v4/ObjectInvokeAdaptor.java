package rwperrott.stringtemplate.v4;

import lombok.NonNull;
import org.stringtemplate.v4.STGroup;

/**
 * An alternative to ObjectAdapter
 *
 * @author rwperrott
 */
@SuppressWarnings("unused")
public final class ObjectInvokeAdaptor extends AbstractInvokeAdaptor<Object> {
  static {
    ObjectFunctions.registerAdapterFunctions();
  }

  public static void register(final @NonNull STGroup stGroup) {
    stGroup.registerModelAdaptor(Object.class, new ObjectInvokeAdaptor());
  }

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
}
