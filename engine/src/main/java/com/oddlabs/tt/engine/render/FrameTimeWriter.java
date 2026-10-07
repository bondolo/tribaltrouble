package com.oddlabs.tt.engine.render;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.locks.LockSupport;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Streams frame timings from a {@link FrameTimeRing} to a CSV file on a background virtual thread.
 */
final class FrameTimeWriter implements AutoCloseable {
    private static final Logger logger = Logger.getLogger(FrameTimeWriter.class.getName());
    private static final long DRAIN_INTERVAL_NANOS = 250_000_000L;
    private static final long FLUSH_INTERVAL_NANOS = 1_000_000_000L;
    private static final long NANOS_PER_MICRO = 1_000L;
    private static final long UNAVAILABLE_MICROS = -1L;
    private static final char SEPARATOR = ',';

    private final FrameTimeRing ring;
    private final FrameTimeRecorder.Phase[] phases = FrameTimeRecorder.Phase.values();
    private final GpuPass[] gpuPasses = GpuPass.values();
    private final BufferedWriter out;
    private final Thread thread;
    private final StringBuilder line = new StringBuilder();
    private volatile boolean running = true;
    private long nextFrame;
    private long lastFlushNanos = System.nanoTime();

    private FrameTimeWriter(FrameTimeRing ring, BufferedWriter out) {
        this.ring = ring;
        this.out = out;
        this.thread = Thread.ofVirtual().name("frame-time-writer").unstarted(this::run);
    }

    static FrameTimeWriter start(FrameTimeRing ring, Path file) throws IOException {
        Files.createDirectories(file.toAbsolutePath().getParent());
        var writer = new FrameTimeWriter(ring, Files.newBufferedWriter(file));
        writer.writeHeader();
        writer.thread.start();
        return writer;
    }

    private void writeHeader() throws IOException {
        line.setLength(0);
        line.append("frame").append(SEPARATOR).append("start_ns");
        for (var phase : phases) {
            line.append(SEPARATOR).append(phase.name().toLowerCase(Locale.ROOT)).append("_us");
        }
        line.append(SEPARATOR).append("total_us");
        for (var pass : gpuPasses) {
            line.append(SEPARATOR).append("gpu_").append(pass.name().toLowerCase(Locale.ROOT)).append("_us");
        }
        line.append(SEPARATOR).append("gpu_total_us").append(SEPARATOR).append("dropped").append('\n');
        out.append(line);
    }

    private void run() {
        try {
            while (running) {
                drain();
                flushIfDue();
                LockSupport.parkNanos(DRAIN_INTERVAL_NANOS);
            }
            drain();
            out.flush();
        } catch (IOException e) {
            logger.log(Level.WARNING, "Frame timing writer failed", e);
        } finally {
            try {
                out.close();
            } catch (IOException e) {
                logger.log(Level.WARNING, "Failed to close frame timing file", e);
            }
        }
    }

    private void drain() throws IOException {
        long published = ring.publishedFrames();
        while (nextFrame < published) {
            writeFrame(nextFrame);
            ring.release(nextFrame);
            nextFrame++;
        }
    }

    private void writeFrame(long frame) throws IOException {
        long start = ring.get(frame, 0);
        long previous = start;
        line.setLength(0);
        line.append(frame).append(SEPARATOR).append(start);
        for (int i = 0; i < phases.length; i++) {
            long mark = ring.get(frame, 1 + i);
            line.append(SEPARATOR).append((mark - previous) / NANOS_PER_MICRO);
            previous = mark;
        }
        line.append(SEPARATOR).append((previous - start) / NANOS_PER_MICRO);
        long gpuTotal = 0;
        boolean gpuAvailable = true;
        for (int i = 0; i < gpuPasses.length; i++) {
            long nanos = ring.get(frame, 1 + phases.length + i);
            if (nanos < 0) {
                gpuAvailable = false;
                line.append(SEPARATOR).append(UNAVAILABLE_MICROS);
            } else {
                gpuTotal += nanos;
                line.append(SEPARATOR).append(nanos / NANOS_PER_MICRO);
            }
        }
        line.append(SEPARATOR).append(gpuAvailable ? gpuTotal / NANOS_PER_MICRO : UNAVAILABLE_MICROS)
                .append(SEPARATOR).append(ring.droppedFrames())
                .append('\n');
        out.append(line);
    }

    private void flushIfDue() throws IOException {
        long now = System.nanoTime();
        if (now - lastFlushNanos >= FLUSH_INTERVAL_NANOS) {
            out.flush();
            lastFlushNanos = now;
        }
    }

    @Override
    public void close() {
        running = false;
        LockSupport.unpark(thread);
        try {
            thread.join();
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
        }
    }
}
