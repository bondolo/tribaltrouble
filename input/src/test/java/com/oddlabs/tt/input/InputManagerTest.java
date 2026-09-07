package com.oddlabs.tt.input;

import org.junit.jupiter.api.Test;

import java.util.NavigableSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InputManagerTest {

    @Test
    void testDefaultBindingsNotNull() {
        InputManager manager = new InputManager();
        NavigableSet<InputBinding> bindings = manager.getBindings(GameAction.GLOBAL_MENU);
        assertNotNull(bindings);
        assertFalse(bindings.isEmpty());
        assertEquals("ESCAPE", manager.getBindingString(GameAction.GLOBAL_MENU));
    }

    @Test
    void testActionMatchingAndState() {
        InputManager manager = new InputManager();

        KeyboardEvent event = new KeyboardEvent(Key.ESCAPE, 0, Set.of(), 1);
        Set<GameAction> actions = manager.getActions(event);
        assertTrue(actions.contains(GameAction.GLOBAL_MENU));
        assertTrue(actions.contains(GameAction.UI_CANCEL));

        manager.updateState(event, true);
        assertTrue(manager.isActive(GameAction.GLOBAL_MENU));

        manager.updateState(event, false);
        assertFalse(manager.isActive(GameAction.GLOBAL_MENU));
    }

    @Test
    void testInputManagerWithCustomSettings() {
        InputBindingSettings settings = new InputBindingSettings();
        InputBinding customBinding = new InputBinding(Key.SPACE, Set.of(Modifier.CONTROL), GameAction.GLOBAL_MENU);
        settings.setBindings(GameAction.GLOBAL_MENU, Set.of(customBinding));

        InputManager manager = new InputManager(settings);
        KeyboardEvent event = new KeyboardEvent(Key.SPACE, 0, Set.of(Modifier.CONTROL), 1);
        Set<GameAction> actions = manager.getActions(event);
        assertTrue(actions.contains(GameAction.GLOBAL_MENU));
    }

    @Test
    void testExtendedMouseButtonActionsAndState() {
        InputManager manager = new InputManager();

        // Default binding: GAMEPLAY_BACK is bound to X1 without modifiers
        Set<GameAction> actions = manager.getActions(ExtendedMouseButton.X1, Set.of());
        assertTrue(actions.contains(GameAction.GAMEPLAY_BACK));

        // X2 is unbound by default
        Set<GameAction> x2Actions = manager.getActions(ExtendedMouseButton.X2, Set.of());
        assertTrue(x2Actions.isEmpty());

        // Update mouse button state
        manager.updateMouseState(ExtendedMouseButton.X1, true);
        assertTrue(manager.isActive(GameAction.GAMEPLAY_BACK));

        manager.updateMouseState(ExtendedMouseButton.X1, false);
        assertFalse(manager.isActive(GameAction.GAMEPLAY_BACK));
    }

    @Test
    void testActiveModifiersFromKeyState() {
        InputManager manager = new InputManager();
        assertTrue(manager.getActiveModifiers().isEmpty());

        KeyboardEvent shiftDown = new KeyboardEvent(Key.LSHIFT, 0, Set.of(), 1);
        manager.updateState(shiftDown, true);
        assertEquals(Set.of(Modifier.SHIFT), manager.getActiveModifiers());

        KeyboardEvent ctrlDown = new KeyboardEvent(Key.RCONTROL, 0, Set.of(), 1);
        manager.updateState(ctrlDown, true);
        assertEquals(Set.of(Modifier.SHIFT, Modifier.CONTROL), manager.getActiveModifiers());

        manager.updateState(shiftDown, false);
        assertEquals(Set.of(Modifier.CONTROL), manager.getActiveModifiers());

        manager.updateState(ctrlDown, false);
        assertTrue(manager.getActiveModifiers().isEmpty());
    }
}
