package com.oddlabs.tt.base.animation;

import com.oddlabs.tt.base.event.StateChecksum;

/**
 * Executes periodic timer callbacks managed by an animation manager.
 */
public final class TimerAnimation implements Animated {
    private final AnimationManager manager;
    private final Updatable<TimerAnimation> timer_owner;
    private float time = 0;
    private float interval;
    private boolean running = false;

    public TimerAnimation(AnimationManager manager, Updatable<TimerAnimation> owner, float interval) {
        this.manager = manager;
        this.timer_owner = owner;
        this.interval = interval;
    }

    @Override
    public String toString() {
        return "TimerAnimation{owner=" + timer_owner + "}";
    }

    @Override
    public void updateChecksum(StateChecksum checksum) {
        checksum.update(time);
    }

    public boolean isRunning() {
        return running;
    }

    public void stop() {
        running = false;
        manager.removeAnimation(this);
    }

    public void start() {
        running = true;
        manager.registerAnimation(this);
    }

    public void setTimerInterval(float interval) {
        this.interval = interval;
    }

    public void resetTime() {
        time = 0;
    }

    @Override
    public void animate(float dt) {
        time += dt;
        while (time > interval) {
            time -= Math.max(dt, interval);
            timer_owner.update(this);
        }
    }
}
