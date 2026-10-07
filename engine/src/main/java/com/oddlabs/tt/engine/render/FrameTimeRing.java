package com.oddlabs.tt.engine.render;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Fixed-capacity single-producer single-consumer ring of per-frame timestamps and GPU durations.
 *
 * <p>The producer never blocks and never allocates; frames that do not fit are counted as dropped. Frames are
 * claimed when they start and published in order once their GPU results have been filled in.
 */
final class FrameTimeRing {
    static final int CAPACITY = 1 << 12;
    static final long UNAVAILABLE = -1L;

    private final int cpuPhases;
    private final int gpuPasses;
    private final int stride;
    private final long[] data;
    private final AtomicLong published = new AtomicLong();
    private final AtomicLong consumed = new AtomicLong();
    private final AtomicLong dropped = new AtomicLong();

    private long claimed;
    private long publishedHead;
    private int base;

    FrameTimeRing(int cpuPhases, int gpuPasses) {
        this.cpuPhases = cpuPhases;
        this.gpuPasses = gpuPasses;
        this.stride = 1 + cpuPhases + gpuPasses;
        this.data = new long[CAPACITY * stride];
    }

    boolean claim(long startNanos) {
        if (claimed - consumed.getAcquire() >= CAPACITY) {
            dropped.incrementAndGet();
            return false;
        }
        base = slotBase(claimed);
        data[base] = startNanos;
        claimed++;
        return true;
    }

    long currentFrame() {
        return claimed - 1;
    }

    void set(int phase, long nanos) {
        data[base + 1 + phase] = nanos;
    }

    void setGpu(long frame, int pass, long nanos) {
        data[slotBase(frame) + 1 + cpuPhases + pass] = nanos;
    }

    void publishNext() {
        publishedHead++;
        published.setRelease(publishedHead);
    }

    void publishRemaining() {
        while (publishedHead < claimed) {
            for (int pass = 0; pass < gpuPasses; pass++) {
                setGpu(publishedHead, pass, UNAVAILABLE);
            }
            publishNext();
        }
    }

    long droppedFrames() {
        return dropped.get();
    }

    long publishedFrames() {
        return published.getAcquire();
    }

    long get(long frame, int offset) {
        return data[slotBase(frame) + offset];
    }

    void release(long frame) {
        consumed.setRelease(frame + 1);
    }

    private int slotBase(long frame) {
        return (int) (frame & (CAPACITY - 1)) * stride;
    }
}
