package com.oddlabs.geometry;

import java.io.Serializable;

public final strictfp class AnimationInfo implements Serializable {
	private final static long serialVersionUID = 1;

    /**
     * Type of animation
     */
    public enum Type {
        LOOP,
        PLAIN
    };

	private final float[][] frames;
	private final Type type;
	private final float wpc;

	public AnimationInfo(float[][] frames, Type type, float wpc) {
		this.frames = frames;
		this.type = type;
		this.wpc = wpc;
	}

    @SuppressWarnings("ReturnOfCollectionOrArrayField")
	public float[][] getFrames() {
		return frames;
	}

	public Type getType() {
		return type;
	}

	public float getWPC() {
		return wpc;
	}
}
