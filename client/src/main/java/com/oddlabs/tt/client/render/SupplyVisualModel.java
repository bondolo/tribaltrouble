package com.oddlabs.tt.client.render;

import com.oddlabs.tt.base.geom.BoundsProvider;
import com.oddlabs.tt.engine.render.SpriteKey;
import com.oddlabs.tt.simulation.model.SupplyModel;
import com.oddlabs.util.Color;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Visual model interface for supply objects managing spawn animations, transforms, and rendering effects.
 */
@NullMarked
public interface SupplyVisualModel extends VisualModel {
    /**
     * {@return the graphics sprite key for rendering}
     */
    SpriteKey getSpriteKey();

    /**
     * Properties defining the drop shadow for a supply model.
     *
     * @param diameter the diameter of the shadow
     * @param opacity the opacity of the shadow (0.0 to 1.0)
     * @param verticalCenter the vertical center offset of the shadow
     */
    record ShadowProperties(float diameter, float opacity, float verticalCenter) {
    }

    /**
     * Properties defining the ground crack decal for a spawning supply model.
     *
     * @param color the linear tint color of the crack decal, or null if invisible
     * @param opacity the opacity of the crack decal (0.0 to 1.0)
     * @param diameter the diameter of the crack decal
     * @param pattern the texture pattern index/frame of the crack decal
     */
    record DecalProperties(Color.@Nullable Linear color, float opacity, float diameter, float pattern) {
    }

    @Override
    SupplyModel getModel();

    /**
     * {@return the visual vertical offset along the Z-axis in world units}
     */
    float getOffsetZ();

    /**
     * {@return the rotation angle in radians around the Z-axis}
     */
    float getRotation();

    /**
     * {@return the linear color tint during spawn, or null for default lighting}
     */
    Color.@Nullable Linear getSpawnColorTint();

    /**
     * {@return the shadow properties for rendering}
     */
    ShadowProperties getShadowProperties();

    /**
     * {@return the crack decal properties for rendering}
     */
    DecalProperties getDecalProperties();

    /**
     * {@return the bounds provider for LOD and collision/picking calculations}
     */
    BoundsProvider getBoundsProvider();

    /**
     * Updates the spawn progress ratio.
     *
     * @param progress the spawn progress ratio between 0.0 (started) and 1.0 (completed)
     */
    void setSpawnProgress(float progress);

    /**
     * Marks the spawn animation as complete.
     */
    void completeSpawn();

    /**
     * {@return true if the supply is actively undergoing a spawn animation}
     */
    boolean isSpawning();

    /**
     * {@return the current spawn progress ratio between 0.0 and 1.0}
     */
    float getSpawnProgress();

    /**
     * {@return the total duration in seconds of the spawn animation}
     */
    float getSpawnDuration();
}
