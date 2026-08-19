package com.oddlabs.tt.client.viewer;

import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.render.UIRenderer;
import org.jspecify.annotations.NonNull;

/**
 * Callback invoked during world loading to initialize UI rendering on the GUI root.
 */
@FunctionalInterface
public interface LoadCallback extends com.oddlabs.tt.base.util.LoadCallback<@NonNull GUIRoot, @NonNull UIRenderer> {
    @Override
    @NonNull
    UIRenderer load(@NonNull GUIRoot root);
}
