package com.oddlabs.tt.client.screen;

import com.oddlabs.tt.client.render.SceneHoverProvider;
import com.oddlabs.tt.client.render.WorldSceneRenderer;
import com.oddlabs.tt.gui.GUIRoot;
import org.jspecify.annotations.Nullable;

/**
 * Default immutable {@link Screen} implementation.
 */
public record DefaultScreen(
                            GUIRoot guiRoot,
                            @Nullable WorldSceneRenderer sceneRenderer,
                            @Nullable SceneHoverProvider hoverProvider
) implements Screen {
    public DefaultScreen(GUIRoot guiRoot, @Nullable WorldSceneRenderer sceneRenderer) {
        this(guiRoot, sceneRenderer, sceneRenderer instanceof SceneHoverProvider hover ? hover : null);
    }

    public DefaultScreen(GUIRoot guiRoot) {
        this(guiRoot, null, null);
    }

    @Override
    public Screen withGUIRoot(GUIRoot newRoot) {
        return new DefaultScreen(newRoot, sceneRenderer, hoverProvider);
    }
}
