package com.oddlabs.tt.gui;

import com.oddlabs.tt.engine.font.Font;
import com.oddlabs.tt.engine.render.ModeIconQuads;
import com.oddlabs.util.Color;

public record MultiColumnComboBoxData(Box box,
                                      Horizontal buttonPressed,
                                      Horizontal buttonUnpressed,
                                      ModeIconQuads descending,
                                      ModeIconQuads ascending,
                                      Color.Linear color1,
                                      Color.Linear color2,
                                      Color.Linear colorMarked,
                                      Font font,
                                      int captionOffset) {
}
