package com.oddlabs.tt.input;


import java.util.EnumSet;
import java.util.Set;

/**
 * Keyboard modifier keys.
 */
public enum Modifier {
    SHIFT(0x0001 | 0x0002),
    CONTROL(0x0040 | 0x0080),
    ALT(0x0100 | 0x0200),
    META(0x0400 | 0x0800);

    private final int mask;

    Modifier(int mask) {
        this.mask = mask;
    }

    public int getMask() {
        return mask;
    }

    /**
     * Extracts active modifiers from a raw bitmask.
     *
     * @param mask bitmask of active modifiers
     * @return set of active modifiers
     */
    public static Set<Modifier> fromMask(int mask) {
        Set<Modifier> modifiers = EnumSet.noneOf(Modifier.class);
        for (Modifier modifier : values()) {
            if ((mask & modifier.mask) != 0) {
                modifiers.add(modifier);
            }
        }
        return modifiers;
    }
}
