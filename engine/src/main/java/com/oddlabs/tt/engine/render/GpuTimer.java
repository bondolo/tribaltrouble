package com.oddlabs.tt.engine.render;

import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL33;

import java.util.Arrays;

/**
 * Measures GPU time per {@link GpuPass} with {@code GL_TIME_ELAPSED} queries.
 *
 * <p>Queries are issued only between {@link #beginFrame(long)} and {@link #endFrame()}; otherwise every call is a
 * no-op. Results are read asynchronously up to {@link #DEPTH} frames later so that reading never stalls the
 * pipeline. Must be used from the thread that owns the OpenGL context.
 */
public final class GpuTimer implements AutoCloseable {
    static final int DEPTH = 16;
    private static final int PASS_COUNT = GpuPass.values().length;
    private static final int NO_PASS = -1;

    private final int[] queries = new int[DEPTH * PASS_COUNT];
    private final int[] issued = new int[DEPTH];
    private boolean frameActive;
    private int slot;
    private int openPass = NO_PASS;

    void beginFrame(long frame) {
        if (queries[0] == 0) {
            Arrays.setAll(queries, _ -> GL15.glGenQueries());
        }
        slot = (int) (frame % DEPTH);
        issued[slot] = 0;
        frameActive = true;
    }

    /** Starts timing {@code pass}, ending the previously timed pass if one is open. */
    public void begin(GpuPass pass) {
        if (!frameActive) {
            return;
        }
        end();
        int index = pass.ordinal();
        GL15.glBeginQuery(GL33.GL_TIME_ELAPSED, queries[slot * PASS_COUNT + index]);
        issued[slot] |= 1 << index;
        openPass = index;
    }

    /** Ends the pass currently being timed, if any. */
    public void end() {
        if (openPass != NO_PASS) {
            GL15.glEndQuery(GL33.GL_TIME_ELAPSED);
            openPass = NO_PASS;
        }
    }

    void endFrame() {
        end();
        frameActive = false;
    }

    /**
     * Checks if all issued queries for {@code frame} are available without stalling the GPU.
     */
    boolean isAvailable(long frame) {
        int frameSlot = (int) (frame % DEPTH);
        int mask = issued[frameSlot];
        for (int pass = 0; pass < PASS_COUNT; pass++) {
            if ((mask & (1 << pass)) != 0
                    && GL15.glGetQueryObjecti(queries[frameSlot * PASS_COUNT + pass],
                    GL15.GL_QUERY_RESULT_AVAILABLE) == 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Reads the per-pass nanoseconds for {@code frame} into {@code nanos}.
     * Assumes {@link #isAvailable(long)} is true or query is ready.
     */
    void resolve(long frame, long[] nanos) {
        int frameSlot = (int) (frame % DEPTH);
        int mask = issued[frameSlot];
        for (int pass = 0; pass < PASS_COUNT; pass++) {
            nanos[pass] = (mask & (1 << pass)) != 0
                    ? GL33.glGetQueryObjecti64(queries[frameSlot * PASS_COUNT + pass], GL15.GL_QUERY_RESULT)
                    : 0L;
        }
    }

    @Override
    public void close() {
        end();
        frameActive = false;
        if (queries[0] != 0) {
            for (int query : queries) {
                GL15.glDeleteQueries(query);
            }
            Arrays.fill(queries, 0);
        }
    }
}
