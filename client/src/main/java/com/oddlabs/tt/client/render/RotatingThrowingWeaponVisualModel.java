package com.oddlabs.tt.client.render;

import com.oddlabs.tt.audio.AudioImplementation;
import com.oddlabs.tt.client.resource.AudioRegistry;
import com.oddlabs.tt.simulation.model.weapon.RotatingThrowingWeapon;
import org.joml.Matrix4f;

/**
 * Visual model for rotating throwing weapons (axes) managing spin animation and grip pivot offsets.
 */
final class RotatingThrowingWeaponVisualModel extends ThrowingWeaponVisualModel<RotatingThrowingWeapon> {
    private static final float AXE_LOFT_FACTOR = 1.01f;
    private static final float INITIAL_AXE_ROTATION = (float) Math.toRadians(29.2);
    private static final float AXE_GRIP_OFFSET_X = 0.40f;
    private static final float AXE_GRIP_OFFSET_Z = 0.70f;

    private final float angleVelocity;

    RotatingThrowingWeaponVisualModel(RotatingThrowingWeapon weapon, AudioImplementation audio) {
        super(weapon, audio, AXE_LOFT_FACTOR, AudioRegistry.SFX_WEAPON_AXE);
        float rotsPerSec = switch (weapon.getWeaponTier()) {
            case ROCK -> 3f;
            case IRON -> 6f;
            case RUBBER -> 9f;
        };
        this.angleVelocity = (float) (rotsPerSec * 2.0 * Math.PI);
    }

    @Override
    public void getTransform(Matrix4f dest) {
        float t = weapon.getTime();
        float visualZ = computeVisualZ();
        float yawRad = (float) Math.atan2(weapon.getDirectionY(), weapon.getDirectionX());

        dest.translation(weapon.getPositionX(), weapon.getPositionY(), visualZ)
                .rotate(yawRad, 0f, 0f, 1f)
                .rotate(INITIAL_AXE_ROTATION + angleVelocity * t, 0f, 1f, 0f)
                .translate(AXE_GRIP_OFFSET_X, 0f, AXE_GRIP_OFFSET_Z);
    }
}
