package com.oddlabs.tt.gui;

import com.oddlabs.tt.engine.font.Font;
import com.oddlabs.tt.engine.render.ModeIconQuads;

/** Configuration metrics, fill icons, and font for progress bars. */
record ProgressBarData(Horizontal progressBar,
                       ModeIconQuads leftFill,
                       ModeIconQuads centerFill,
                       ModeIconQuads rightFill,
                       Font font) {
}
