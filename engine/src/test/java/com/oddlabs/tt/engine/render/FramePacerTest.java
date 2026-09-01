package com.oddlabs.tt.engine.render;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link FramePacer}.
 */
class FramePacerTest {

    @Test
    void testInitialState() {
        AtomicLong clock = new AtomicLong(1000L);
        FramePacer pacer = new FramePacer(clock::get);

        assertTrue(pacer.isTimeFrozen());
        assertFalse(pacer.isTimeStopped());
        assertEquals(0L, pacer.getExecutionTime());
        assertEquals(0f, pacer.getExecutionTimePrecision());
        assertEquals(0L, pacer.getChecksumMillisecondCounter());
        assertTrue(pacer.shouldComplainChecksum());
        assertEquals(1000L, pacer.getLastFrameTime());
        assertEquals(1000L, pacer.getSystemTime());
    }

    @Test
    void testSystemTimeProgressionWhenUnfrozen() {
        AtomicLong clock = new AtomicLong(1000L);
        FramePacer pacer = new FramePacer(clock::get);

        pacer.unfreezeTime();
        assertFalse(pacer.isTimeFrozen());

        clock.set(1500L);
        assertEquals(1500L, pacer.getSystemTime());

        clock.set(2200L);
        assertEquals(2200L, pacer.getSystemTime());
    }

    @Test
    void testFreezeTimeFreezesSystemTime() {
        AtomicLong clock = new AtomicLong(1000L);
        FramePacer pacer = new FramePacer(clock::get);

        pacer.unfreezeTime();
        clock.set(1200L);
        assertEquals(1200L, pacer.getSystemTime());

        pacer.freezeTime();
        assertTrue(pacer.isTimeFrozen());

        clock.set(5000L);
        assertEquals(1200L, pacer.getSystemTime());
    }

    @Test
    void testUnfreezeCompensatesElapsedTimeWhileFrozen() {
        AtomicLong clock = new AtomicLong(1000L);
        FramePacer pacer = new FramePacer(clock::get);

        pacer.unfreezeTime();
        clock.set(1500L);
        assertEquals(1500L, pacer.getSystemTime());

        pacer.freezeTime();
        clock.set(11500L);
        assertEquals(1500L, pacer.getSystemTime());

        pacer.unfreezeTime();
        assertEquals(1500L, pacer.getSystemTime());

        clock.set(11600L);
        assertEquals(1600L, pacer.getSystemTime());
    }

    @Test
    void testWarpTimeAdvancesSystemTime() {
        AtomicLong clock = new AtomicLong(1000L);
        FramePacer pacer = new FramePacer(clock::get);

        pacer.unfreezeTime();
        assertEquals(1000L, pacer.getSystemTime());

        pacer.warpTime(500L);
        assertEquals(1500L, pacer.getSystemTime());

        clock.set(1100L);
        assertEquals(1600L, pacer.getSystemTime());
    }

    @Test
    void testTimeStopToggle() {
        AtomicLong clock = new AtomicLong(1000L);
        FramePacer pacer = new FramePacer(clock::get);

        pacer.unfreezeTime();
        assertFalse(pacer.isTimeFrozen());
        assertFalse(pacer.isTimeStopped());

        pacer.toggleTimeStop();
        assertTrue(pacer.isTimeStopped());
        assertTrue(pacer.isTimeFrozen());

        pacer.toggleTimeStop();
        assertFalse(pacer.isTimeStopped());
        assertFalse(pacer.isTimeFrozen());
    }

    @Test
    void testIdempotentFreezeAndUnfreeze() {
        AtomicLong clock = new AtomicLong(1000L);
        FramePacer pacer = new FramePacer(clock::get);

        pacer.freezeTime();
        assertTrue(pacer.isTimeFrozen());

        pacer.unfreezeTime();
        assertFalse(pacer.isTimeFrozen());

        pacer.unfreezeTime();
        assertFalse(pacer.isTimeFrozen());
    }

    @Test
    void testLastFrameTimeTracking() {
        FramePacer pacer = new FramePacer();
        pacer.setLastFrameTime(54321L);
        assertEquals(54321L, pacer.getLastFrameTime());
    }

    @Test
    void testPrecisionExecutionTimeAccumulator() {
        FramePacer pacer = new FramePacer();
        assertEquals(0f, pacer.getExecutionTimePrecision());

        pacer.addExecutionTimePrecision(0.004f);
        assertEquals(0.004f, pacer.getExecutionTimePrecision(), 1e-6f);

        pacer.addExecutionTimePrecision(-0.002f);
        assertEquals(0.002f, pacer.getExecutionTimePrecision(), 1e-6f);
    }

    @Test
    void testExecutionTimeAccumulator() {
        FramePacer pacer = new FramePacer();
        assertEquals(0L, pacer.getExecutionTime());

        pacer.addExecutionTime(20L);
        assertEquals(20L, pacer.getExecutionTime());

        pacer.addExecutionTime(-10L);
        assertEquals(10L, pacer.getExecutionTime());
    }

    @Test
    void testChecksumCounterAndComplaintFlag() {
        FramePacer pacer = new FramePacer();
        assertEquals(0L, pacer.getChecksumMillisecondCounter());

        pacer.addChecksumMillisecondCounter(20L);
        assertEquals(20L, pacer.getChecksumMillisecondCounter());

        assertTrue(pacer.shouldComplainChecksum());
        pacer.setChecksumComplain(false);
        assertFalse(pacer.shouldComplainChecksum());
    }

    @Test
    void testStatCounters() {
        FramePacer pacer = new FramePacer();
        for (int i = 0; i < 10; i++) {
            pacer.getFrameTimeCounter().updateAbsolute(16);
        }
        for (int i = 0; i < 100; i++) {
            pacer.pathfindsPerTick.updateAbsolute(5);
        }

        assertEquals(16f, pacer.getFrameTimeCounter().getAveragePerUpdate(), 1e-6f);
        assertEquals(16L, pacer.getFrameTimeCounter().getMax());
        assertEquals(5f, pacer.pathfindsPerTick.getAveragePerUpdate(), 1e-6f);
        assertEquals(5L, pacer.pathfindsPerTick.getMax());
    }
}
