package com.oddlabs.tt.client.gui;

import com.oddlabs.tt.engine.render.IconQuad;

/**
 * Icon and geometry metrics for notification arrows pointing to world coordinates.
 */
record NotifyArrowData(IconQuad arrow,
                       int headX,
                       int headY,
                       int endX,
                       int endY) {
}
