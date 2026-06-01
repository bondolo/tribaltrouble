package com.oddlabs.tt.model;

import com.oddlabs.tt.render.SpriteKey;
import org.jspecify.annotations.NonNull;

/**
 * Callbacks that the simulation can trigger on the client-side state.
 */
public interface ModelClient extends ClientState {
    float DURATION_CHICKEN_CLUCK = 0.8f;
    float DURATION_CHICKEN_DEATH = 1.2f;
    float DURATION_UNIT_DEATH = 1.5f;
    float DURATION_HARVEST = 1.0f;
    float DURATION_REPAIR = 1.0f;

    /**
     * Spawns a visual sound emoji/thought accessory on the model.
     *
     * @param sprite        The sprite key of the emoji/thought.
     * @param duration      The duration to display the thought.
     * @param audioDistance The maximum distance at which the sound/visual is audible.
     */
    void addVisualSound(@NonNull SpriteKey sprite, float duration, float audioDistance);
}
