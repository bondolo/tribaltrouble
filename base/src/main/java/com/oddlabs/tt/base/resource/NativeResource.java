package com.oddlabs.tt.base.resource;

import java.lang.ref.Cleaner;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jspecify.annotations.Nullable;

/// Base class for objects holding native state not automatically reclaimed by the garbage collector.
///
/// @param <R> native state type
public abstract class NativeResource<R extends NativeResource.NativeState> implements AutoCloseable {

  private static final Logger logger = Logger.getLogger(NativeResource.class.getSimpleName());
  private static final Cleaner cleaner = Cleaner.create();

  // Cleanup tasks executed on the thread owning the required native context (e.g., OpenGL context) via
  // processCleanupTasks().
  private static final Queue<Runnable> cleanupTasks = new ConcurrentLinkedQueue<>();

  private static volatile @Nullable Consumer<String> errorChecker = null;

  public static void setErrorChecker(@Nullable Consumer<String> checker) {
    errorChecker = checker;
  }

  /// Enqueues a cleanup task to run on the context-owning thread, avoiding missing-context errors when triggered by the
  /// garbage collector `Cleaner`.
  public static void addCleanupTask(Runnable task) {
    cleanupTasks.add(task);
  }

  /// Executes all pending cleanup tasks; must be called on the thread holding the required native context.
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

  /// Holds native state for a resource.
  public abstract static class NativeState implements AutoCloseable, Runnable {
    // Count of unfinalized native resources.
    static final AtomicInteger count = new AtomicInteger(0);

    protected NativeState() {
      count.incrementAndGet();
    }

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
