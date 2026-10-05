package com.oddlabs.tt.gui;

import com.oddlabs.tt.engine.render.ModeIconQuads;

/** Configuration metrics and slider button icon for sliders. */
record SliderData(Horizontal slider,
                  ModeIconQuads button,
                  int leftOffset,
                  int rightOffset) {
}
