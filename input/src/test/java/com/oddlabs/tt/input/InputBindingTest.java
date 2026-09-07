package com.oddlabs.tt.input;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InputBindingTest {

    @Test
    void testMouseButtonMatching() {
        InputBinding mouseBinding = new InputBinding(ExtendedMouseButton.X1, Set.of(), GameAction.GAMEPLAY_BACK);
        assertTrue(mouseBinding.matches(ExtendedMouseButton.X1, Set.of()));
        assertFalse(mouseBinding.matches(ExtendedMouseButton.X2, Set.of()));
        assertFalse(mouseBinding.matches(ExtendedMouseButton.X1, Set.of(Modifier.SHIFT)));

        KeyboardEvent keyEvent = new KeyboardEvent(Key.ESCAPE, 0, Set.of(), 1);
        assertFalse(mouseBinding.matches(keyEvent));
    }

    @Test
    void testMouseButtonWithModifiersMatching() {
        InputBinding binding = new InputBinding(ExtendedMouseButton.X2, Set.of(Modifier.CONTROL, Modifier.SHIFT),
                GameAction.GAMEPLAY_BACK);
        assertTrue(binding.matches(ExtendedMouseButton.X2, Set.of(Modifier.CONTROL, Modifier.SHIFT)));
        assertFalse(binding.matches(ExtendedMouseButton.X2, Set.of(Modifier.CONTROL)));
        assertFalse(binding.matches(ExtendedMouseButton.X1, Set.of(Modifier.CONTROL, Modifier.SHIFT)));
    }

    @Test
    void testCompareTo() {
        InputBinding mouseX1 = new InputBinding(ExtendedMouseButton.X1, Set.of(), GameAction.GAMEPLAY_BACK);
        InputBinding mouseX2 = new InputBinding(ExtendedMouseButton.X2, Set.of(), GameAction.GAMEPLAY_BACK);
        InputBinding keyBinding = new InputBinding(Key.ESCAPE, Set.of(), GameAction.GAMEPLAY_BACK);

        assertTrue(mouseX1.compareTo(mouseX2) < 0);
        assertTrue(mouseX2.compareTo(mouseX1) > 0);
        assertNotEquals(0, mouseX1.compareTo(keyBinding));
    }
}
