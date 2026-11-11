package com.oddlabs.tt.client.render;

import com.oddlabs.tt.engine.render.Accessory;
import com.oddlabs.tt.simulation.model.ClientModel;
import com.oddlabs.tt.simulation.model.Model;
import java.util.SequencedCollection;

/**
 * Manages the client-side visual accessories for a simulation model.
 */
public interface VisualModel extends ClientModel {
    Model getModel();

    SequencedCollection<Accessory> getAccessories();

    boolean isExpired();

    /**
     * update the visual model
     *
     * @param dt time delta since last update
     */
    void update(float dt);

    @Override
    void close();

    void addAccessory(Accessory accessory);

    /**
     * Renders or picks this visual model in the current scene context.
     *
     * @param renderState the active render state
     */
    void visit(RenderState renderState);
}
