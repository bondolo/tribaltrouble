package com.oddlabs.net;

/**
 * Configuration parameters for {@link JitterTimeManager}.
 *
 * @param baseClock monotonic time source
 * @param seed PRNG seed for deterministic repeatability
 * @param jitterProbability probability in [0.0, 1.0] of jitter occurring per tick transition
 * @param meanDelayMillis mean latency in milliseconds added during an active jitter event
 * @param maxDelayMillis upper bound clamp on synthetic delay in milliseconds
 * @param burstProbability probability in [0.0, 1.0] that an active jitter event initiates a burst
 * @param burstContinuationProbability probability in [0.0, 1.0] that a burst persists across consecutive ticks
 * @param driftPpm parts-per-million clock skew rate (0.0 = zero drift)
 */
public record JitterConfig(
                           TimeManager baseClock,
                           long seed,
                           double jitterProbability,
                           double meanDelayMillis,
                           long maxDelayMillis,
                           double burstProbability,
                           double burstContinuationProbability,
                           double driftPpm) {

    public static final double DEFAULT_JITTER_PROBABILITY = 0.05;
    public static final double DEFAULT_MEAN_DELAY_MILLIS = 2.0;
    public static final long DEFAULT_MAX_DELAY_MILLIS = 500L;
    public static final double DEFAULT_BURST_PROBABILITY = 0.20;
    public static final double DEFAULT_BURST_CONTINUATION_PROBABILITY = 0.60;
    public static final double DEFAULT_DRIFT_PPM = 0.0;

    public JitterConfig {
        if (jitterProbability < 0.0 || jitterProbability > 1.0) {
            throw new IllegalArgumentException("jitterProbability must be in [0.0, 1.0]: " + jitterProbability);
        }
        if (meanDelayMillis < 0.0) {
            throw new IllegalArgumentException("meanDelayMillis must be non-negative: " + meanDelayMillis);
        }
        if (maxDelayMillis < 0L) {
            throw new IllegalArgumentException("maxDelayMillis must be non-negative: " + maxDelayMillis);
        }
        if (burstProbability < 0.0 || burstProbability > 1.0) {
            throw new IllegalArgumentException("burstProbability must be in [0.0, 1.0]: " + burstProbability);
        }
        if (burstContinuationProbability < 0.0 || burstContinuationProbability > 1.0) {
            throw new IllegalArgumentException(
                    "burstContinuationProbability must be in [0.0, 1.0]: " + burstContinuationProbability);
        }
    }

    /**
     * Preset for zero jitter, returning the underlying baseline clock source directly.
     *
     * @param baseClock underlying clock source
     * @return zero-jitter configuration
     */
    public static JitterConfig clean(TimeManager baseClock) {
        return builder(baseClock, 0L)
                .jitterProbability(0.0)
                .meanDelayMillis(0.0)
                .maxDelayMillis(0L)
                .burstProbability(0.0)
                .burstContinuationProbability(0.0)
                .driftPpm(0.0)
                .build();
    }

    /**
     * Preset for mild baseline jitter (~2ms mean latency, rare bursts).
     *
     * @param baseClock underlying clock source
     * @param seed PRNG seed
     * @return mild jitter configuration
     */
    public static JitterConfig mild(TimeManager baseClock, long seed) {
        return builder(baseClock, seed).build();
    }

    /**
     * Preset for heavy network congestion (25% jitter probability, 10ms mean delay, frequent bursts).
     *
     * @param baseClock underlying clock source
     * @param seed PRNG seed
     * @return heavy jitter configuration
     */
    public static JitterConfig heavy(TimeManager baseClock, long seed) {
        return builder(baseClock, seed)
                .jitterProbability(0.25)
                .meanDelayMillis(10.0)
                .maxDelayMillis(1000L)
                .burstProbability(0.40)
                .burstContinuationProbability(0.80)
                .build();
    }

    /**
     * Preset for low-performance, high-latency, laggy network conditions (50% jitter probability, 80ms mean delay,
     * persistent bursts).
     *
     * @param baseClock underlying clock source
     * @param seed PRNG seed
     * @return laggy network jitter configuration
     */
    public static JitterConfig laggy(TimeManager baseClock, long seed) {
        return builder(baseClock, seed)
                .jitterProbability(0.50)
                .meanDelayMillis(80.0)
                .maxDelayMillis(2000L)
                .burstProbability(0.50)
                .burstContinuationProbability(0.85)
                .build();
    }

    /**
     * Preset for local loopback networking subject to non-realtime consumer OS thread scheduling
     * and low CPU core availability (fast baseline with occasional thread preemption stalls).
     *
     * @param baseClock underlying clock source
     * @param seed PRNG seed
     * @return loopback scheduling jitter configuration
     */
    public static JitterConfig loopback(TimeManager baseClock, long seed) {
        return builder(baseClock, seed)
                .jitterProbability(0.08)
                .meanDelayMillis(15.0)
                .maxDelayMillis(100L)
                .burstProbability(0.15)
                .burstContinuationProbability(0.35)
                .driftPpm(0.0)
                .build();
    }

    /**
     * Creates a builder initialized with the given base clock and seed.
     *
     * @param baseClock underlying clock source
     * @param seed PRNG seed
     * @return new builder instance
     */
    public static Builder builder(TimeManager baseClock, long seed) {
        return new Builder(baseClock, seed);
    }

    /**
     * Fluent builder for {@link JitterConfig}.
     */
    public static final class Builder {
        private final TimeManager baseClock;
        private final long seed;
        private double jitterProbability = DEFAULT_JITTER_PROBABILITY;
        private double meanDelayMillis = DEFAULT_MEAN_DELAY_MILLIS;
        private long maxDelayMillis = DEFAULT_MAX_DELAY_MILLIS;
        private double burstProbability = DEFAULT_BURST_PROBABILITY;
        private double burstContinuationProbability = DEFAULT_BURST_CONTINUATION_PROBABILITY;
        private double driftPpm = DEFAULT_DRIFT_PPM;

        public Builder(TimeManager baseClock, long seed) {
            this.baseClock = baseClock;
            this.seed = seed;
        }

        public Builder jitterProbability(double jitterProbability) {
            this.jitterProbability = jitterProbability;
            return this;
        }

        public Builder meanDelayMillis(double meanDelayMillis) {
            this.meanDelayMillis = meanDelayMillis;
            return this;
        }

        public Builder maxDelayMillis(long maxDelayMillis) {
            this.maxDelayMillis = maxDelayMillis;
            return this;
        }

        public Builder burstProbability(double burstProbability) {
            this.burstProbability = burstProbability;
            return this;
        }

        public Builder burstContinuationProbability(double burstContinuationProbability) {
            this.burstContinuationProbability = burstContinuationProbability;
            return this;
        }

        public Builder driftPpm(double driftPpm) {
            this.driftPpm = driftPpm;
            return this;
        }

        public JitterConfig build() {
            return new JitterConfig(
                    baseClock,
                    seed,
                    jitterProbability,
                    meanDelayMillis,
                    maxDelayMillis,
                    burstProbability,
                    burstContinuationProbability,
                    driftPpm);
        }
    }
}
