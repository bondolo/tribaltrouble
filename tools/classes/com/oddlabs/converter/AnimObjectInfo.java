package com.oddlabs.converter;

import com.oddlabs.geometry.AnimationInfo;
import java.io.*;

public final strictfp class AnimObjectInfo extends ObjectInfo {
	private final float wpc;
	private final AnimationInfo.Type type;

	public AnimObjectInfo(File file, float wpc, AnimationInfo.Type type) {
		super(file);
		this.wpc = wpc;
		this.type = type;
	}

	public final AnimationInfo.Type getType() {
		return type;
	}

	public final float getWPC() {
		return wpc;
	}
}
