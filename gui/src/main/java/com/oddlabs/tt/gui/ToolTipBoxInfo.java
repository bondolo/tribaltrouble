package com.oddlabs.tt.gui;

/** Configuration metrics and layout offsets for tooltip boxes. */
record ToolTipBoxInfo(Horizontal box,
                      int leftOffset,
                      int bottomOffset,
                      int rightOffset,
                      int topOffset) {
}
