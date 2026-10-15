package com.oddlabs.net;

import com.oddlabs.event.NotDeterministic;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link TaskThread}.
 */
class TaskThreadTest {

    @Test
    void testSuccessfulTaskExecution() throws InterruptedException {
        CountDownLatch notified = new CountDownLatch(1);
        AtomicReference<String> completedResult = new AtomicReference<>();
        AtomicReference<Throwable> failedError = new AtomicReference<>();

        try (TaskThread taskThread = new TaskThread(new NotDeterministic(), notified::countDown)) {
            taskThread.addTask(
                    () -> "hello from virtual thread",
                    new TaskCallback<String>() {
                        @Override
                        public void taskCompleted(String result) {
                            completedResult.set(result);
                        }

                        @Override
                        public void taskFailed(Throwable e) {
                            failedError.set(e);
                        }
                    });

            assertTrue(notified.await(5, TimeUnit.SECONDS), "Notification action should be invoked");
            taskThread.poll();

            assertEquals("hello from virtual thread", completedResult.get());
            assertNull(failedError.get());
        }
    }

    @Test
    void testFailedTaskExecution() throws InterruptedException {
        CountDownLatch notified = new CountDownLatch(1);
        AtomicReference<String> completedResult = new AtomicReference<>();
        AtomicReference<Throwable> failedError = new AtomicReference<>();

        try (TaskThread taskThread = new TaskThread(new NotDeterministic(), notified::countDown)) {
            taskThread.addTask(
                    () -> {
                        throw new IOException("Simulated network failure");
                    },
                    new TaskCallback<String>() {
                        @Override
                        public void taskCompleted(String result) {
                            completedResult.set(result);
                        }

                        @Override
                        public void taskFailed(Throwable e) {
                            failedError.set(e);
                        }
                    });

            assertTrue(notified.await(5, TimeUnit.SECONDS), "Notification action should be invoked on failure");
            taskThread.poll();

            assertNull(completedResult.get());
            assertInstanceOf(IOException.class, failedError.get());
            assertEquals("Simulated network failure", failedError.get().getMessage());
        }
    }

    @Test
    void testCancelledTaskDoesNotDeliver() throws InterruptedException {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch block = new CountDownLatch(1);
        CountDownLatch notified = new CountDownLatch(1);
        AtomicBoolean delivered = new AtomicBoolean(false);

        try (TaskThread taskThread = new TaskThread(new NotDeterministic(), notified::countDown)) {
            Task task = taskThread.addTask(
                    () -> {
                        started.countDown();
                        block.await(2, TimeUnit.SECONDS);
                        return "cancelled-data";
                    },
                    new TaskCallback<String>() {
                        @Override
                        public void taskCompleted(String result) {
                            delivered.set(true);
                        }

                        @Override
                        public void taskFailed(Throwable e) {
                            delivered.set(true);
                        }
                    });

            assertTrue(started.await(5, TimeUnit.SECONDS), "Task should start execution");
            task.cancel();
            block.countDown();

            assertTrue(notified.await(5, TimeUnit.SECONDS),
                    "Notification action should be invoked after task finishes");
            taskThread.poll();

            assertFalse(delivered.get(), "Cancelled task should not deliver results to callback");
        }
    }
}
