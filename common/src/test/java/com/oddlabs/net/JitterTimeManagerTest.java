package com.oddlabs.net;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests verifying contract, monotonicity, determinism, and burst behaviour for {@link JitterTimeManager}.
 */
class JitterTimeManagerTest {

    @Test
    void testStrictMonotonicity() {
        AtomicLong base = new AtomicLong(0L);
        JitterConfig config = JitterConfig.heavy(base::get, 42L);
        JitterTimeManager clock = new JitterTimeManager(config);

        Random random = new Random(101L);
        long prev = clock.getMillis();

        for (int i = 0; i < 100_000; i++) {
            base.addAndGet(random.nextInt(51)); // 0 to 50ms advancement
            long current = clock.getMillis();
            assertTrue(current >= prev, "Clock must never return an earlier timestamp: " + current + " < " + prev);
            prev = current;
        }
    }

    @Test
    void testLatencyOnlyNeverEarly() {
        AtomicLong base = new AtomicLong(10_000L);
        JitterConfig config = JitterConfig.mild(base::get, 999L);
        JitterTimeManager clock = new JitterTimeManager(config);

        for (int i = 0; i < 5_000; i++) {
            base.addAndGet(20L);
            long current = clock.getMillis();
            assertTrue(current >= base.get(), "Output clock must never be earlier than base clock when drift is zero");
        }
    }

    @Test
    void testSubTickCachingConsistency() {
        AtomicLong base = new AtomicLong(100L);
        JitterConfig config = JitterConfig.heavy(base::get, 555L);
        JitterTimeManager clock = new JitterTimeManager(config);

        long first = clock.getMillis();
        for (int i = 0; i < 100; i++) {
            assertEquals(first, clock.getMillis(), "Sub-tick queries must return identical cached timestamp");
        }
    }

    @Test
    void testDeterministicRepeatability() {
        AtomicLong base1 = new AtomicLong(0L);
        AtomicLong base2 = new AtomicLong(0L);

        long seed = 12345L;
        JitterConfig config1 = JitterConfig.heavy(base1::get, seed);
        JitterConfig config2 = JitterConfig.heavy(base2::get, seed);

        JitterTimeManager clock1 = new JitterTimeManager(config1);
        JitterTimeManager clock2 = new JitterTimeManager(config2);

        Random random = new Random(777L);
        for (int i = 0; i < 10_000; i++) {
            int step = random.nextInt(40);
            base1.addAndGet(step);
            base2.addAndGet(step);

            long out1 = clock1.getMillis();
            long out2 = clock2.getMillis();

            assertEquals(out1, out2, "Clocks with identical seed must yield identical time output at step " + i);
            assertEquals(clock1.isInBurst(), clock2.isInBurst(), "Clocks must match burst state at step " + i);
            assertEquals(clock1.getCurrentDelayMillis(), clock2.getCurrentDelayMillis(),
                    "Clocks must match delay at step " + i);
        }
    }

    @Test
    void testZeroLongTermDrift() {
        AtomicLong base = new AtomicLong(0L);
        JitterConfig config = JitterConfig.builder(base::get, 888L)
                .jitterProbability(0.5)
                .meanDelayMillis(20.0)
                .maxDelayMillis(100L)
                .build();
        JitterTimeManager clock = new JitterTimeManager(config);

        for (int i = 0; i < 5_000; i++) {
            base.addAndGet(20L);
            clock.getMillis();
        }

        // Simulate a quiet relaxation period without jitter
        base.addAndGet(10_000L);
        long out = clock.getMillis();

        assertEquals(base.get() + clock.getCurrentDelayMillis(), out,
                "Once delays subside, target time catches up and realigns");
    }

    @Test
    void testManualDelayInjection() {
        AtomicLong base = new AtomicLong(1_000L);
        JitterConfig config = JitterConfig.clean(base::get);
        JitterTimeManager clock = new JitterTimeManager(config);

        assertEquals(1_000L, clock.getMillis());

        clock.injectDelay(75L);
        base.addAndGet(20L); // 1020L

        assertEquals(1020L + 75L, clock.getMillis(), "Injected delay must be applied on next tick");

        // Subsequent tick without injection should drop back to unjittered (clean config)
        base.addAndGet(100L); // 1120L
        assertEquals(1120L, clock.getMillis(), "Delay injection is one-shot and consumes itself");
    }

    @Test
    void testForceBurst() {
        AtomicLong base = new AtomicLong(0L);
        JitterConfig config = JitterConfig.builder(base::get, 321L)
                .burstProbability(0.0)
                .burstContinuationProbability(0.0)
                .build();
        JitterTimeManager clock = new JitterTimeManager(config);

        assertFalse(clock.isInBurst());

        clock.forceBurst(5);
        assertTrue(clock.isInBurst());

        for (int i = 0; i < 5; i++) {
            base.addAndGet(20L);
            clock.getMillis();
            assertTrue(clock.isInBurst(), "Should remain in forced burst at tick " + i);
        }

        base.addAndGet(20L);
        clock.getMillis();
        assertFalse(clock.isInBurst(), "Should exit burst after forced ticks expire");
    }

