package com.oddlabs.tt.client.render;

import com.oddlabs.tt.engine.render.Accessory;
import com.oddlabs.tt.simulation.model.EmojiType;
import com.oddlabs.tt.simulation.model.Model;

import java.util.SequencedCollection;

/**
 * Manages the client-side visual accessories for a simulation model.
 */
public interface VisualModel {
    float DURATION_CHICKEN_CLUCK = 0.8f;
    float DURATION_UNIT_DEATH = 1.5f;
    float DURATION_HARVEST = 1.0f;
    float DURATION_REPAIR = 1.0f;

    Model getModel();

    SequencedCollection<Accessory> getAccessories();

    boolean isExpired();

    void update(float t);

    void close();

    void addVisualSound(EmojiType emoji, float duration, float audioDistance);
}
