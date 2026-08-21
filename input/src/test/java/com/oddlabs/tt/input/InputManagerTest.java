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
}
