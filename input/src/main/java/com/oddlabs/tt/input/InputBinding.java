package com.oddlabs.tt.input;


import java.io.Serializable;
import java.util.EnumSet;
import java.util.Set;

import org.jspecify.annotations.Nullable;

/**
 * Binds a key, character, or extended mouse button (with modifiers) to a game action.
 */
public record InputBinding(Key key, int codepoint, @Nullable ExtendedMouseButton mouseButton,
                           Set<Modifier> modifiers, GameAction action)
        implements Comparable<InputBinding>, Serializable {
    private static final boolean IS_MACOS = System.getProperty("os.name", "").toLowerCase().contains("mac");

    public InputBinding(Key key, Set<Modifier> modifiers, GameAction action) {
        this(key, -1, null, modifiers, action);
    }

    public InputBinding(Key key, int codepoint, Set<Modifier> modifiers, GameAction action) {
        this(key, codepoint, null, modifiers, action);
    }

    public InputBinding(ExtendedMouseButton mouseButton, Set<Modifier> modifiers, GameAction action) {
        this(Key.KEY_UNKNOWN, -1, mouseButton, modifiers, action);
    }

    public InputBinding {
        modifiers = modifiers.isEmpty() ? EnumSet.noneOf(Modifier.class) : EnumSet.copyOf(modifiers);
    }

    public boolean matches(KeyboardEvent event) {
        if (mouseButton != null) {
            return false;
        }
        return ((codepoint != -1) && (event.keyCodepoint() != -1))
                ? event.keyCodepoint() == codepoint
                : event.keyCode() == key && modifiers.equals(event.modifiers());
    }

    public boolean matches(ExtendedMouseButton button, Set<Modifier> activeModifiers) {
        return mouseButton == button && modifiers.equals(activeModifiers);
    }

    public boolean shift() {
        return modifiers.contains(Modifier.SHIFT);
    }

    public boolean control() {
        return modifiers.contains(Modifier.CONTROL);
    }

    public boolean alt() {
        return modifiers.contains(Modifier.ALT);
    }

    public boolean meta() {
        return modifiers.contains(Modifier.META);
    }

    @Override
    public String toString() {
        if (codepoint != -1) {
            return String.valueOf((char) codepoint);
        }
        String s = formatModifierPrefix();
        if (mouseButton != null) {
            return s + mouseButton.getDisplayName();
        }
        return s + key().getDisplayName();
    }

    private String formatModifierPrefix() {
        String s = "";
        if (IS_MACOS) {
            if (control()) s = s + "⌃";
            if (alt()) s = s + "⌥";
            if (shift()) s = s + "⇧";
            if (meta()) s = s + "⌘";
        } else {
            if (control()) s = s + "Ctrl+";
            if (alt()) s = s + "Alt+";
            if (shift()) s = s + "Shift+";
            if (meta()) s = s + "Meta+";
        }
        return s;
    }

    @Override
    public int compareTo(InputBinding o) {
        int mouseCompare = compareMouseButtons(this.mouseButton, o.mouseButton);
        if (mouseCompare != 0) {
            return mouseCompare;
        }
        if (this.codepoint != o.codepoint) {
            return Integer.compare(this.codepoint, o.codepoint);
        }
        int keyCompare = this.key().getDisplayName().compareTo(o.key().getDisplayName());
        if (keyCompare != 0) {
            return keyCompare;
        }
        if (this.control() != o.control()) {
            return this.control() ? 1 : -1;
        }
        if (this.alt() != o.alt()) {
            return this.alt() ? 1 : -1;
        }
        if (this.shift() != o.shift()) {
            return this.shift() ? 1 : -1;
        }
        if (this.meta() != o.meta()) {
            return this.meta() ? 1 : -1;
        }

        return this.action().compareTo(o.action());
    }

    private static int compareMouseButtons(@Nullable ExtendedMouseButton a, @Nullable ExtendedMouseButton b) {
        if (a == b) {
            return 0;
        }
        if (a == null) {
            return -1;
        }
        if (b == null) {
            return 1;
        }
        return a.compareTo(b);
    }
}
