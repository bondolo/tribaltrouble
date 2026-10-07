package com.oddlabs.tt.engine.render;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Records per-frame phase timings from the render thread and streams them to a CSV file.
 *
 * <p>Recording follows {@link DebugFlags#frame_timing}. While disabled each call is a single flag check; while
 * enabled the render thread only writes timestamps into a preallocated ring buffer.
 */
public final class FrameTimeRecorder implements AutoCloseable {
    private static final Logger logger = Logger.getLogger(FrameTimeRecorder.class.getName());
    private static final int PHASE_COUNT = Phase.values().length;
    private static final int GPU_PASS_COUNT = GpuPass.values().length;
    private static final DateTimeFormatter FILE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final String FILE_PREFIX = "frametimes-";
    private static final String FILE_SUFFIX = ".csv";

    /** Segments of a frame, in the order they are marked. */
    public enum Phase {
        POLL_EVENTS,
        SIMULATION,
        AUDIO,
        DISPLAY,
        SWAP,
        GL_FINISH
    }

    private final Path directory;
    private final GpuTimer gpuTimer;
    private final long[] gpuNanos = new long[GPU_PASS_COUNT];
    private @Nullable FrameTimeRing ring;
    private @Nullable FrameTimeWriter writer;
    private boolean claimed;

    public FrameTimeRecorder(Path directory, GpuTimer gpuTimer) {
        this.directory = directory;
        this.gpuTimer = gpuTimer;
    }

    public void begin() {
        if (DebugFlags.frame_timing != (ring != null)) {
            if (DebugFlags.frame_timing) {
                startRecording();
            } else {
                stopRecording();
            }
        }
        var active = ring;
        claimed = active != null && active.claim(System.nanoTime());
        if (claimed) {
            gpuTimer.beginFrame(active.currentFrame());
        }
    }

    public void mark(Phase phase) {
        var active = ring;
        if (claimed && active != null) {
            active.set(phase.ordinal(), System.nanoTime());
        }
    }

    private long oldestUnresolvedFrame;

    public void end() {
        var active = ring;
        if (claimed && active != null) {
            gpuTimer.endFrame();
            drainReadyFrames(active);
        }
        claimed = false;
    }

    private void drainReadyFrames(FrameTimeRing target) {
        long current = target.currentFrame();
        while (oldestUnresolvedFrame <= current) {
            // If the query ring is about to overwrite oldestUnresolvedFrame, force resolve or mark unavailable
            boolean mustEvict = (current - oldestUnresolvedFrame) >= GpuTimer.DEPTH;
            if (gpuTimer.isAvailable(oldestUnresolvedFrame)) {
                gpuTimer.resolve(oldestUnresolvedFrame, gpuNanos);
                for (int pass = 0; pass < GPU_PASS_COUNT; pass++) {
                    target.setGpu(oldestUnresolvedFrame, pass, gpuNanos[pass]);
                }
                target.publishNext();
                oldestUnresolvedFrame++;
            } else if (mustEvict) {
                for (int pass = 0; pass < GPU_PASS_COUNT; pass++) {
                    target.setGpu(oldestUnresolvedFrame, pass, FrameTimeRing.UNAVAILABLE);
                }
                target.publishNext();
                oldestUnresolvedFrame++;
            } else {
                break;
            }
        }
    }

    private void startRecording() {
        Path file = directory.resolve(FILE_PREFIX + FILE_TIMESTAMP.format(LocalDateTime.now(ZoneId.systemDefault()))
                + FILE_SUFFIX);
        var newRing = new FrameTimeRing(PHASE_COUNT, GPU_PASS_COUNT);
        try {
            writer = FrameTimeWriter.start(newRing, file);
            ring = newRing;
            oldestUnresolvedFrame = 0;
            logger.info("Frame timing recording to " + file.toAbsolutePath());
        } catch (IOException e) {
            logger.log(Level.WARNING, "Unable to start frame timing recording", e);
            DebugFlags.frame_timing = false;
        }
    }

    private void stopRecording() {
        var active = ring;
        ring = null;
        var activeWriter = writer;
        writer = null;
        claimed = false;
        gpuTimer.endFrame();
        if (active != null) {
            active.publishRemaining();
        }
        if (activeWriter != null) {
            activeWriter.close();
            logger.info("Frame timing recording stopped");
        }
    }

    @Override
    public void close() {
        stopRecording();
    }
}
