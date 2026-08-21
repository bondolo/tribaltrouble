package com.oddlabs.tt.gui;

import com.oddlabs.tt.engine.font.Font;
import com.oddlabs.tt.engine.render.ModeIconQuads;

public record FormData(Box form,
                       Box slimForm,
                       ModeIconQuads formClose,
                       int objectSpacing,
                       int sectionSpacing,
                       int captionLeft,
                       int captionY,
                       int closeRight,
                       int closeTop,
                       Font captionFont) {
}
