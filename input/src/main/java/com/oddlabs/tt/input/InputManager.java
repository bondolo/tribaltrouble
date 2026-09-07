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
    private final Set<GameAction> activeActions = EnumSet.noneOf(GameAction.class);
    private final Map<Key, Set<GameAction>> keyState = new EnumMap<>(Key.class);
    private final Map<ExtendedMouseButton, Set<GameAction>> mouseButtonState = new EnumMap<>(ExtendedMouseButton.class);

    public InputManager(InputBindingSettings settings) {
        this.settings = settings;
    }

    public InputManager() {
        this(new InputBindingSettings());
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

    public Set<GameAction> getActions(ExtendedMouseButton button, Set<Modifier> modifiers) {
        Set<GameAction> actions = EnumSet.noneOf(GameAction.class);
        for (InputBinding binding : settings.getAllBindings()) {
            if (binding.matches(button, modifiers)) {
                actions.add(binding.action());
            }
        }
        return actions;
    }

    public void updateState(KeyboardEvent event, boolean pressed) {
        if (pressed) {
            Set<GameAction> actions = getActions(event);
            keyState.put(event.keyCode(), actions);
            if (!actions.isEmpty()) {
                activeActions.addAll(actions);
            }
        } else {
            Set<GameAction> actions = keyState.remove(event.keyCode());
            if (actions != null) {
                activeActions.removeAll(actions);
            }
        }
    }

    public void updateMouseState(ExtendedMouseButton button, boolean pressed) {
        if (pressed) {
            Set<GameAction> actions = getActions(button, getActiveModifiers());
            if (!actions.isEmpty()) {
                mouseButtonState.put(button, actions);
                activeActions.addAll(actions);
            }
        } else {
            Set<GameAction> actions = mouseButtonState.remove(button);
            if (actions != null) {
                activeActions.removeAll(actions);
            }
        }
    }

    /**
     * Derives the currently active keyboard modifiers from tracked key state.
     */
    public Set<Modifier> getActiveModifiers() {
        Set<Modifier> modifiers = EnumSet.noneOf(Modifier.class);
        for (Key key : keyState.keySet()) {
            switch (key) {
                case LSHIFT, RSHIFT -> modifiers.add(Modifier.SHIFT);
                case LCONTROL, RCONTROL -> modifiers.add(Modifier.CONTROL);
                case LALT, RALT -> modifiers.add(Modifier.ALT);
                case LSUPER, RSUPER -> modifiers.add(Modifier.META);
                default -> {
                }
            }
        }
        return modifiers;
    }

    public boolean isActive(GameAction action) {
        return activeActions.contains(action);
    }

    public void reset() {
        activeActions.clear();
        keyState.clear();
        mouseButtonState.clear();
    }
}
