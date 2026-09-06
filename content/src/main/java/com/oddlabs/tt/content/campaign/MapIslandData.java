package com.oddlabs.tt.content.campaign;

import com.oddlabs.tt.engine.render.IconQuad;
import com.oddlabs.tt.engine.render.ModeIconQuads;

/**
 * Visual layout and icon data for an island on the campaign map.
 */
record MapIslandData(ModeIconQuads button,
                     int x,
                     int y,
                     IconQuad flag,
                     IconQuad boat,
                     int pinX,
                     int pinY) {

}
