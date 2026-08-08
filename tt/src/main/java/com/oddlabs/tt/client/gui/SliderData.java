package com.oddlabs.tt.client.gui;

import org.jspecify.annotations.NonNull;

record SliderData(@NonNull Horizontal slider,
                  @NonNull ModeIconQuads button,
                  int leftOffset,
                  int rightOffset) {
}
