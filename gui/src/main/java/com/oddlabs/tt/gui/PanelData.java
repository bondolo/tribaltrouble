package com.oddlabs.tt.gui;

/** Configuration metrics, tab headers, and background box for panels. */
public record PanelData(Box box,
                        Horizontal tab,
                        int leftCaptionOffset,
                        int rightCaptionOffset,
                        int bottomCaptionOffset,
                        int leftTabOffset,
                        int bottomTabOffset) {

}
