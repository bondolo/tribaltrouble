package com.oddlabs.tt.gui;

import com.oddlabs.tt.engine.font.Font;
import com.oddlabs.tt.engine.render.ModeIconQuads;

public record PulldownData(Horizontal pulldownTop,
                           Horizontal pulldownBottom,
                           Box pulldownItem,
                           Horizontal pulldownButton,
                           ModeIconQuads arrow,
                           int arrowOffsetRight,
                           int textOffsetLeft,
                           Font font) {

}
