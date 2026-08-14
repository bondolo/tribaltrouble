package com.oddlabs.tt.gui.form;

import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.client.render.UIRenderer;
import org.jspecify.annotations.NonNull;

@FunctionalInterface
public interface LoadCallback extends com.oddlabs.tt.base.util.LoadCallback<@NonNull GUIRoot, @NonNull UIRenderer> {
    @Override
    @NonNull
    UIRenderer load(@NonNull GUIRoot root);
}
