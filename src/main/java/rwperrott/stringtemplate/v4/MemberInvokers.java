package rwperrott.stringtemplate.v4;

import java.util.List;

/**
 * Provided, so don't have to have an extended ArrayList in another object, to prevent modification.
 *
 * @author rwperrott
 */
interface MemberInvokers {
  MemberInvokers NONE = new MemberInvokers() {
    @Override
    public int maxTypeConverterCount() {
      return 0;
    }

    @Override
    public MemberInvoker find(final boolean onlyPublic, final Class<?> returnType, final List<Object> args, final int extrasLen) {
      return null;
    }
  };

  /**
   * Used by AbstractInvokeAdaptor
   */
  int maxTypeConverterCount();

  default MemberInvoker find(final boolean onlyPublic,
                             final Class<?> returnType,
                             final List<Object> args) {
    return find(onlyPublic, returnType, args, 0);
  }

  /**
   * Used by AbstractInvokeAdaptor and StringInvokeRenderer
   */
  MemberInvoker find(final boolean onlyPublic,
                     final Class<?> returnType,
                     final List<Object> args,
                     final int extrasLen);
}
