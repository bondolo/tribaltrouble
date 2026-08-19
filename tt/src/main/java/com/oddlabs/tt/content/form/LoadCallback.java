package com.oddlabs.tt.content.form;

import com.oddlabs.tt.gui.*;
import com.oddlabs.tt.gui.event.*;
import com.oddlabs.tt.client.gui.*;

import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.render.UIRenderer;
import org.jspecify.annotations.NonNull;

@FunctionalInterface
public interface LoadCallback extends com.oddlabs.tt.base.util.LoadCallback<@NonNull GUIRoot, @NonNull UIRenderer> {
    @Override
    @NonNull
    UIRenderer load(@NonNull GUIRoot root);
}
