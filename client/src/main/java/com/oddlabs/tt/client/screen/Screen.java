package com.oddlabs.tt.client.screen;

import com.oddlabs.tt.client.render.SceneHoverProvider;
import com.oddlabs.tt.client.render.WorldSceneRenderer;
import com.oddlabs.tt.engine.render.CameraState;
import com.oddlabs.tt.engine.render.state.RenderContext;
import com.oddlabs.tt.gui.GUIRoot;
import org.jspecify.annotations.Nullable;

/**
 * Application screen combining a 2D user interface and an optional 3D scene.
 */
public interface Screen extends AutoCloseable {
    /**
     * Returns the 2D user interface root.
     *
     * @return the GUI root
     */
    GUIRoot guiRoot();

    /**
     * Returns the 3D scene renderer, or null if this screen has no 3D scene.
     *
     * @return the scene renderer
     */
    @Nullable
    WorldSceneRenderer sceneRenderer();

    /**
     * Returns the picking and tooltip provider for the 3D scene.
     *
     * @return the hover provider, or null if hovering is unsupported
     */
    @Nullable
    SceneHoverProvider hoverProvider();

    /**
     * Renders the 3D scene if present, or clears the viewport.
     *
     * @param context the current render context
     * @param camera the camera frustum state
     */
    default void render3D(RenderContext context, CameraState camera) {
        var renderer = sceneRenderer();
        if (renderer != null && !renderer.isClosed()) {
            renderer.render(context, camera, guiRoot());
        } else {
            context.clear(true, true);
        }
    }

    /**
     * Returns a new screen with the specified 2D root and the existing 3D scene.
     *
     * @param newRoot the new GUI root
     * @return the new screen
     */
    default Screen withGUIRoot(GUIRoot newRoot) {
        return new DefaultScreen(newRoot, sceneRenderer(), hoverProvider());
    }

    @Override
    default void close() {
        var renderer = sceneRenderer();
        if (renderer != null) {
            renderer.close();
        }
    }
}
