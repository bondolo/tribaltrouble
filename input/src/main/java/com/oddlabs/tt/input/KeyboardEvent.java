package com.oddlabs.tt.input;


import java.util.EnumSet;
import java.util.Set;

/**
 * Represents a keyboard event with key code, character codepoint, and active modifiers.
 */
public record KeyboardEvent(Key keyCode, int keyCodepoint, Set<Modifier> modifiers,
                            int clicks) {
    public KeyboardEvent {

        modifiers = modifiers.isEmpty() ? EnumSet.noneOf(Modifier.class) : EnumSet.copyOf(modifiers);
    }

    public KeyboardEvent(Key keyCode, int keyCodepoint, boolean shiftDown, boolean controlDown,
            boolean altDown, boolean metaDown, int clicks) {
        Set<Modifier> set = EnumSet.noneOf(Modifier.class);
        if (shiftDown) set.add(Modifier.SHIFT);
        if (controlDown) set.add(Modifier.CONTROL);
        if (altDown) set.add(Modifier.ALT);
        if (metaDown) set.add(Modifier.META);

        this(keyCode, keyCodepoint, set, clicks);
    }

    public boolean shiftDown() {
        return modifiers.contains(Modifier.SHIFT);
    }

    public boolean controlDown() {
        return modifiers.contains(Modifier.CONTROL);
    }

    public boolean altDown() {
        return modifiers.contains(Modifier.ALT);
    }

    public boolean metaDown() {
        return modifiers.contains(Modifier.META);
    }
}
