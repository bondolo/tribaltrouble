package com.oddlabs.tt.gui;

/** Interface for components capable of supplying tooltip information. */
public interface ToolTip {
    void appendToolTip(ToolTipBox tool_tip);

    default boolean hasToolTip() {
        return true;
    }
}
