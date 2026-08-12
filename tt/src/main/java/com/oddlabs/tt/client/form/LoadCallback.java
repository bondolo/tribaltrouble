package com.oddlabs.tt.client.form;

import com.oddlabs.tt.client.gui.GUIRoot;
import com.oddlabs.tt.client.render.UIRenderer;
import org.jspecify.annotations.NonNull;

@FunctionalInterface
public interface LoadCallback extends com.oddlabs.tt.core.util.LoadCallback<@NonNull GUIRoot, @NonNull UIRenderer> {
    @Override
    @NonNull
    UIRenderer load(@NonNull GUIRoot root);
}
