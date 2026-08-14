package com.oddlabs.tt.engine.render;

import com.oddlabs.tt.simulation.model.Model;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Context interface providing element render states with access to render queues,
 * active camera state, and model response status without coupling to client state.
 */
public interface SceneContext {
    /**
     * Returns the render queues collection for registering draw calls.
     *
     * @return render queues
     */
    @NonNull
    RenderQueues getRenderQueues();

    /**
     * Returns the current camera state, if available.
     *
     * @return camera state or null
     */
    @Nullable
    CameraState getCamera();

    /**
     * Checks if the specified model is currently in a response state (e.g. selection animation).
     *
     * @param model the model to query
     * @return true if responding
     */
    boolean isResponding(@NonNull Model model);
}
