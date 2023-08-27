package rwperrott.stringtemplate.v4;

import org.stringtemplate.v4.STErrorListener;
import org.stringtemplate.v4.misc.STMessage;

import java.util.function.BiConsumer;

/**
 * Base interface for Group and Template
 *
 * @author rwperrott
 */
@SuppressWarnings("unused")
public interface STErrorConsumer extends STErrorListener, BiConsumer<String, STMessage> {
  default void compileTimeError(STMessage msg) {
    accept("ST CompileTime", msg);
  }

  /**
   * Don't ignore ErrorType.NO_SUCH_PROPERTY, it maybe an important warning!
   */
  default void runTimeError(STMessage msg) {
    accept("ST Runtime", msg);
  }

  default void IOError(STMessage msg) {
    accept("ST IO", msg);
  }

  default void internalError(STMessage msg) {
    accept("ST Internal", msg);
  }
}
