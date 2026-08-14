package com.oddlabs.tt.base.animation;

import com.oddlabs.net.MonotoneTimeManager;
import com.oddlabs.tt.base.util.StatCounter;
import com.oddlabs.tt.base.event.StateChecksum;
import org.jspecify.annotations.NonNull;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.logging.Logger;

/**
 * Manages game tick timing, time warping, and tick animation listener callbacks.
 */
public final class AnimationManager {
    private static final Logger logger = Logger.getLogger(AnimationManager.class.getName());
    public static final long ANIMATION_MILLISECONDS_PER_TICK = 20L;
    public static final long ANIMATION_MILLISECONDS_PER_PRECISION_TICK = ANIMATION_MILLISECONDS_PER_TICK / 5;
    public static final float ANIMATION_SECONDS_PER_TICK = ANIMATION_MILLISECONDS_PER_TICK / 1000f;
    public static final float ANIMATION_SECONDS_PER_PRECISION_TICK = ANIMATION_MILLISECONDS_PER_PRECISION_TICK / 1000f;
    public static final long ANIMATION_MILLISECONDS_PER_CHECKSUM = TimeUnit.SECONDS.toMillis(2);
    public static final long MAX_STEP_MILLIS = TimeUnit.SECONDS.toMillis(30);

    public static final StatCounter pathfindsPerTick = new StatCounter(100);

    private static final StatCounter frameTime = new StatCounter(10);
    private static final MonotoneTimeManager timeSource = new MonotoneTimeManager(() -> TimeUnit.NANOSECONDS.toMillis(
            System.nanoTime()));

    private static long current_time;
    private static long last_frame_time;
    private static long execution_time = 0;
    private static float execution_time_precision = 0;
    private static long time_warp;
    private static boolean time_stopped;
    private static boolean time_frozen;
    private static long frozen_start_time;
    private static long frozen_start_time_warped;
    private static long checksum_millisecond_counter;
    private static boolean checksum_complain = true;

    private final Set<@NonNull Animated> animations = new CopyOnWriteArraySet<>();
    private final Set<@NonNull Animated> deleted_animations = new CopyOnWriteArraySet<>();

    private int tick;

    static {
        current_time = getSystemTime();
        last_frame_time = current_time;
        freezeTime();
    }

    public static void warpTime(long warp_delta) {
        time_warp += warp_delta;
    }

    public static long getSystemTime() {
        return time_frozen ? frozen_start_time_warped : timeSource.getMillis() + time_warp;
    }

    public static void toggleTimeStop() {
        if (time_frozen)
            unfreezeTime();
        else
            freezeTime();
        time_stopped = time_frozen;
        logger.config("time_stopped = " + time_stopped);
    }

    public static boolean isTimeFrozen() {
        return time_frozen;
    }

    public static boolean isTimeStopped() {
        return time_stopped;
    }

    public static void unfreezeTime() {
        if (!time_frozen)
            return;
        time_frozen = false;
        time_warp -= timeSource.getMillis() - frozen_start_time;
    }

    public static void freezeTime() {
        if (time_frozen)
            return;
        frozen_start_time_warped = getSystemTime();
        time_frozen = true;
        frozen_start_time = timeSource.getMillis();
    }

    public static long getLastFrameTime() {
        return last_frame_time;
    }

    public static void setLastFrameTime(long frameTime) {
        last_frame_time = frameTime;
    }

    public static StatCounter getFrameTimeCounter() {
        return frameTime;
    }

    public static float getExecutionTimePrecision() {
        return execution_time_precision;
    }

    public static void addExecutionTimePrecision(float delta) {
        execution_time_precision += delta;
    }

    public static long getExecutionTime() {
        return execution_time;
    }

    public static void addExecutionTime(long delta) {
        execution_time += delta;
    }

    public static long getChecksumMillisecondCounter() {
        return checksum_millisecond_counter;
    }

    public static void addChecksumMillisecondCounter(long delta) {
        checksum_millisecond_counter += delta;
    }

    public static boolean shouldComplainChecksum() {
        return checksum_complain;
    }

    public static void setChecksumComplain(boolean complain) {
        checksum_complain = complain;
    }

    public int getTick() {
        return tick;
    }

    public void registerAnimation(@NonNull Animated anim) {
        deleted_animations.remove(anim);
        animations.add(anim);
    }

    public void removeAnimation(@NonNull Animated anim) {
        if (animations.contains(anim)) {
            deleted_animations.add(anim);
        }
    }

    private void flushAnimations() {
        animations.removeAll(deleted_animations);
        deleted_animations.clear();
    }

    public void updateChecksum(@NonNull StateChecksum checksum) {
        flushAnimations();
        animations.forEach(anim -> anim.updateChecksum(checksum));
    }

    public void runAnimations(float t) {
        tick++;
        flushAnimations();
        Predicate<Animated> notDeleted = ((Predicate<Animated>) deleted_animations::contains).negate();
        animations.stream()
                .filter(notDeleted)
                .forEach(a -> a.animate(t));
    }

    public void debugPrintAnimations() {
        flushAnimations();
        animations.forEach(anim -> logger.fine("anim = " + anim));
    }
}
