package com.oddlabs.tt.client.gui;

import com.oddlabs.tt.engine.font.Font;
import org.jspecify.annotations.NonNull;

public final class SortedLabel extends Label {
    private final int index;

    public SortedLabel(@NonNull String text, int index, @NonNull Font font) {
        super(text, font);
        this.index = index;
    }

    @Override
    public int compareTo(@NonNull Label o) {
        if (o instanceof SortedLabel other) {
            return index - other.index;
        } else
            return -1;
    }
}
