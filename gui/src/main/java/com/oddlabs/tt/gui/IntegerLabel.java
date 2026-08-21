package com.oddlabs.tt.gui;

import com.oddlabs.tt.engine.font.Font;

public final class IntegerLabel extends Label {
    private final int val;

    public IntegerLabel(int val, Font font, int width) {
        super(Integer.toString(val), font, width, Origin.AT_END);
        this.val = val;
    }

    public IntegerLabel(int val, Font font) {
        super(Integer.toString(val), font);
        this.val = val;
    }

    @Override
    public int compareTo(Label o) {
        return o instanceof IntegerLabel other ? val - other.val : -1;
    }
}
