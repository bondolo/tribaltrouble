package com.oddlabs.tt.input;

import org.jspecify.annotations.Nullable;

/**
 * Extended mouse buttons beyond the three primary GUI buttons (left, right, middle).
 * <p>
 * These correspond to SDL3's {@code SDL_BUTTON_X1} and {@code SDL_BUTTON_X2},
 * typically the thumb "back" and "forward" buttons found on most mice with side buttons.
 * Button indices use the 0-based remapping from {@code LWJGL3InputProvider}.
 */
public enum ExtendedMouseButton {
    X1(3, "Mouse4"),
    X2(4, "Mouse5");

    private final int buttonIndex;
    private final String displayName;

    ExtendedMouseButton(int buttonIndex, String displayName) {
        this.buttonIndex = buttonIndex;
        this.displayName = displayName;
    }

    public int getButtonIndex() {
        return buttonIndex;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Looks up an extended mouse button by its 0-based button index.
     *
     * @param index the button index from the input provider
     * @return the matching extended mouse button, or {@code null} if not an extended button
     */
    public static @Nullable ExtendedMouseButton fromIndex(int index) {
        return switch (index) {
            case 3 -> X1;
            case 4 -> X2;
            default -> null;
        };
    }
}
