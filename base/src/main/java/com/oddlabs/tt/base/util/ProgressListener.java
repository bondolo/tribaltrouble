package com.oddlabs.tt.base.util;

import org.jspecify.annotations.NonNull;

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
    static @NonNull ProgressListener current() {
        return CURRENT.orElse(NONE);
    }

    /**
     * Notifies the current progress listener of progress with a default step.
     */
    static void progress() {
        CURRENT.orElse(NONE).onProgress();
    }

    /**
     * Notifies the current progress listener of progress with the given step amount.
     *
     * @param step the progress step amount
     */
    static void progress(float step) {
        CURRENT.orElse(NONE).onProgress(step);
    }

    /**
     * Executes a runnable operation within the context of the specified {@link ProgressListener}.
     *
     * @param listener the progress listener to bind
     * @param operation the operation to execute
     */
    static void run(@NonNull ProgressListener listener, @NonNull Runnable operation) {
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
    static <T> T supply(@NonNull ProgressListener listener, @NonNull Supplier<T> supplier) {
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
    static <T, X extends Throwable> T call(@NonNull ProgressListener listener,
            ScopedValue.@NonNull CallableOp<T, X> operation) throws X {
        return ScopedValue.where(CURRENT, listener).call(operation);
    }

    void onProgress(float step);

    default void onProgress() {
        onProgress(0f);
    }
}

