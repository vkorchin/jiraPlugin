package streaming;

import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Ensures tasks for the same key are executed serially, while allowing parallelism across keys.
 */
public final class PerKeySerialExecutor<K> implements AutoCloseable {
    private final ExecutorService backingPool;
    private final Map<K, KeyQueue> queues = new ConcurrentHashMap<>();

    public PerKeySerialExecutor(int parallelism) {
        if (parallelism <= 0) throw new IllegalArgumentException("parallelism must be > 0");
        this.backingPool = Executors.newFixedThreadPool(parallelism);
    }

    public void execute(K key, Runnable task) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(task, "task");
        KeyQueue keyQueue = queues.computeIfAbsent(key, k -> new KeyQueue());
        keyQueue.enqueue(task);
    }

    @Override
    public void close() {
        backingPool.shutdown();
    }

    private final class KeyQueue {
        private final Queue<Runnable> queue = new ConcurrentLinkedQueue<>();
        private final AtomicBoolean running = new AtomicBoolean(false);

        void enqueue(Runnable task) {
            queue.add(task);
            schedule();
        }

        private void schedule() {
            if (!running.compareAndSet(false, true)) {
                return; // already scheduled
            }
            try {
                backingPool.execute(this::drain);
            } catch (RejectedExecutionException ex) {
                running.set(false);
                throw ex;
            }
        }

        private void drain() {
            try {
                for (;;) {
                    Runnable r = queue.poll();
                    if (r == null) {
                        return;
                    }
                    try {
                        r.run();
                    } catch (Throwable t) {
                        // swallow to keep draining; production system should log
                    }
                }
            } finally {
                running.set(false);
                if (!queue.isEmpty()) {
                    schedule();
                }
            }
        }
    }
}