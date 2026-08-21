package com.oddlabs.converter;

import com.oddlabs.geometry.AnimationInfo;

import java.nio.file.Path;

public final class AnimObjectInfo extends ObjectInfo {
    private final float wpc;
    private final AnimationInfo.AnimationType type;
    private final String name;

    public AnimObjectInfo(Path file, float wpc, AnimationInfo.AnimationType type,
            String name) {
        super(file);
        this.wpc = wpc;
        this.type = type;
        this.name = name;
    }

    public AnimationInfo.AnimationType getType() {
        return type;
    }

    public float getWPC() {
        return wpc;
    }

    public String getName() {
        return name;
    }
}
