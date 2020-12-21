package rwperrott.stringtemplate.v4;

import lombok.NonNull;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * No need for a pool library for simple memory-based stuff
 *
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

    /**
     * @param a can be null or contain null references, to allow for speculative use.
     */
    @SafeVarargs
    public final void offer(T... a) {
        if (null == a)
            return;
        int i = a.length;
        while (--i >= 0)
            offer(a[i]);
    }

    /**
     * Recycles objects, then offers to pool.
     * <p>
     * If offer fails and consumer not null, is given to consumer to depose of it.
     *
     * @param t .
     */
    public void offer(T t) {
        // Allow null, to support use of optional swap instance
        if (null == t)
            return;
        // Must recycle before offering!
        recycler.accept(t);
        if (!pool.offer(t) && null != consumer)
            consumer.accept(t);
    }

    /**
     * Make one-shot use easy
     *
     * @param user must not be null, accepts temporary resource and returns result
     * @param <R> type of result
     *
     * @return result
     */
    public <R> R use(@NonNull Function<T, R> user) {
        final T t = remove();
        try {
            return user.apply(t);
        } finally {
            offer(t);
        }
    }

    /**
     * Only polls pool or creates a new object.
     * <p>
     * It's pointless doing any re-processing on a pool object, because it's already been recycled by offer.
     *
     * @return resource
     */
    public T remove() {
        final T t = pool.poll();
        return null == t ? supplier.get() : t;
    }
}
