package com.oddlabs.tt.gui;

import com.oddlabs.tt.engine.render.ModeIconQuads;

public record SliderData(Horizontal slider,
                         ModeIconQuads button,
                         int leftOffset,
                         int rightOffset) {
}
