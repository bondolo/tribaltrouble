package com.oddlabs.tt.client.gui;

import com.oddlabs.tt.engine.font.Font;
import org.jspecify.annotations.NonNull;

record ProgressBarData(@NonNull Horizontal progressBar,
                       @NonNull ModeIconQuads leftFill,
                       @NonNull ModeIconQuads centerFill,
                       @NonNull ModeIconQuads rightFill,
                       @NonNull Font font) {

}
