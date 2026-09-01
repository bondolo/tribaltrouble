package com.oddlabs.tt.engine.render;

import com.oddlabs.net.TimeManager;
import com.oddlabs.tt.base.util.StatCounter;
import org.jspecify.annotations.NullMarked;

import java.util.logging.Logger;

/**
 * Manages client wall-clock timing, frame pacing, time warping, and freeze controls.
 */
@NullMarked
public final class FramePacer {
    private static final Logger logger = Logger.getLogger(FramePacer.class.getName());

    public final StatCounter pathfindsPerTick = new StatCounter(100);
    private final StatCounter frameTime = new StatCounter(10);
    private final TimeManager timeSource;

    private long current_time;
    private long last_frame_time;
    private long execution_time = 0;
    private float execution_time_precision = 0;
    private long time_warp;
    private boolean time_stopped;
    private boolean time_frozen;
    private long frozen_start_time;
    private long frozen_start_time_warped;
    private long checksum_millisecond_counter;
    private boolean checksum_complain = true;

    public FramePacer() {
        this(TimeManager.DEFAULT);
    }

    public FramePacer(TimeManager timeSource) {
        this.timeSource = timeSource;
        current_time = getSystemTime();
        last_frame_time = current_time;
        freezeTime();
    }

    public void warpTime(long warp_delta) {
        time_warp += warp_delta;
    }

    public long getSystemTime() {
        return time_frozen ? frozen_start_time_warped : timeSource.getMillis() + time_warp;
    }

    public void toggleTimeStop() {
        if (time_frozen) {
            unfreezeTime();
        } else {
            freezeTime();
        }
        time_stopped = time_frozen;
        logger.config("time_stopped = " + time_stopped);
    }

    public boolean isTimeFrozen() {
        return time_frozen;
    }

    public boolean isTimeStopped() {
        return time_stopped;
    }

    public void unfreezeTime() {
        if (!time_frozen) {
            return;
        }
        time_frozen = false;
        time_warp -= timeSource.getMillis() - frozen_start_time;
    }

    public void freezeTime() {
        if (time_frozen) {
            return;
        }
        frozen_start_time_warped = getSystemTime();
        time_frozen = true;
        frozen_start_time = timeSource.getMillis();
    }

    public long getLastFrameTime() {
        return last_frame_time;
    }

    public void setLastFrameTime(long frameTime) {
        last_frame_time = frameTime;
    }

    public StatCounter getFrameTimeCounter() {
        return frameTime;
    }

    public float getExecutionTimePrecision() {
        return execution_time_precision;
    }

    public void addExecutionTimePrecision(float delta) {
        execution_time_precision += delta;
    }

    public long getExecutionTime() {
        return execution_time;
    }

    public void addExecutionTime(long delta) {
        execution_time += delta;
    }

    public long getChecksumMillisecondCounter() {
        return checksum_millisecond_counter;
    }

    public void addChecksumMillisecondCounter(long delta) {
        checksum_millisecond_counter += delta;
    }

    public boolean shouldComplainChecksum() {
        return checksum_complain;
    }

    public void setChecksumComplain(boolean complain) {
        checksum_complain = complain;
    }
}
