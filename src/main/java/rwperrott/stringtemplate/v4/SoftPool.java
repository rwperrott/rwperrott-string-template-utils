package rwperrott.stringtemplate.v4;

import lombok.*;
import lombok.experimental.Accessors;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * A Thread-safe soft-reference-based pooling class using a concurrent queue,
 * and functional references, for a far less bloaded pool implementation than commons-pool2.
 * <br/>
 * Loosely based upon LinkedBlockingQueue, commons-pool2 PooledObjectFactory, and WeakHashMap.
 *
 * @param <E> resource type.
 * @author rwperrott
 */
@SuppressWarnings({"unused", "SpellCheckingInspection"})
public final class SoftPool<E> implements CloseableSupplier<CloseableSupplier<E>> {
  /**
   * @param <E> resource type.
   */
  public static <E> Builder<E> builder(@NonNull Supplier<E> supplier) {
    return new Builder<>(supplier);
  }

  /**
   * Reset state of instance, e.g. StringBuilder.setLength(0).
   */
  private final Consumer<E> passivator; // For pool

  /**
   * Check if instance usable.
   */
  private final Predicate<E> validator; // For pool

  /**
   * Destroy instance.
   */
  private final Consumer<E> destroyer; // For pool

  private final AtomicInteger count = new AtomicInteger(); // For queue
  private final ReentrantLock pollLock = new ReentrantLock(); // For queue
  private final ReentrantLock offerLock = new ReentrantLock(); // For queue
  /**
   * Queue of stale instances, which must be destroyed, and left for GC.
   */
  private final ReferenceQueue<E> referenceQueue; // For SoftReference Nodes

  /**
   * The maximum pool capacity.
   */
  private int capacity;
  private Node<E> head; // For queue
  private Node<E> last; // For queue

  /**
   * Creates new instances of E.
   */
  private Supplier<E> supplier;

  private SoftPool(int capacity,
                   Supplier<E> supplier,
                   Consumer<E> passivator,
                   Predicate<E> validator,
                   Consumer<E> destroyer,
                   ReferenceQueue<E> staleReferences) {
    this.capacity = capacity;
    this.supplier = supplier;
    this.passivator = passivator;
    this.validator = validator;
    this.destroyer = destroyer;
    this.referenceQueue = staleReferences;
    last = head = new Node<>(null);
  }

  private void offer(E e) {
    cleanup();

    boolean accepted = false;
    try {
      final AtomicInteger count = this.count;
      if (validator.test(e) && count.get() < capacity) { // not full
        final ReentrantLock offerLock = this.offerLock;
        offerLock.lock();
        try {
          if (count.get() >= capacity) return; // full
          // No point calling passivator until we know it will be stored.
          passivator.accept(e);
          // enqueue like in LinkedBlocking Queue
          last = last.nextNode = new Node<>(e);
          count.getAndIncrement();
          accepted = true;
        } finally {
          offerLock.unlock();
        }
      }
    } finally {
      if (!accepted) destroyer.accept(e);
    }
  }

  private E poll() {
    cleanup();

    final AtomicInteger count = this.count;
    if (count.get() != 0) {
      final ReentrantLock pollLock = this.pollLock;
      pollLock.lock();
      try {
        while (count.get() != 0) {
          // dequeue like in LinkedBlocking Queue
          final Node<E> h = head;
          final Node<E> first = h.nextNode; // .next is for ReferenceQueue use
          h.nextNode = h; // help GC
          head = first;
          final E e = first.get();
          if (e != null) {
            first.clear();
            // An activator only seem useful, to update use state after validation.
            // So doesn't really look useful.
            if (validator.test(e)) return e;
            destroyer.accept(e);
          }
          count.getAndDecrement();
        }
      } finally {
        pollLock.unlock();
      }
    }

    return supplier.get();
  }

  /**
   * Get a valid instance of E from pool or supplier, wrapped in a CloseableSupplier.
   *
   * @return resource.
   */
  public CloseableSupplier<E> get() {
    return new Borrowed(poll());
  }

  /**
   * Main call to clean up all the expired resource soft references.
   * Thread-safe to call by external code.
   */
  public void cleanup() {
    if (null == referenceQueue)
      return;
    pollLock.lock();
    try {
      for (Reference<? extends E> x; (x = referenceQueue.poll()) != null; ) {
        final E e = x.get();
        if (null == e)
          continue;
        destroyer.accept(e);
        x.clear();
      }
    } finally {
      pollLock.unlock();
    }
  }

  /**
   * Dispose of all the elements in reference queue and pool.
   */
  public void close() {
    offerLock.lock();
    pollLock.lock();
    try {
      // Block new creation
      supplier = null;

      // Drop all returns
      capacity = 0;

      // Drop all returned
      for (Node<E> p, h = head; (p = h.nextNode) != null; h = p) {
        h.nextNode = h;
        p.clear();
      }
      head = last;
    } finally {
      pollLock.unlock();
      offerLock.unlock();
    }
    cleanup();
  }

  /**
   * SoftReference extended as a single linked-list node for the pool queue.
   *
   * @param <E>
   */
  private static final class Node<E> extends SoftReference<E> {
    private Node<E> nextNode;

    private Node(E x) {
      super(x);
    }
  }

  /**
   * Sadly the Project Lombok @Builder annotation does not provide enough functionality,
   * like validation and initialisation, so I had to create this class.
   *
   * @param <E> the interface/object type pooled.
   */
  @Accessors(fluent = true)
  public static final class Builder<E> {
    private static <E> Consumer<E> resolve(Consumer<E> o) {
      return null == o ? e -> {
      } : o;
    }

    private static <E> Predicate<E> resolve(Predicate<E> o) {
      return null == o ? e -> true : o;
    }

    /**
     * The supplier for new instances of E, the pooled resource.
     */
    @Getter
    private final Supplier<E> supplier;
    /**
     * The maximum pool capacity.
     */
    @Setter
    private int capacity = Integer.MAX_VALUE;
    /**
     * Converts an offered E into a passive state, and returns true if successful.
     * If true, it will be pushed onto the pool queue.
     */
    @Setter
    @NonNull
    private Consumer<E> passivator = e -> {
    };

    @Setter
    private Predicate<E> validator;

    private Consumer<E> destructorStub;
    @Setter
    private Consumer<E> destroyer;

    @Setter
    private boolean useReferenceQueue;

    /**
     * @param supplier creates new instances of E for the pool.
     */
    private Builder(@NonNull Supplier<E> supplier) {
      this.supplier = supplier;
    }

    public SoftPool<E> build() {
      if (capacity <= 0) throw new IllegalArgumentException("" + capacity);
      return new SoftPool<>(capacity,
                            supplier,
                            resolve(passivator),
                            resolve(validator),
                            resolve(destroyer),
                            useReferenceQueue ? new ReferenceQueue<>() : null);
    }
  }

  /**
   * This cannot be the same object as Node, to prevent memory leaks!
   */
  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  private final class Borrowed implements CloseableSupplier<E> {
    private E ref;

    @Override
    public E get() {
      return ref;
    }

    private E getAndClear() {
      E r = ref;
      ref = null;
      return r;
    }

    @Override
    public void close() {
      final E e = this.ref;
      this.ref = null;
      offer(e);
    }
  }
}