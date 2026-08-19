package com.oddlabs.tt.gui;

import com.oddlabs.tt.engine.font.Font;
import com.oddlabs.tt.engine.render.ModeIconQuads;
import org.jspecify.annotations.NonNull;

public record PulldownData(@NonNull Horizontal pulldownTop,
                           @NonNull Horizontal pulldownBottom,
                           @NonNull Box pulldownItem,
                           @NonNull Horizontal pulldownButton,
                           @NonNull ModeIconQuads arrow,
                           int arrowOffsetRight,
                           int textOffsetLeft,
                           @NonNull Font font) {

}
