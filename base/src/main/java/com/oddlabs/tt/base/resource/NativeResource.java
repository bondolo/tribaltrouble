package com.oddlabs.tt.base.resource;

import org.jspecify.annotations.Nullable;

import java.lang.ref.Cleaner;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A class with associated native state that is not reclaimed automatically by the garbage collector.
 *
 * @param <R> The native state type
 */
public abstract class NativeResource<R extends NativeResource.NativeState> implements AutoCloseable {

    private static final Logger logger = Logger.getLogger(NativeResource.class.getSimpleName());
    private static final Cleaner cleaner = Cleaner.create();

    /**
     * Queue for cleanup tasks to be executed on the app thread via {@link #processCleanupTasks()}. This is usually
     * the thread with a specific context such as the OpenGL context.
     */
    private static final Queue<Runnable> cleanupTasks = new ConcurrentLinkedQueue<>();
    private static volatile @Nullable Consumer<String> errorChecker = null;

    /**
     * Sets an error checker callback to check for errors after running a cleanup task.
     *
     * @param checker The error checker callback.
     */
    public static void setErrorChecker(@Nullable Consumer<String> checker) {
        errorChecker = checker;
    }

    /**
     * Adds an OpenGL cleanup task to a queue to be processed on the GL thread.
     * This is used to avoid "No OpenGL context found" errors when cleanup is triggered by the Cleaner.
     *
     * @param task The Runnable task to execute on the GL thread for cleanup.
     */
    public static void addCleanupTask(Runnable task) {
        cleanupTasks.add(task);
    }

    /**
     * Processes all pending cleanup tasks. This method must be called from the thread that has the appropriate context
     * current (e.g., the main rendering thread).
     */
    public static void processCleanupTasks() {
        Runnable task;
        while ((task = cleanupTasks.poll()) != null) {
            try {
                task.run();
                Consumer<String> checker = errorChecker;
                if (checker != null) {
                    checker.accept("After closing resource " + task.getClass().getSimpleName());
                }
            } catch (Throwable t) {
                logger.log(Level.SEVERE, "Error during cleanup task execution", t);
            }
        }
    }

    /**
     * Holds native state for a resource.
     */
    public static abstract class NativeState implements AutoCloseable, Runnable {
        /**
         * Count of unfinalized native resources
         */
        static final AtomicInteger count = new AtomicInteger(0);

        protected NativeState() {
            count.incrementAndGet();
        }

        /**
         * clean up native resource
         */
        @Override
        public abstract void close();

        @Override
        public final void run() {
            count.decrementAndGet();
            try {
                close();
            } catch (Throwable all) {
                logger.log(Level.WARNING, "Exception thrown in close()", all);
            }
        }
    }

    private final Cleaner.Cleanable cleanable;
    protected final R state;

    public NativeResource(R state) {
        this(state, NativeResource::addCleanupTask);
    }

    protected NativeResource(R state, Consumer<Runnable> cleanupStrategy) {
        this.state = state;
        this.cleanable = cleaner.register(this, () -> cleanupStrategy.accept(state));
    }

    @Override
    public void close() {
        cleanable.clean(); // execute the cleaning action immediately
    }

    public static int getCount() {
        return NativeState.count.get();
    }
}
