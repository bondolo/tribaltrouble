package com.oddlabs.tt.base.util;


import java.util.function.Supplier;

/**
 * Functional interface for progress reporting during map loading and generation.
 */
@FunctionalInterface
public interface ProgressListener {
    /**
     * Listener instance that ignores progress notifications.
     */
    ProgressListener NONE = _ -> {
    };

    /**
     * Scoped value containing the active {@link ProgressListener} for the current execution context.
     */
    ScopedValue<ProgressListener> CURRENT = ScopedValue.newInstance();

    /**
     * Returns the active progress listener in the current scope, or {@link #NONE} if none is bound.
     *
     * @return the active progress listener
     */
    static ProgressListener current() {
        return CURRENT.orElse(NONE);
    }

    /**
     * Notifies the current progress listener of progress with a default step.
     */
    static void progress() {
        CURRENT.orElse(NONE).onProgress();
    }

    /**
     * Notifies the current progress listener of progress with the given step or fraction.
     *
     * @param step the progress step or advance amount
     */
    static void progress(float step) {
        CURRENT.orElse(NONE).onAdvance(step);
    }

    /**
     * Advances progress by the given delta amount.
     *
     * @param delta the advance delta amount
     */
    static void advance(float delta) {
        CURRENT.orElse(NONE).onAdvance(delta);
    }

    /**
     * Sets absolute progress within the current scope.
     *
     * @param fraction the absolute progress fraction (0.0 to 1.0)
     */
    static void set(float fraction) {
        CURRENT.orElse(NONE).onProgress(fraction);
    }

    /**
     * Executes a runnable operation within the context of the specified {@link ProgressListener}.
     *
     * @param listener the progress listener to bind
     * @param operation the operation to execute
     */
    static void run(ProgressListener listener, Runnable operation) {
        ScopedValue.where(CURRENT, listener).run(operation);
    }

    /**
     * Executes a supplier within the context of the specified {@link ProgressListener}.
     *
     * @param listener the progress listener to bind
     * @param supplier the supplier to compute
     * @param <T> the return type
     * @return the computed value
     */
    static <T> T supply(ProgressListener listener, Supplier<T> supplier) {
        var ref = new Object() {
            T value;
        };
        ScopedValue.where(CURRENT, listener).run(() -> ref.value = supplier.get());
        return ref.value;
    }

    /**
     * Executes a callable operation within the context of the specified {@link ProgressListener}.
     *
     * @param listener the progress listener to bind
     * @param operation the callable operation to execute
     * @param <T> the return type
     * @param <X> the exception type
     * @return the result of the callable operation
     * @throws X if unable to compute a result
     */
    static <T, X extends Throwable> T call(ProgressListener listener,
            ScopedValue.CallableOp<T, X> operation) throws X {
        return ScopedValue.where(CURRENT, listener).call(operation);
    }

    /**
     * Creates a scoped sub-task listener allocating a portion of progress with the given weight.
     *
     * @param weight the weight fraction of the parent task
     * @return the sub-task progress listener
     */
    default ProgressListener subTask(float weight) {
        return new SubTask(this, weight);
    }

    /**
     * Executes an operation as a scoped sub-task allocated the given weight of the current task.
     *
     * @param weight the weight fraction of the sub-task (e.g. 0.25f for 25%)
     * @param operation the operation to execute
     */
    static void subTask(float weight, Runnable operation) {
        SubTask sub = new SubTask(current(), weight);
        try {
            ScopedValue.where(CURRENT, sub).run(operation);
        } finally {
            sub.complete();
        }
    }

    /**
     * Executes a supplier as a scoped sub-task allocated the given weight of the current task.
     *
     * @param weight the weight fraction of the sub-task
     * @param supplier the supplier to execute
     * @param <T> the return type
     * @return the result of the supplier
     */
    static <T> T subTask(float weight, Supplier<T> supplier) {
        SubTask sub = new SubTask(current(), weight);
        var ref = new Object() {
            T value;
        };
        try {
            ScopedValue.where(CURRENT, sub).run(() -> ref.value = supplier.get());
        } finally {
            sub.complete();
        }
        return ref.value;
    }

    void onProgress(float fraction);

    default void onAdvance(float delta) {
        onProgress(delta);
    }

    default void onProgress() {
    }

    final class SubTask implements ProgressListener {
        private final ProgressListener parent;
        private final float weight;
        private float currentFraction;

        public SubTask(ProgressListener parent, float weight) {
            this.parent = parent;
            this.weight = weight;
        }

        @Override
        public void onProgress(float fraction) {
            float clamped = Math.clamp(fraction, 0f, 1f);
            float delta = clamped - currentFraction;
            if (delta > 0f) {
                currentFraction = clamped;
                parent.onAdvance(delta * weight);
            }
        }

        @Override
        public void onAdvance(float delta) {
            if (delta > 0f) {
                onProgress(currentFraction + delta);
            }
        }

        public void complete() {
            if (currentFraction < 1f) {
                onProgress(1f);
            }
        }
    }
}
