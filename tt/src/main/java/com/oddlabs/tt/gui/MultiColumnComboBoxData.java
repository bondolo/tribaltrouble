package com.oddlabs.tt.gui;

import com.oddlabs.tt.font.Font;
import com.oddlabs.util.Color;
import org.jspecify.annotations.NonNull;

public record MultiColumnComboBoxData(@NonNull Box box,
                                      @NonNull Horizontal buttonPressed,
                                      @NonNull Horizontal buttonUnpressed,
                                      @NonNull ModeIconQuads descending,
                                      @NonNull ModeIconQuads ascending,
                                      @NonNull Color color1,
                                      @NonNull Color color2,
                                      @NonNull Color colorMarked,
                                      @NonNull Font font,
                                      int captionOffset) {
}
