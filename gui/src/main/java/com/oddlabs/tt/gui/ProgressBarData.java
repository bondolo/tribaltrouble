package com.oddlabs.tt.gui;

import com.oddlabs.tt.engine.font.Font;
import com.oddlabs.tt.engine.render.ModeIconQuads;

public record ProgressBarData(Horizontal progressBar,
                              ModeIconQuads leftFill,
                              ModeIconQuads centerFill,
                              ModeIconQuads rightFill,
                              Font font) {

}
