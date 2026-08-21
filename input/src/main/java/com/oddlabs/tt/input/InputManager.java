package com.oddlabs.tt.input;


import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;

/**
 * Manages game action input evaluation and runtime polling state.
 */
public final class InputManager {
    private final InputBindingSettings settings;
    private final ControlSettings controlSettings;
    private final Set<GameAction> activeActions = EnumSet.noneOf(GameAction.class);
    private final Map<Key, Set<GameAction>> keyState = new EnumMap<>(Key.class);

    public InputManager(InputBindingSettings settings, ControlSettings controlSettings) {
        this.settings = settings;
        this.controlSettings = controlSettings;
    }

    public InputManager(InputBindingSettings settings) {
        this(settings, new ControlSettings());
    }

    public InputManager() {
        this(new InputBindingSettings(), new ControlSettings());
    }

    public ControlSettings getControlSettings() {
        return controlSettings;
    }

    public InputBindingSettings getSettings() {
        return settings;
    }

    public NavigableSet<InputBinding> getBindings(GameAction action) {
        return settings.getBindings(action);
    }

    public String getBindingString(GameAction action) {
        return settings.getBindingString(action);
    }

    public NavigableSet<InputBinding> getDefaultBindings(GameAction action) {
        return settings.getDefaultBindings(action);
    }

    public void setBindings(GameAction action, Collection<InputBinding> newBindings) {
        settings.setBindings(action, newBindings);
    }

    public void resetToDefaults() {
        settings.resetToDefaults();
    }

    public String exportBindings() {
        return settings.exportBindings();
    }

    public void importBindings(String json) {
        settings.importBindings(json);
    }

    public Set<GameAction> getActions(KeyboardEvent event) {
        Set<GameAction> actions = EnumSet.noneOf(GameAction.class);
        for (InputBinding binding : settings.getAllBindings()) {
            if (binding.matches(event)) {
                actions.add(binding.action());
            }
        }
        return actions;
    }

    // Called by LocalInput or InputState to update polling state
    public void updateState(KeyboardEvent event, boolean pressed) {
        if (pressed) {
            Set<GameAction> actions = getActions(event);
            if (!actions.isEmpty()) {
                keyState.put(event.keyCode(), actions);
                activeActions.addAll(actions);
            }
        } else {
            Set<GameAction> actions = keyState.remove(event.keyCode());
            if (actions != null) {
                activeActions.removeAll(actions);
            }
        }
    }

    public boolean isActive(GameAction action) {
        return activeActions.contains(action);
    }

    public void reset() {
        activeActions.clear();
        keyState.clear();
    }
}
