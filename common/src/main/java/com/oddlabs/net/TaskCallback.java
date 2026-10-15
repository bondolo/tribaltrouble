package com.oddlabs.net;

/**
 * Receives the completion or failure of an asynchronous task dispatched by {@link TaskThread}.
 *
 * @param <T> the result type
 */
public interface TaskCallback<T> {
    /**
     * Invoked on the caller's thread when the task completes successfully.
     *
     * @param result the result of the task
     */
    void taskCompleted(T result);

    /**
     * Invoked on the caller's thread when the task throws an exception.
     *
     * @param e the error thrown by the task
     */
    void taskFailed(Throwable e);
}
