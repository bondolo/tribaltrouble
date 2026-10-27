package com.oddlabs.tt.client.render;

import com.oddlabs.tt.audio.AudioImplementation;
import com.oddlabs.tt.client.resource.AudioRegistry;
import com.oddlabs.tt.simulation.model.weapon.DirectedThrowingWeapon;
import org.joml.Matrix4f;

/**
 * Visual model for directed throwing weapons (spears) tracking pitch along the ballistic arc.
 */
final class DirectedThrowingWeaponVisualModel extends ThrowingWeaponVisualModel<DirectedThrowingWeapon> {
    private static final float SPEAR_LOFT_FACTOR = 1.05f;
    private static final float SPEAR_ROLL = (float) Math.PI;

    DirectedThrowingWeaponVisualModel(DirectedThrowingWeapon weapon, AudioImplementation audio) {
        super(weapon, audio, SPEAR_LOFT_FACTOR, AudioRegistry.SFX_WEAPON_SPEAR);
    }

    @Override
    public void getTransform(Matrix4f dest) {
        float t = weapon.getTime();
        float totalTime = weapon.getTimeLimit();
        float visualZ;
        float pitchRad;

        float baselineVz = totalTime > 0f ? (weapon.getDestZ() - weapon.getStartZ()) / totalTime : 0f;
        if (totalTime > 0f && t < totalTime && effectiveGravity > 0f) {
            visualZ = weapon.getPositionZ() + 0.5f * effectiveGravity * t * (totalTime - t);
            float loftVz = 0.5f * effectiveGravity * (totalTime - 2f * t);
            pitchRad = (float) Math.atan2(baselineVz + loftVz, weapon.getMetersPerSecond());
        } else {
            visualZ = weapon.getPositionZ();
            pitchRad = (float) Math.atan2(baselineVz, weapon.getMetersPerSecond());
        }

        float yawRad = (float) Math.atan2(weapon.getDirectionY(), weapon.getDirectionX());
        dest.translation(weapon.getPositionX(), weapon.getPositionY(), visualZ)
                .rotate(yawRad, 0f, 0f, 1f)
                .rotate(-pitchRad, 0f, 1f, 0f)
                .rotate(SPEAR_ROLL, 1f, 0f, 0f);
    }
}
