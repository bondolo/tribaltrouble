package com.oddlabs.net;

import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

/**
 * Monotonic elapsed-time clock that wraps an underlying monotonic clock and injects
 * synthetic latency jitter, burst clustering, and optional clock skew.
 */
public final class JitterTimeManager implements TimeManager {

    private static final double BURST_MULTIPLIER_MIN = 1.5;
    private static final double BURST_MULTIPLIER_MAX = 3.0;
    private static final double PPM_TO_FRACTION = 1e-6;
    private static final double MAX_PROBABILITY_EXCLUSIVE = 0.999999999;

    private final JitterConfig config;
    private final RandomGenerator rng;

    // Mutable state guarded by synchronization
    private long lastBaseMillis = Long.MIN_VALUE;
    private long cachedOutputMillis = 0L;
    private long lastReportedMillis = 0L;
    private long currentDelayMillis = 0L;
    private boolean inBurst = false;
    private int forcedBurstRemainingTicks = 0;
    private long pendingInjectedDelayMillis = 0L;

    public JitterTimeManager(JitterConfig config) {
        this.config = config;
        this.rng = RandomGeneratorFactory.of("L64X128MixRandom").create(config.seed());
    }

    @Override
    public synchronized long getMillis() {
        long currentBase = config.baseClock().getMillis();

        // 1. Sub-tick caching: if base clock has not advanced, return cached result
        if (currentBase == lastBaseMillis) {
            return cachedOutputMillis;
        }

        // 2. Advance tick state
        lastBaseMillis = currentBase;

        // Handle forced burst ticks
        if (forcedBurstRemainingTicks > 0) {
            forcedBurstRemainingTicks--;
            inBurst = true;
        } else if (inBurst) {
            // Markov burst continuation check
            if (rng.nextDouble() > config.burstContinuationProbability()) {
                inBurst = false;
            }
        } else {
            // Check if jitter triggers
            if (config.jitterProbability() > 0.0 && rng.nextDouble() < config.jitterProbability()) {
                // Check if this jitter initiates a burst
                if (config.burstProbability() > 0.0 && rng.nextDouble() < config.burstProbability()) {
                    inBurst = true;
                }
            }
        }

        // 3. Determine delay magnitude
        long delay = 0L;
        if (pendingInjectedDelayMillis > 0L) {
            delay = pendingInjectedDelayMillis;
            pendingInjectedDelayMillis = 0L;
        } else if (inBurst || (config.jitterProbability() > 0.0 && rng.nextDouble() < config.jitterProbability())) {
            if (config.meanDelayMillis() > 0.0) {
                // Sample exponential distribution: -ln(1 - U) * mean
                double u = rng.nextDouble();
                // Protect against Math.log(0.0)
                if (u >= 1.0) {
                    u = MAX_PROBABILITY_EXCLUSIVE;
                }
                double rawDelay = -Math.log(1.0 - u) * config.meanDelayMillis();
                if (inBurst) {
                    rawDelay *= (BURST_MULTIPLIER_MIN + rng.nextDouble() * (BURST_MULTIPLIER_MAX
                            - BURST_MULTIPLIER_MIN));
                }
                delay = Math.round(rawDelay);
            }
        }

        // Clamp delay to maxDelayMillis
        if (config.maxDelayMillis() > 0L) {
            delay = Math.min(delay, config.maxDelayMillis());
        }
        currentDelayMillis = delay;

        // 4. Compute optional continuous drift
        long drift = 0L;
        if (config.driftPpm() != 0.0) {
            drift = Math.round(currentBase * config.driftPpm() * PPM_TO_FRACTION);
        }

        // 5. Target time and strict monotonicity enforcement
        long targetMillis = currentBase + drift + delay;
        long outputMillis = Math.max(lastReportedMillis, targetMillis);

        lastReportedMillis = outputMillis;
        cachedOutputMillis = outputMillis;

        return outputMillis;
    }

    /**
     * Injects an exact delay in milliseconds to be applied on the next base clock advance.
     *
     * @param millis delay duration in milliseconds
     */
    public synchronized void injectDelay(long millis) {
        if (millis < 0L) {
            throw new IllegalArgumentException("Injected delay must be non-negative: " + millis);
        }
        this.pendingInjectedDelayMillis = millis;
    }

    /**
     * Forces the clock into the congested burst state for a specified number of base clock advances.
     *
     * @param ticks number of ticks to remain in burst state
     */
    public synchronized void forceBurst(int ticks) {
        if (ticks < 0) {
            throw new IllegalArgumentException("Burst ticks must be non-negative: " + ticks);
        }
        this.forcedBurstRemainingTicks = ticks;
        this.inBurst = true;
    }

    /**
     * {@return the current synthetic latency delay in milliseconds applied on the latest tick}
     */
    public synchronized long getCurrentDelayMillis() {
        return currentDelayMillis;
    }

    /**
     * {@return true if the clock is currently in a congested burst state}
     */
    public synchronized boolean isInBurst() {
        return inBurst;
    }

    /**
     * {@return the raw unjittered time from the underlying base supplier}
     */
    public long getBaseMillis() {
        return config.baseClock().getMillis();
    }

    /**
     * Advances the underlying base clock if it is an instance of {@link TickTimeManager}.
     *
     * @return the new elapsed milliseconds value from the base clock
     */
    public long advance() {
        if (config.baseClock() instanceof TickTimeManager tickTimeManager) {
            return tickTimeManager.advance();
        }
        return getBaseMillis();
    }

    /**
     * Advances the underlying base clock by the specified delta if it is an instance of {@link TickTimeManager}.
     *
     * @param deltaMillis duration in milliseconds to advance
     * @return the new elapsed milliseconds value from the base clock
     */
    public long advance(long deltaMillis) {
        if (config.baseClock() instanceof TickTimeManager tickTimeManager) {
            return tickTimeManager.advance(deltaMillis);
        }
        return getBaseMillis();
    }

    /**
     * {@return the configuration record driving this jitter clock}
     */
    public JitterConfig getConfig() {
        return config;
    }
}
