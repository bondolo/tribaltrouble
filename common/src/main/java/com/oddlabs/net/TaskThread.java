package com.oddlabs.net;

import com.oddlabs.event.Deterministic;
import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Executes asynchronous tasks on virtual threads and dispatches completed results back to the caller's thread.
 */
public final class TaskThread implements AutoCloseable {
    private final Map<Integer, TaskCallback<?>> id_to_callback = new ConcurrentHashMap<>();
    private final Queue<BlockingTask> finished_tasks = new ConcurrentLinkedQueue<>();
    private final AtomicInteger current_id = new AtomicInteger();
    private final @Nullable Runnable notification_action;
    private final Deterministic deterministic;
    private final @Nullable ExecutorService executor;
    private volatile boolean finished;

    public TaskThread(Deterministic deterministic, @Nullable Runnable notification_action) {
        this.deterministic = deterministic;
        this.notification_action = notification_action;
        this.executor = deterministic.isPlayback() ? null : Executors.newVirtualThreadPerTaskExecutor();
    }

    static final class BlockingTask implements Task, Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        final int id;
        volatile boolean cancelled;
        @Nullable
        TaskResult<?> result;
        private transient @Nullable Future<?> future;

        BlockingTask(int id) {
            this.id = id;
        }

        void setFuture(Future<?> future) {
            this.future = future;
        }

        @Override
        public void cancel() {
            cancelled = true;
            Future<?> f = future;
            if (f != null) {
                f.cancel(true);
            }
        }
    }

    sealed interface TaskResult<T> extends Serializable
            permits TaskFailed, TaskSucceeded {
        void deliverResult(TaskCallback<T> callback);
    }

    record TaskFailed<T>(Throwable result) implements TaskResult<T> {
        @Override
        public void deliverResult(TaskCallback<T> callback) {
            callback.taskFailed(result);
        }
    }

    record TaskSucceeded<T>(@Nullable T result) implements TaskResult<T> {
        @Override
        public void deliverResult(TaskCallback<T> callback) {
            callback.taskCompleted(result);
        }
    }

    public Deterministic getDeterministic() {
        return deterministic;
    }

    public <T> Task addTask(Callable<T> callable, TaskCallback<T> callback) {
        int task_id = current_id.getAndIncrement();
        id_to_callback.put(task_id, callback);
        BlockingTask task = new BlockingTask(task_id);
        if (executor != null && !finished) {
            Future<?> future = executor.submit(() -> runTask(task, callable));
            task.setFuture(future);
        }
        return task;
    }

    private void runTask(BlockingTask task, Callable<?> callable) {
        TaskResult<?> result;
        try {
            Object callable_result = callable.call();
            result = new TaskSucceeded<>(callable_result);
        } catch (Throwable t) {
            result = new TaskFailed<>(t);
        }
        if (finished) {
            return;
        }
        task.result = result;
        finished_tasks.add(task);
        if (notification_action != null) {
            notification_action.run();
        }
    }

    public void poll() {
        while (true) {
            BlockingTask task = deterministic.isPlayback() ? null : finished_tasks.poll();
            if (!deterministic.log(task != null)) {
                return;
            }
            task = deterministic.log(task);
            if (task == null) {
                return;
            }
            TaskCallback<?> callback = id_to_callback.remove(task.id);
            if (!task.cancelled && callback != null && task.result != null) {
                deliver(task.result, callback);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> void deliver(TaskResult<?> result, TaskCallback<?> callback) {
        ((TaskResult<T>) result).deliverResult((TaskCallback<T>) callback);
    }

    @Override
    public void close() {
        finished = true;
        if (executor != null) {
            executor.shutdown();
            // briefly wait for clean shutdown
            try {
                //noinspection ResultOfMethodCallIgnored
                executor.awaitTermination(10, TimeUnit.MILLISECONDS);
            } catch (InterruptedException _) {
                Thread.currentThread().interrupt();
            }
            if (!executor.isTerminated()) {
                // force shutdown on any remaining tasks
                executor.shutdownNow();
            }
        }
        id_to_callback.clear();
        finished_tasks.clear();
        if (notification_action != null) {
            notification_action.run();
        }
    }
}
