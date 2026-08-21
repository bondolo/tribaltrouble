package com.oddlabs.tt.input;

import org.jspecify.annotations.NonNull;

import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Set;

/**
 * Manages game action input evaluation and runtime polling state.
 */
public final class InputManager {
    private final @NonNull InputBindingSettings settings;
    private final Set<@NonNull GameAction> activeActions = EnumSet.noneOf(GameAction.class);
    private final Map<@NonNull Key, @NonNull Set<@NonNull GameAction>> keyState = new EnumMap<>(Key.class);

    public InputManager(@NonNull InputBindingSettings settings) {
        this.settings = Objects.requireNonNull(settings);
    }

    public InputManager() {
        this(new InputBindingSettings());
    }

    public @NonNull InputBindingSettings getSettings() {
        return settings;
    }

    public @NonNull NavigableSet<@NonNull InputBinding> getBindings(@NonNull GameAction action) {
        return settings.getBindings(action);
    }

    public @NonNull String getBindingString(@NonNull GameAction action) {
        return settings.getBindingString(action);
    }

    public @NonNull NavigableSet<@NonNull InputBinding> getDefaultBindings(@NonNull GameAction action) {
        return settings.getDefaultBindings(action);
    }

    public void setBindings(@NonNull GameAction action, @NonNull Collection<@NonNull InputBinding> newBindings) {
        settings.setBindings(action, newBindings);
    }

    public void resetToDefaults() {
        settings.resetToDefaults();
    }

    public @NonNull String exportBindings() {
        return settings.exportBindings();
    }

    public void importBindings(@NonNull String json) {
        settings.importBindings(json);
    }

    public @NonNull Set<@NonNull GameAction> getActions(@NonNull KeyboardEvent event) {
        Set<GameAction> actions = EnumSet.noneOf(GameAction.class);
        for (InputBinding binding : settings.getAllBindings()) {
            if (binding.matches(event)) {
                actions.add(binding.action());
            }
        }
        return actions;
    }

    // Called by LocalInput or InputState to update polling state
    public void updateState(@NonNull KeyboardEvent event, boolean pressed) {
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

    public boolean isActive(@NonNull GameAction action) {
        return activeActions.contains(action);
    }

    public void reset() {
        activeActions.clear();
        keyState.clear();
    }
}
