package rwperrott.stringtemplate.v4;

import lombok.NonNull;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.*;

/**
 * Based upon LinkedBlockingQueue and commons-pool PooledObjectFactory
 *
 * @param <E> resource type.
 */
public class SoftPool<E> implements Supplier<E>, UnaryOperator<E>, Consumer<E> {
    private final int capacity;
    private final AtomicInteger count = new AtomicInteger();
    private final Supplier<E> supplier;
    private final Predicate<E> passivator;
    private final Predicate<E> activator;
    private final Consumer<E> destroyer;
    private final ReferenceQueue<E> refQueue;
    private final ReentrantLock takeLock = new ReentrantLock();
    private final ReentrantLock putLock = new ReentrantLock();
    transient Node<E> head;
    private transient Node<E> last;

    private SoftPool(int capacity,
                     final Supplier<E> supplier,
                     final Predicate<E> passivator,
                     final Predicate<E> activator,
                     final Consumer<E> destroyer) {
        if (capacity <= 0) throw new IllegalArgumentException();
        this.capacity = capacity;
        this.supplier = supplier;
        this.passivator = passivator;
        this.activator = activator;
        this.destroyer = destroyer;
        last = head = new Node<>(null);
        refQueue = null == activator
                   ? null
                   : new ReferenceQueue<>();
    }

    /**
     * Do like accept(e), get(), to get or reuse a resource
     * <p>
     * Useful for clear-able builder objects, like StringBuilder.
     *
     * @param e null or resource
     *
     * @return resource
     */
    @Override
    public E apply(final E e) {
        if (null != e
            && (null == passivator || passivator.test(e))
            && (null == activator || activator.test(e)))
            return e;
        return get();
    }

    /**
     * Provide resource
     *
     * @return resource
     */
    public E get() {
        cleanup();

        final AtomicInteger count = this.count;
        if (count.get() == 0)
            return supplier.get();
        //
        final ReentrantLock takeLock = this.takeLock;
        takeLock.lock();
        try {
            // Look for a non-null SoftReference value.
            while (count.get() > 0) {
                //dequeue
                final Node<E> h = head;
                final Node<E> first = h.next;
                h.next = h; // help GC
                head = first;
                final Reference<E> ref = first.item;
                first.item = null;
                count.getAndDecrement();
                final E x = ref.get();
                if (null != x) {
                    if (null == activator || activator.test(x))
                        return x;
                    if (null != destroyer)
                        destroyer.accept(x);
                }
            }
        } finally {
            takeLock.unlock();
        }
        return supplier.get();
    }

    /**
     * Could be called by a regular cleanup task.
     */
    public void cleanup() {
        takeLock.lock();
        try {
            if (null != refQueue)
                for (Reference<? extends E> x; (x = refQueue.poll()) != null; ) {
                    synchronized (refQueue) {
                        destroyer.accept(x.get());
                        x.clear();
                    }
                }
        } finally {
            takeLock.unlock();
        }
    }

    /**
     * Allow some code to temporarily use resource.
     * <p>
     * Useful for clear-able builder objects, like StringBuilder.
     *
     * @param user .
     */
    public <R> R use(final Function<E, R> user) {
        final E e = get();
        try {
            return user.apply(e);
        } finally {
            accept(e);
        }
    }

    /**
     * Accept returned resource, and keep or depose of it.
     *
     * @param e resource
     */
    public void accept(E e) {
        if (null == e)
            return;
        //
        if (null != passivator && !passivator.test(e)) {
            if (null != destroyer)
                destroyer.accept(e);
            return;
        }
        //
        final AtomicInteger count = this.count;
        if (count.get() == capacity)
            return;
        //
        final ReentrantLock putLock = this.putLock;
        putLock.lock();
        try {
            if (count.get() < capacity)
                last = last.next = new Node<>(null == refQueue
                                              ? new SoftReference<>(e)
                                              : new SoftReference<>(e, refQueue));
        } finally {
            putLock.unlock();
        }
    }

    /**
     * Atomically depose of all the elements in this pool.
     */
    public void clear() {
        putLock.lock();
        takeLock.lock();
        try {
            for (Node<E> p, h = head; (p = h.next) != null; h = p) {
                h.next = h;
                p.item = null;
            }
            head = last;
        } finally {
            takeLock.unlock();
            putLock.unlock();
        }
    }

    static class Node<E> {
        Reference<E> item;

        Node<E> next;

        Node(Reference<E> x) {
            item = x;
        }
    }

    public static class Builder<E> {
        private final Supplier<E> supplier;
        private int capacity = Integer.MAX_VALUE;
        private Predicate<E> passivator;
        private Predicate<E> activator;
        private Consumer<E> destroyer;

        private Builder(@NonNull final Supplier<E> supplier) {
            this.supplier = supplier;
        }

        public Builder<E> capacity(final int capacity) {
            this.capacity = capacity;
            return this;
        }

        public Builder<E> passivator(@NonNull final Predicate<E> passivator) {
            this.passivator = passivator;
            return this;
        }

        public Builder<E> activator(@NonNull final Predicate<E> activator) {
            this.activator = activator;
            return this;
        }

        public Builder<E> destroyer(@NonNull final Consumer<E> destroyer) {
            this.destroyer = destroyer;
            return this;
        }

        public SoftPool<E> build() {
            if (capacity <= 0) throw new IllegalArgumentException("capacity: " + capacity);
            return new SoftPool<>(capacity, supplier, passivator, activator, destroyer);
        }
    }

    public static <E> Builder<E> builder(@NonNull Supplier<E> supplier) {
        return new Builder<>(supplier);
    }
}
