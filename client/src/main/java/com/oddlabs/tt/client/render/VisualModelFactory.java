package com.oddlabs.tt.client.render;

import com.oddlabs.tt.audio.AudioImplementation;
import com.oddlabs.tt.simulation.model.Model;

/**
 * Create a {@link VisualModel} decorator for a simulation {@link Model}.
 *
 * @param <M> the simulation model type
 */
@FunctionalInterface
public interface VisualModelFactory<M extends Model> {
    /**
     * Creates a decorated {@link VisualModel} for the given simulation model and audio system.
     *
     * @param model the simulation model instance
     * @param audio the active audio implementation
     * @return decorated VisualModel instance
     */
    VisualModel create(M model, AudioImplementation audio);
}
