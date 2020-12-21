package rwperrott.stringtemplate.v4;

import lombok.NonNull;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * No need for a pool library for simple memory-based stuff
 * @param <T>
 */
public final class SimplePool<T> {
    private final Queue<T> pool;
    private final Supplier<T> supplier;
    private final Consumer<T> recycler;
    private final Consumer<T> consumer;

    public SimplePool(final int capacity,
                      @NonNull final Supplier<T> supplier,
                      @NonNull final Consumer<T> recycler,
                      final Consumer<T> consumer) {
        // ArrayBlockingQueue makes this really easy.
        this.pool = new ArrayBlockingQueue<>(capacity);
        this.supplier = supplier;
        this.recycler = recycler;
        this.consumer = consumer;
    }

    public T remove() {
        final T t = pool.poll();
        return null == t ? supplier.get() : t;
   }

    public void offer(T t) {
        // Allow null, to support use of optional swap instance
        if (null == t)
            return;
        // Must recycle before offering!
        recycler.accept(t);
        if (!pool.offer(t))
            if (null != consumer)
                consumer.accept(t);
    }

    @SuppressWarnings("unused")
    @SafeVarargs
    public final void offer(T... a) {
        if (null == a)
            return;
        int i = a.length;
        while (--i >=0)
            offer(a[i]);
    }

    /**
     * Make one-shot use easy
     *
     * @param user .
     * @param <R> .
     * @return .
     */
    public <R> R use(Function<T,R> user) {
        final T t = remove();
        try {
            return user.apply(t);
        } finally {
            offer(t);
        }
    }
}
