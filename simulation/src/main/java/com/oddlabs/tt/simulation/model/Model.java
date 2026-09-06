package com.oddlabs.tt.simulation.model;

import com.oddlabs.tt.base.geom.BoundingBox;
import com.oddlabs.tt.simulation.landscape.World;
import org.jspecify.annotations.Nullable;


/**
 * Represents a world entity with visual representation and world association.
 */
public abstract class Model extends Element<Model> implements Shadowable {
    private final World world;
    /** ground height if {@link #groundBased} */
    private float baseZ;
    /** if true then the Model is positioned relative to the terrain at (x, y) */
    private boolean groundBased = true;

    protected Model(World world) {
        super(world.getElementRoot());
        this.world = world;
    }

    @Override
    protected Model self() {
        return this;
    }

    @Override
    public boolean isDead() {
        return !isRegistered();
    }

    @Override
    public float getShadowDiameter() {
        // no shadow
        return 0f;
    }

    /** {@return rendering position offset to the base z of the model */
    public float getOffsetZ() {
        return 0f;
    }


    public int getAnimation() {
        return 0;
    }

    public float getAnimationTicks() {
        return 0f;
    }

    public float getNoDetailSize() {
        return 0f;
    }

    /** {@return the bounds of the model in the local coordinate system for each animation} */
    protected abstract BoundingBox @Nullable [] getLocalBounds();

    protected void updateBounds() {
        var modelBounds = getLocalBounds();
        if (modelBounds != null) {
            BoundingBox unit_bounds = modelBounds[getAnimation()];
            float x = getPositionX();
            float y = getPositionY();
            float z = getPositionZ();
            float error = getZError();
            setBounds(unit_bounds.bmin_x + x, unit_bounds.bmax_x + x, unit_bounds.bmin_y + y, unit_bounds.bmax_y + y,
                    unit_bounds.bmin_z + z - error, unit_bounds.bmax_z + z + error);
        }
    }


    protected float getZError() {
        return 0f;
    }

    protected final float getLandscapeError() {
        return world.getHeightMap().getLeafFromCoordinates(getPositionX(), getPositionY()).getMaxError();
    }

    public final World getWorld() {
        return world;
    }

    @Override
    public final void setPosition(float x, float y) {
        super.setPosition(x, y);
        groundBased = true;
        reinsert();
    }

    public final void setPosition(float x, float y, float z) {
        super.setPosition(x, y);
        baseZ = z;
        groundBased = false;
        updateModelInternal();
    }

    @Override
    public void register() {
        if (groundBased) {
            baseZ = world.getHeightMap().getNearestHeight(getPositionX(), getPositionY());
        }
        setPositionZ(baseZ + getOffsetZ());
        updateBounds();
        onReinsert();
        super.register();
    }

    /**
     * update positions related to model position
     */
    protected void onReinsert() {
        // No-op by default
    }

    public final void reinsert() {
        if (isRegistered()) {
            if (groundBased) {
                baseZ = world.getHeightMap().getNearestHeight(getPositionX(), getPositionY());
            }
            updateModelInternal();
        }
    }

    private void updateModelInternal() {
        setPositionZ(baseZ + getOffsetZ());
        updateBounds();
        onReinsert();
        if (isRegistered()) {
            reregister();
        }
    }
}
