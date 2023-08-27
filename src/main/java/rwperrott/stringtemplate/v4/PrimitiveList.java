package rwperrott.stringtemplate.v4;

import java.lang.reflect.Array;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

/**
 * A List facade for a primitive arrays.
 *
 * @param <E>
 */
public class PrimitiveList<E> extends AbstractList<E> {
  private final Object array;

  public PrimitiveList(Object array) {
    this.array = array;
  }

  @Override
  public int size() {
    return Array.getLength(array);
  }

  @SuppressWarnings("unchecked")
  @Override
  public E get(int index) {
    return (E) Array.get(array, index);
  }

  @Override
  public E set(int index, E element) {
    E oldValue = get(index);
    Array.set(array, index, element);
    return oldValue;
  }


  @Override
  public int indexOf(Object o) {
    int n = size();
    if (o == null) {
      for (int i = 0; i < n; i++)
        if (get(i) == null)
          return i;
    } else {
      for (int i = 0; i < n; i++)
        if (o.equals(get(i)))
          return i;
    }
    return -1;
  }

  @Override
  public boolean contains(Object o) {
    return indexOf(o) != -1;
  }

  @Override
  public void forEach(Consumer<? super E> action) {
    Objects.requireNonNull(action);
    for (E e : this) {
      action.accept(e);
    }
  }

  @Override
  public void replaceAll(UnaryOperator<E> operator) {
    Objects.requireNonNull(operator);
    final int n = size();
    for (int i = 0; i < n; i++) {
      set(i, operator.apply(get(i)));
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public void sort(Comparator<? super E> c) {
    final int n = size();
    E[] array0 = (E[]) new Object[n];
    for (int i = 0; i < n; i++) {
      array0[i] = get(i);
    }
    Arrays.sort(array0, c);
    for (int i = 0; i < n; i++) {
      set(i, array0[i]);
    }
  }
}
