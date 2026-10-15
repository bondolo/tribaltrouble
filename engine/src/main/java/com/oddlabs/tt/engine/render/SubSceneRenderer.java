package com.oddlabs.tt.engine.render;


import com.oddlabs.tt.engine.render.state.ScenePassContext;

/**
 * Renders an individual sub-scene element or component within a broader render pass.
 */
@FunctionalInterface
public interface SubSceneRenderer {
    void render(ScenePassContext pass);
}
