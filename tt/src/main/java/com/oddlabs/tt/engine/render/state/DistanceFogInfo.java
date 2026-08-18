package com.oddlabs.tt.engine.render.state;

import com.oddlabs.tt.simulation.model.Terrain;
import com.oddlabs.util.Color;
import org.jspecify.annotations.NonNull;

import java.util.EnumMap;
import java.util.Map;

/**
 * Distance and height-based exponential fog parameters for scene rendering.
 */
public final class DistanceFogInfo extends FogInfo {

    private static final Map<Terrain, @NonNull Color> FOG_COLOR = new EnumMap<>(Map.of(
            Terrain.NATIVE, new Color.Standard(0xFF_A5_BF_FF),
            Terrain.VIKING, new Color.Standard(0xFF_33_66_8C)
    ));
    private static final float NATIVE_FOG_DENSITY = 0.001f;
    private static final float VIKING_FOG_DENSITY = 0.0015f;
    private static final float NATIVE_FOG_HEIGHT = 1.2f;
    private static final float VIKING_FOG_HEIGHT = 1.4f;

    private final float start;
    private final float end;
    private final float height_factor;

    public DistanceFogInfo(FogInfo.@NonNull Mode mode, @NonNull Color color, float density, float height_factor,
            float start, float end) {
        super(mode, color, density);
        this.height_factor = height_factor;
        this.start = start;
        this.end = end;
    }

    /**
     * Creates distance fog configuration corresponding to the specified terrain type and world size.
     *
     * @param terrain the landscape terrain type
     * @param meters_per_world world size in meters
     * @return distance fog parameters
     */
    public static @NonNull DistanceFogInfo forTerrain(@NonNull Terrain terrain, int meters_per_world) {
        return switch (terrain) {
            case NATIVE ->
                new DistanceFogInfo(Mode.EXP2, FOG_COLOR.get(terrain), NATIVE_FOG_DENSITY, NATIVE_FOG_HEIGHT
                        * meters_per_world, 0f, meters_per_world >> 2);
            case VIKING ->
                new DistanceFogInfo(Mode.EXP2, FOG_COLOR.get(terrain), VIKING_FOG_DENSITY, VIKING_FOG_HEIGHT
                        * meters_per_world, 0f, meters_per_world >> 2);
        };
    }

    public float getStart() {
        return start;
    }

    public float getEnd() {
        return end;
    }

    public float getHeightFactor() {
        return height_factor;
    }
}
