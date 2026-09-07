package com.oddlabs.tt.input;

import org.junit.jupiter.api.Test;

import java.util.NavigableSet;
import java.util.Properties;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class InputBindingSettingsTest {

    @Test
    void testDefaultBindingsNotNull() {
        InputBindingSettings settings = new InputBindingSettings();
        NavigableSet<InputBinding> bindings = settings.getBindings(GameAction.GLOBAL_MENU);
        assertNotNull(bindings);
        assertFalse(bindings.isEmpty());
        assertEquals("ESCAPE", settings.getBindingString(GameAction.GLOBAL_MENU));
    }

    @Test
    void testCustomBindingPersistence() {
        InputBindingSettings settings = new InputBindingSettings();
        InputBinding customBinding = new InputBinding(Key.SPACE, Set.of(Modifier.CONTROL), GameAction.GLOBAL_MENU);
        settings.setBindings(GameAction.GLOBAL_MENU, Set.of(customBinding));

        Properties props = new Properties();
        settings.saveToProperties(props);

        InputBindingSettings reloaded = new InputBindingSettings();
        reloaded.loadFromProperties(props);

        NavigableSet<InputBinding> reloadedBindings = reloaded.getBindings(GameAction.GLOBAL_MENU);
        assertEquals(1, reloadedBindings.size());
        assertEquals(customBinding, reloadedBindings.first());
    }

    @Test
    void testExportImportJson() {
        InputBindingSettings settings = new InputBindingSettings();
        InputBinding customBinding = new InputBinding(Key.F5, Set.of(Modifier.ALT), GameAction.GLOBAL_SCREENSHOT);
        settings.setBindings(GameAction.GLOBAL_SCREENSHOT, Set.of(customBinding));

        String json = settings.exportBindings();
        assertNotNull(json);

        InputBindingSettings imported = new InputBindingSettings();
        imported.importBindings(json);

        NavigableSet<InputBinding> importedBindings = imported.getBindings(GameAction.GLOBAL_SCREENSHOT);
        assertEquals(1, importedBindings.size());
        assertEquals(customBinding, importedBindings.first());
    }

    @Test
    void testMouseBindingPersistence() {
        InputBindingSettings settings = new InputBindingSettings();
        InputBinding mouseBinding = new InputBinding(ExtendedMouseButton.X2, Set.of(Modifier.SHIFT),
                GameAction.GAMEPLAY_BACK);
        settings.setBindings(GameAction.GAMEPLAY_BACK, Set.of(mouseBinding));

        Properties props = new Properties();
        settings.saveToProperties(props);

        InputBindingSettings reloaded = new InputBindingSettings();
        reloaded.loadFromProperties(props);

        NavigableSet<InputBinding> reloadedBindings = reloaded.getBindings(GameAction.GAMEPLAY_BACK);
        assertEquals(1, reloadedBindings.size());
        assertEquals(mouseBinding, reloadedBindings.first());
    }

    @Test
    void testExportImportJsonWithMouseBinding() {
        InputBindingSettings settings = new InputBindingSettings();
        InputBinding mouseBinding = new InputBinding(ExtendedMouseButton.X1, Set.of(), GameAction.GAMEPLAY_BACK);
        settings.setBindings(GameAction.GAMEPLAY_BACK, Set.of(mouseBinding));

        String json = settings.exportBindings();
        assertNotNull(json);

        InputBindingSettings imported = new InputBindingSettings();
        imported.importBindings(json);

        NavigableSet<InputBinding> importedBindings = imported.getBindings(GameAction.GAMEPLAY_BACK);
        assertEquals(1, importedBindings.size());
        assertEquals(mouseBinding, importedBindings.first());
    }
}