    @Test
    void testMaxDelayClamping() {
        AtomicLong base = new AtomicLong(100L);
        JitterConfig config = JitterConfig.builder(base::get, 111L)
                .maxDelayMillis(50L)
                .build();
        JitterTimeManager clock = new JitterTimeManager(config);

        clock.injectDelay(200L);
        base.addAndGet(20L);

        long out = clock.getMillis();
        assertEquals(120L + 50L, out, "Injected delay must be clamped to maxDelayMillis");
        assertEquals(50L, clock.getCurrentDelayMillis());
    }

    @Test
    void testSimulationTickCounterWrapper() {
        TickTimeManager tickClock = new TickTimeManager(0L, 20L);
        JitterConfig config = JitterConfig.mild(tickClock, 123L);
        JitterTimeManager jitterClock = new JitterTimeManager(config);

        for (int i = 0; i < 1_000; i++) {
            tickClock.advance();
            long t = jitterClock.getMillis();
            assertTrue(t >= tickClock.getMillis());
        }
    }

    @Test
    void testThreadSafety() throws Exception {
        AtomicLong base = new AtomicLong(0L);
        JitterConfig config = JitterConfig.heavy(base::get, 777L);
        JitterTimeManager clock = new JitterTimeManager(config);

        int threads = 8;
        int iterationsPerThread = 5_000;
        ExecutorService executor = Executors.newFixedThreadPool(threads);

        try {
            List<Callable<List<Long>>> tasks = new ArrayList<>();
            for (int t = 0; t < threads; t++) {
                tasks.add(() -> {
                    List<Long> sampled = new ArrayList<>(iterationsPerThread);
                    for (int i = 0; i < iterationsPerThread; i++) {
                        base.incrementAndGet();
                        sampled.add(clock.getMillis());
                    }
                    return sampled;
                });
            }

            List<Future<List<Long>>> futures = executor.invokeAll(tasks);
            for (Future<List<Long>> future : futures) {
                List<Long> sampled = future.get();
                long prev = sampled.getFirst();
                for (int i = 1; i < sampled.size(); i++) {
                    long cur = sampled.get(i);
                    assertTrue(cur >= prev, "Monotonicity violated across concurrent queries: " + cur + " < " + prev);
                    prev = cur;
                }
            }
        } finally {
            executor.shutdown();
        }
    }

    @Test
    void testLaggyPresetHighLatency() {
        AtomicLong base = new AtomicLong(0L);
        JitterConfig config = JitterConfig.laggy(base::get, 2026L);
        JitterTimeManager clock = new JitterTimeManager(config);

        long maxDelayObserved = 0L;
        long prev = clock.getMillis();

        for (int i = 0; i < 2_000; i++) {
            base.addAndGet(20L); // 50Hz tick
            long current = clock.getMillis();
            assertTrue(current >= prev, "Monotonicity invariant must hold in laggy conditions");
            assertTrue(current >= base.get(), "Clock must not run earlier than base");
            long delay = clock.getCurrentDelayMillis();
            assertTrue(delay <= config.maxDelayMillis(), "Delay must not exceed max delay clamp");
            if (delay > maxDelayObserved) {
                maxDelayObserved = delay;
            }
            prev = current;
        }

        assertTrue(maxDelayObserved >= 50L, "Laggy preset should produce significant multi-tick latency spikes: "
                + maxDelayObserved);
    }

    @Test
    void testLoopbackPresetSchedulingJitter() {
        AtomicLong base = new AtomicLong(0L);
        JitterConfig config = JitterConfig.loopback(base::get, 54321L);
        JitterTimeManager clock = new JitterTimeManager(config);

        long prev = clock.getMillis();
        long stallsObserved = 0L;

        for (int i = 0; i < 2_000; i++) {
            base.addAndGet(20L); // 50Hz tick
            long current = clock.getMillis();
            assertTrue(current >= prev, "Monotonicity must hold under loopback scheduling jitter");
            assertTrue(current >= base.get(), "Clock must not be earlier than base");
            long delay = clock.getCurrentDelayMillis();
            assertTrue(delay <= config.maxDelayMillis(), "Delay must be clamped to max bound");
            if (delay > 0L) {
                stallsObserved++;
            }
            prev = current;
        }

        assertTrue(stallsObserved > 0L, "Loopback config should experience occasional scheduler preemption stalls");
    }
}
