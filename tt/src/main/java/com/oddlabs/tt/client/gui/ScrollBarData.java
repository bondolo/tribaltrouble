package com.oddlabs.tt.client.gui;

import org.jspecify.annotations.NonNull;

record ScrollBarData(@NonNull Vertical scrollBar,
                     @NonNull ModeIconQuads scrollDownButtonPressed,
                     @NonNull ModeIconQuads scrollDownButtonUnpressed,
                     @NonNull ModeIconQuads scrollDownArrow,
                     @NonNull ModeIconQuads scrollUpButtonPressed,
                     @NonNull ModeIconQuads scrollUpButtonUnpressed,
                     @NonNull ModeIconQuads scrollUpArrow,
                     @NonNull Vertical scrollButton,
                     int leftOffset,
                     int bottomOffset,
                     int topOffset) {

}
