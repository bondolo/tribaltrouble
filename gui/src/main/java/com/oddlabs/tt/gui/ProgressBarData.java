package com.oddlabs.tt.gui;

import com.oddlabs.tt.engine.font.Font;
import com.oddlabs.tt.engine.render.ModeIconQuads;
import org.jspecify.annotations.NonNull;

public record ProgressBarData(@NonNull Horizontal progressBar,
                              @NonNull ModeIconQuads leftFill,
                              @NonNull ModeIconQuads centerFill,
                              @NonNull ModeIconQuads rightFill,
                              @NonNull Font font) {

}
