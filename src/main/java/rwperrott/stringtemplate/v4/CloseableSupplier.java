package rwperrott.stringtemplate.v4;

import java.util.function.Supplier;

public interface CloseableSupplier<E> extends Supplier<E>, UncheckedCloseable {
}
