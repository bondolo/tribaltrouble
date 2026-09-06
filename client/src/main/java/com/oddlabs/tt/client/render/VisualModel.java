package com.oddlabs.tt.client.render;

import com.oddlabs.tt.engine.render.Accessory;
import com.oddlabs.tt.simulation.model.Model;
import java.util.SequencedCollection;

/**
 * Manages the client-side visual accessories for a simulation model.
 */
public interface VisualModel {
    Model getModel();

    SequencedCollection<Accessory> getAccessories();

    boolean isExpired();

    void update(float t);

    void close();

    void addAccessory(Accessory accessory);
}
