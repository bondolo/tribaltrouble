package com.oddlabs.tt.render;

import com.oddlabs.geometry.AnimationInfo;
import com.oddlabs.tt.util.BoundingBox;

public final strictfp class SpriteKey extends RenderQueueKey {
	private final BoundingBox[] bounds;
	private final AnimationInfo.Type[] anim_types;

	SpriteKey(int key, BoundingBox[] bounds, AnimationInfo.Type[] anim_types) {
		super(key);
		this.bounds = bounds;
		this.anim_types = anim_types;
	}

	public BoundingBox getBounds(int anim_index) {
		return bounds[anim_index];
	}

	public AnimationInfo.Type getAnimationType(int anim) {
		return anim_types[anim];
	}
}
