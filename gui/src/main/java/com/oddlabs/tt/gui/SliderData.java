package com.oddlabs.tt.gui;

import com.oddlabs.tt.engine.render.ModeIconQuads;
import org.jspecify.annotations.NonNull;

public record SliderData(@NonNull Horizontal slider,
                         @NonNull ModeIconQuads button,
                         int leftOffset,
                         int rightOffset) {
}
