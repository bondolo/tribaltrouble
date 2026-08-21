package com.oddlabs.tt.client.viewer;

import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.render.UIRenderer;

/**
 * Callback invoked during world loading to initialize UI rendering on the GUI root.
 */
@FunctionalInterface
public interface LoadCallback extends com.oddlabs.tt.base.util.LoadCallback<GUIRoot, UIRenderer> {
    @Override
    UIRenderer load(GUIRoot root);
}
