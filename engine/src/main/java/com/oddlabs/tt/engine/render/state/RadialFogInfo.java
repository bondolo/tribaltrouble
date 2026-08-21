package com.oddlabs.tt.engine.render.state;

import com.oddlabs.util.Color;

/**
 * Screen-space radial fog configuration for map and tactical views.
 */
public final class RadialFogInfo extends FogInfo {
    private final float radiusScale;

    public RadialFogInfo(Color color, float density) {
        this(color, density, 1.0f);
    }

    public RadialFogInfo(Color color, float density, float radiusScale) {
        super(Mode.RADIAL, color, density);
        this.radiusScale = radiusScale;
    }

    public float getRadiusScale() {
        return radiusScale;
    }
}
