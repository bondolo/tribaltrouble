package com.oddlabs.tt.client.render;

import com.oddlabs.tt.base.animation.Animated;
import com.oddlabs.tt.base.animation.AnimationManager;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Tracks and expires temporary highlight or response visual states for objects.
 */
public final class RespondManager implements Animated {
    private static final float SECONDS_PER_PICK_RESPOND = 1f / 3f;

    private final NavigableMap<Timeout, Object> respond_timeouts = new TreeMap<>();
    private final Map<Object, Timeout> respond_targets = new HashMap<>();

    private int current_id;
    private float time;

    public RespondManager(AnimationManager manager) {
        manager.registerAnimation(this);
    }

    @Override
    public void animate(float dt) {
        time += dt;
        timeout();
    }

    private void timeout() {
        Map.Entry<Timeout, Object> entry;
        while ((entry = respond_timeouts.firstEntry()) != null && entry.getKey().timeout <= time) {
            respond_timeouts.pollFirstEntry();
            respond_targets.remove(entry.getValue());
            if (entry.getKey().stop_action != null) {
                entry.getKey().stop_action.run();
            }
        }
    }

    public void addResponder(Object target) {
        addResponder(target, null);
    }

    public void addResponder(Object target, @Nullable Runnable stop_action) {
        addResponder(SECONDS_PER_PICK_RESPOND, target, stop_action);
    }

    private void addResponder(float respond_time, Object target, @Nullable Runnable stop_action) {
        removeResponder(target);
        Timeout timeout = new Timeout(time + respond_time, current_id++, target, stop_action);
        respond_targets.put(target, timeout);
        respond_timeouts.put(timeout, target);
    }

    private void removeResponder(Object target) {
        Timeout timeout = respond_targets.remove(target);
        if (timeout != null) {
            respond_timeouts.remove(timeout);
            if (timeout.stop_action != null) {
                timeout.stop_action.run();
            }
        }
    }

    boolean isResponding(Object target) {
        if (respond_targets.isEmpty()) {
            return false;
        }
        return isResponding(respond_targets.get(target));
    }

    private boolean isResponding(@Nullable Timeout timeout) {
        if (timeout == null) {
            return false;
        }
        float time_diff = timeout.timeout - time;
        float blink = SECONDS_PER_PICK_RESPOND / 4f;
        return time_diff > 0 && (time_diff >= SECONDS_PER_PICK_RESPOND - blink || time_diff <= blink);
    }

    private record Timeout(float timeout, int id, Object target,
                           @Nullable Runnable stop_action) implements Comparable<Timeout> {

        @Override
        public boolean equals(@Nullable Object other) {
            if (other instanceof Timeout timeout_obj) {
                return timeout_obj.timeout == timeout && timeout_obj.id == id;
            }
            return false;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 59 * hash + Float.floatToIntBits(this.timeout);
            hash = 59 * hash + this.id;
            return hash;
        }

        @Override
        public int compareTo(Timeout other) {
            int c = Float.compare(timeout, other.timeout);
            return c != 0 ? c : Integer.compare(id, other.id);
        }
    }
}
