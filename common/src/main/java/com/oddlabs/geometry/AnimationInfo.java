package com.oddlabs.geometry;


import java.io.Serial;
import java.io.Serializable;

public final class AnimationInfo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1;

    public enum AnimationType {
        LOOP,
        PLAIN
    }

    private final float[][] frames;
    private final AnimationType type;
    private final float wpc;
    private final String name;

    public AnimationInfo(float[][] frames, AnimationType type, float wpc,
            String name) {
        this.frames = frames;
        this.type = type;
        this.wpc = wpc;
        this.name = name;
    }

    public float[][] getFrames() {
        return frames;
    }

    public AnimationType getType() {
        return type;
    }

    public float getWPC() {
        return wpc;
    }

    public String getName() {
        return name;
    }
}
