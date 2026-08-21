package com.oddlabs.tt.gui;

import com.oddlabs.tt.engine.render.ModeIconQuads;

public record ScrollBarData(Vertical scrollBar,
                            ModeIconQuads scrollDownButtonPressed,
                            ModeIconQuads scrollDownButtonUnpressed,
                            ModeIconQuads scrollDownArrow,
                            ModeIconQuads scrollUpButtonPressed,
                            ModeIconQuads scrollUpButtonUnpressed,
                            ModeIconQuads scrollUpArrow,
                            Vertical scrollButton,
                            int leftOffset,
                            int bottomOffset,
                            int topOffset) {

}
