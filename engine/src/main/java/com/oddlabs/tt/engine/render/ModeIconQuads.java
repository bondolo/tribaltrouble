package com.oddlabs.tt.engine.render;


import java.util.EnumMap;

/**
 * A related set of icons for different {@link ModeIconQuads.Mode}s.
 */
public class ModeIconQuads extends EnumMap<ModeIconQuads.Mode, IconQuad> {

    public enum Mode {
        NORMAL,
        ACTIVE,
        DISABLED
    }

    public ModeIconQuads(IconQuad normal, IconQuad active, IconQuad disabled) {
        super(Mode.class);
        put(Mode.NORMAL, normal);
        put(Mode.ACTIVE, active);
        put(Mode.DISABLED, disabled);
    }

    public IconQuad quad(Mode skinMode) {
        return get(skinMode);
    }
}
