package com.oddlabs.tt.model;

import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.render.Shadowable;
import com.oddlabs.tt.render.SpriteKey;
import com.oddlabs.tt.util.BoundingBox;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Represents a world entity with visual representation and world association.
 */
public abstract class Model extends Element<Model> implements Shadowable {
    private final @NonNull World world;
    private @Nullable ClientState clientState;

    protected Model(@NonNull World world) {
        super(Objects.requireNonNull(world, "world").getElementRoot());
        this.world = world;
    }

    @Override
    protected @NonNull Model self() {
        return this;
    }

    @Override
    public float getShadowDiameter() {
        // no shadow
        return 0f;
    }


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

    public abstract @Nullable SpriteKey getSpriteRenderer();

    /** {@return the bounds of the model in the local coordinate system for each animation} */
    protected abstract @NonNull BoundingBox @Nullable [] getLocalBounds();

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

    public final @NonNull World getWorld() {
        return world;
    }

    @Override
    public final void setPosition(float x, float y) {
        super.setPosition(x, y);
        reinsert();
    }

    /**
     * update positions related to model position
     */
    protected void onReinsert() {
        // No-op by default
    }

    public final void reinsert() {
        if (isRegistered()) {
            setPositionZ(world.getHeightMap().getNearestHeight(getPositionX(), getPositionY()) + getOffsetZ());
            updateBounds();
            onReinsert();
            reregister();
        }
    }

    /** {@return the client state of the specified class type, or null if not set or of a different type} */
    public final <C extends ClientState> @Nullable C getClientState(@NonNull Class<? extends C> type) {
        return type.isInstance(clientState) ? type.cast(clientState) : null;
    }

    /**
     * Sets the client state associated with this model.
     *
     * @param clientState The new client state.
     */
    public final void setClientState(@Nullable ClientState clientState) {
        this.clientState = clientState;
    }

    protected final void animateClientState(float t) {
        if (clientState != null) {
            clientState.update(t);
        }
    }
}
