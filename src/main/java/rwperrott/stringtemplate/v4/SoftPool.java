package rwperrott.stringtemplate.v4;

import lombok.NonNull;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.*;

/**
 * Based upon LinkedBlockingQueue, commons-pool PooledObjectFactory, and WeakHashMap.
 *
 * @param <E> resource type.
 * @author rwperrott
 */
@SuppressWarnings("unused")
public final class SoftPool<E> implements Supplier<E>, UnaryOperator<E>, Consumer<E> {
    private final int capacity;
    private final AtomicInteger count = new AtomicInteger();
    private final Supplier<E> supplier;
    private final Predicate<E> passivator;
    private final Predicate<E> activator;
    private final Consumer<E> destroyer;
    private final ReferenceQueue<E> refQueue;
    private final ReentrantLock getLock = new ReentrantLock();
    private final ReentrantLock acceptLock = new ReentrantLock();
    private Node<E> head;
    private Node<E> last;

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

        final AtomicInteger count0 = this.count;
        if (count0.get() == 0)
            return supplier.get();
        //
        final ReentrantLock getLock0 = this.getLock;
        getLock0.lock();
        try {
            // Look for a non-null SoftReference value.
            while (count0.get() > 0) {
                //dequeue
                final Node<E> h = head;
                final Node<E> first = h.nextNode;
                h.nextNode = h; // help GC
                head = first;
                final E x = first.get();
                first.clear();
                count0.getAndDecrement();
                if (null != x) {
                    if (null == activator || activator.test(x))
                        return x;
                    if (null != destroyer)
                        destroyer.accept(x);
                }
            }
        } finally {
            getLock0.unlock();
        }
        return supplier.get();
    }

    /**
     * Could be called by a regular cleanup task.
     */
    public void cleanup() {
        getLock.lock();
        try {
            if (null != refQueue)
                for (Reference<? extends E> x; (x = refQueue.poll()) != null; ) {
                    final E e = x.get();
                    if (null == e)
                        continue;
                    destroyer.accept(x.get());
                    x.clear();
                }
        } finally {
            getLock.unlock();
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
        final AtomicInteger count0 = this.count;
        if (count0.get() == capacity)
            return;
        //
        final ReentrantLock acceptLock0 = this.acceptLock;
        acceptLock0.lock();
        try {
            if (count0.get() < capacity)
                last = last.nextNode = null == refQueue
                        ? new Node<>(e)
                        : new Node<>(e, refQueue);
        } finally {
            acceptLock0.unlock();
        }
    }

    /**
     * Atomically depose of all the elements in this pool.
     */
    public void clear() {
        acceptLock.lock();
        getLock.lock();
        try {
            for (Node<E> p, h = head; (p = h.nextNode) != null; h = p) {
                h.nextNode = h;
                p.clear();
            }
            head = last;
        } finally {
            getLock.unlock();
            acceptLock.unlock();
        }
    }

    // SoftReference extended as a single linked-list node.
    static final class Node<E> extends SoftReference<E> {
        Node<E> nextNode;

        Node(E x) {
            super(x);
        }

        Node(E x, ReferenceQueue<? super E> q) {
            super(x, q);
        }
    }

    @SuppressWarnings("unused")
    public static final class Builder<E> {
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