package com.oddlabs.tt.model;

import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.render.SpriteKey;
import org.joml.Matrix4f;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * An accessory representing resources (wood, rock, iron, rubber) currently being carried by a unit.
 */
public final class CarriedResourceAccessory implements StaticAccessory {
    private final @NonNull Unit unit;

    public CarriedResourceAccessory(@NonNull Unit unit) {
        this.unit = unit;
    }

    @Override
    public @Nullable SpriteKey getSpriteRenderer() {
        UnitSupplyContainer supply_container = unit.getSupplyContainer();
        if (supply_container != null) {
            Class<? extends Supply> type = supply_container.getSupplyType();
            if (type != null) {
                return supply_container.getSupplySpriteRenderer(type);
            }
        }
        return null;
    }

    @Override
    public boolean isVisible(@NonNull AccessorizableModel parent, @NonNull CameraState camera) {
        UnitSupplyContainer supply_container = unit.getSupplyContainer();
        return unit.getAbilities().hasAbilities(Abilities.BUILD) &&
                supply_container != null &&
                supply_container.getSupplyType() != null &&
                supply_container.getNumSupplies() > 0;
    }

    @Override
    public void getRelativeTransform(@NonNull Matrix4f dest, @NonNull AccessorizableModel parent) {
        // Carried resources currently use the parent's exact transform in the old code.
        // We can add offsets here later if needed.
    }

    @Override
    public int getAnimation() {
        return unit.getAnimation();
    }
}
