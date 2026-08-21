package com.oddlabs.tt.client.gui;

import com.oddlabs.tt.engine.render.IconQuad;
import com.oddlabs.tt.engine.render.ModeIconQuads;

public record MapIslandData(ModeIconQuads button,
                            int x,
                            int y,
                            IconQuad flag,
                            IconQuad boat,
                            int pinX,
                            int pinY) {

}
