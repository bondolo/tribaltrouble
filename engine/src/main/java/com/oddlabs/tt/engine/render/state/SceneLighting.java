package com.oddlabs.tt.engine.render.state;

import com.oddlabs.util.Color;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * Directional and ambient lighting parameters for 3D scene rendering.
 *
 * @param lightDirection normalized vector pointing toward the sun
 * @param ambient isotropic ambient illumination color
 * @param sun direct solar illumination color
 */
public record SceneLighting(
        Vector3fc lightDirection,
        Color.Linear ambient,
        Color.Linear sun) {

    public static final float DEFAULT_SUN_ELEVATION_DEGREES = 70.0f;
    public static final float DEFAULT_SUN_AZIMUTH_DEGREES = 225.0f;

    public static final SceneLighting CANONICAL = create(
            DEFAULT_SUN_ELEVATION_DEGREES,
            DEFAULT_SUN_AZIMUTH_DEGREES,
            new Color.Standard(0.65f, 1.0f).linear(),
            new Color.Standard(1.0f, 1.0f).linear());

    public static SceneLighting create(float elevationDegrees, float azimuthDegrees, Color.Linear ambient, Color.Linear sun) {
        float radElev = (float) Math.toRadians(elevationDegrees);
        float radAzim = (float) Math.toRadians(azimuthDegrees);
        float x = (float) (Math.cos(radElev) * Math.cos(radAzim));
        float y = (float) (Math.cos(radElev) * Math.sin(radAzim));
        float z = (float) Math.sin(radElev);
        return new SceneLighting(new Vector3f(x, y, z), ambient, sun);
    }
}
