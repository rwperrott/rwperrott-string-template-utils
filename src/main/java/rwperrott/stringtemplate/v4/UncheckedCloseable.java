package rwperrott.stringtemplate.v4;

public interface UncheckedCloseable extends AutoCloseable {
  @Override
  void close();
}
