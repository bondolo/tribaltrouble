package com.oddlabs.tt.client.form;

import com.oddlabs.tt.client.gui.GUIRoot;
import com.oddlabs.tt.render.UIRenderer;
import org.jspecify.annotations.NonNull;

@FunctionalInterface
public interface LoadCallback {
    UIRenderer load(@NonNull GUIRoot gui_root);
}
