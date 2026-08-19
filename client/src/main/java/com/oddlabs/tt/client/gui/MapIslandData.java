package com.oddlabs.tt.client.gui;

import com.oddlabs.tt.engine.render.IconQuad;
import com.oddlabs.tt.engine.render.ModeIconQuads;
import org.jspecify.annotations.NonNull;

public record MapIslandData(@NonNull ModeIconQuads button,
                            int x,
                            int y,
                            @NonNull IconQuad flag,
                            @NonNull IconQuad boat,
                            int pinX,
                            int pinY) {

}
