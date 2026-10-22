package com.oddlabs.tt.client.render;

import com.oddlabs.tt.client.resource.AssetRegistry;
import com.oddlabs.tt.engine.render.CameraState;
import com.oddlabs.tt.engine.render.ModelState;
import com.oddlabs.tt.engine.render.SpriteKey;
import com.oddlabs.tt.engine.render.SpriteList;
import com.oddlabs.tt.engine.render.StaticAccessory;
import com.oddlabs.tt.simulation.behaviour.AttackBehaviour;
import com.oddlabs.tt.simulation.model.Model;
import com.oddlabs.tt.simulation.model.Race;
import com.oddlabs.tt.simulation.model.Selectable;
import com.oddlabs.tt.simulation.model.SupplyType;
import com.oddlabs.tt.simulation.model.Unit;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Socket-attached weapon accessory for units holding throwing weapons.
 */
final class WeaponAccessory implements StaticAccessory {
    private static final String SOCKET_PROP1 = "Prop1";
    private static final String SOCKET_HAND = "R Hand";

    private static final Matrix4fc VIKING_AXE_LOCAL = new Matrix4f().set(
            -0.000430f, -0.969786f, 0.000022f, 0.000000f,
            0.897271f, 0.025162f, 0.000004f, 0.000000f,
            -0.003478f, 0.067121f, 0.999997f, 0.000000f,
            0.029143f, -0.393509f, 0.923507f, 1.000000f
    );

    private static final Matrix4fc VIKING_AXE_HAND_LOCAL = new Matrix4f().set(
            0.965589f, 0.089989f, -0.005100f, 0.000000f,
            -0.106651f, 0.877709f, 0.154857f, 0.000000f,
            -0.045330f, -0.180879f, 0.984754f, 0.000000f,
            0.598164f, -0.031971f, 0.672150f, 1.000000f
    );

    private static final Matrix4fc NATIVE_SPEAR_LOCAL = new Matrix4f().set(
            -0.019580f, 0.010967f, 0.999797f, 0.000000f,
            0.038799f, 0.997411f, -0.000292f, 0.000000f,
            -0.999036f, 0.038902f, -0.019998f, 0.000000f,
            -0.010216f, 0.027513f, 2.050380f, 1.000000f
    );

    private static final float NATIVE_SPEAR_GRIP_OFFSET_X = 1.04f;
    private static final float NATIVE_SPEAR_LATERAL_OFFSET_Z = -0.137f;
    private static final Vector3fc PITCH_AXIS = new Vector3f(-0.0348f, -0.2590f, -0.9652f);
    private static final float NATIVE_SPEAR_BASE_PITCH = (float) Math.toRadians(6.7f);
    private static final float NATIVE_SPEAR_PRE_RELEASE_START = 0.41f;
    private static final float DEFAULT_SPEAR_LAUNCH_PITCH = (float) Math.toRadians(22.0f);
    private static final float SPEAR_SPAWN_OFFSET_X = 1.25f;
    private static final float SPEAR_SPAWN_OFFSET_Y = -0.35f;
    private static final float SPEAR_SPAWN_OFFSET_Z = 1.90f;
    private static final float SPEAR_EFFECTIVE_GRAVITY = 3.0f * 9.82f * 1.05f * 1.05f;

    private final @NonNull Unit unit;
    private final @NonNull UnitVisualModel visualModel;
    private final boolean isViking;
    private final Matrix4f socketTransform = new Matrix4f();
    private final Matrix4f adjustedTransform = new Matrix4f();
    private final @NonNull Matrix4fc localTransform;
    private final @Nullable Matrix4fc handLocalTransform;

    private int socketBoneIndex = -1;
    private int handBoneIndex = -1;

    WeaponAccessory(@NonNull Unit unit, @NonNull UnitVisualModel visualModel) {
        this.unit = unit;
        this.visualModel = visualModel;
        Race race = unit.getOwner().getPlayerInfo().race();
        this.isViking = (race == Race.VIKINGS);
        this.localTransform = isViking ? VIKING_AXE_LOCAL : NATIVE_SPEAR_LOCAL;
        this.handLocalTransform = isViking ? VIKING_AXE_HAND_LOCAL : null;
    }

    private @Nullable SupplyType getWeaponMaterial() {
        return switch (unit.getTemplate().getUnitType()) {
            case WARRIOR_ROCK -> SupplyType.ROCK;
            case WARRIOR_IRON -> SupplyType.IRON;
            case WARRIOR_RUBBER -> SupplyType.RUBBER;
            default -> null;
        };
    }

    @Override
    public @Nullable SpriteKey getSpriteRenderer() {
        SupplyType visualType = getWeaponMaterial();
        if (visualType != null) {
            Race race = unit.getOwner().getPlayerInfo().race();
            return AssetRegistry.getInstance().getWeaponSprite(race, visualType);
        }
        return null;
    }

    @Override
    public boolean isVisible(@NonNull Model parent, @NonNull CameraState camera) {
        if (unit.isDead()) {
            return false;
        }
        if (unit.getCurrentBehaviour() instanceof AttackBehaviour attackBehaviour) {
            return !attackBehaviour.isReleased();
        }
        if (unit.getAnimation() == Unit.Animation.THROWING.ordinal()) {
            return unit.getAnimationTicks() < unit.getWeaponFactory().getReleaseRatio();
        }
        return true;
    }

    private float getSpearMetersPerSecond() {
        SupplyType type = getWeaponMaterial();
        if (type != null) {
            return switch (type) {
                case ROCK -> 20f;
                case IRON -> 25f;
                case RUBBER -> 30f;
                default -> 20f;
            };
        }
        return 20f;
    }

    private float computeSpearTargetPitch() {
        if (unit.getCurrentBehaviour() instanceof AttackBehaviour attackBehaviour) {
            Selectable<?> target = attackBehaviour.getTarget();
            float startX = unit.getPositionX() + SPEAR_SPAWN_OFFSET_X * unit.getDirectionX()
                    - SPEAR_SPAWN_OFFSET_Y * unit.getDirectionY();
            float startY = unit.getPositionY() + SPEAR_SPAWN_OFFSET_X * unit.getDirectionY()
                    + SPEAR_SPAWN_OFFSET_Y * unit.getDirectionX();
            float dx = target.getPositionX() - startX;
            float dy = target.getPositionY() - startY;
            float len = (float) Math.hypot(dx, dy);
            float mps = getSpearMetersPerSecond();
            float totalTime = mps > 0f ? len / mps : 0f;
            float destZ = target.getPositionZ() + target.getHitOffsetZ();
            float startZ = unit.getPositionZ() + SPEAR_SPAWN_OFFSET_Z;
            float baselineVz = totalTime > 0f ? (destZ - startZ) / totalTime : 0f;
            float loftVz = 0.5f * SPEAR_EFFECTIVE_GRAVITY * totalTime;
            return (float) Math.atan2(baselineVz + loftVz, mps);
        }
        return DEFAULT_SPEAR_LAUNCH_PITCH;
    }

    @Override
    public void getRelativeTransform(@NonNull Matrix4f dest, @NonNull ModelState<?> parentState) {
        SpriteList spriteList = parentState.getSpriteList();
        if (spriteList == null) {
            return;
        }

        boolean isThrowing = parentState.getAnimation() == Unit.Animation.THROWING.ordinal();
        if (isViking && isThrowing && handLocalTransform != null) {
            if (handBoneIndex < 0) {
                handBoneIndex = spriteList.getSocketIndex(SOCKET_HAND);
            }
            if (handBoneIndex >= 0) {
                Matrix4fc[] bones = visualModel.getEvaluatedBones(spriteList, parentState.getAnimation(),
                        parentState.getAnimationTicks());
                if (bones != null && spriteList.getSocketTransform(handBoneIndex, bones, socketTransform)) {
                    dest.mul(socketTransform).mul(handLocalTransform);
                    return;
                }
            }
        }

        if (socketBoneIndex < 0) {
            socketBoneIndex = spriteList.getSocketIndex(SOCKET_PROP1);
        }
        if (socketBoneIndex >= 0) {
            Matrix4fc[] bones = visualModel.getEvaluatedBones(spriteList, parentState.getAnimation(),
                    parentState.getAnimationTicks());
            if (bones != null && spriteList.getSocketTransform(socketBoneIndex, bones, socketTransform)) {
                if (!isViking && isThrowing) {
                    float ticks = parentState.getAnimationTicks();
                    float releaseRatio = unit.getWeaponFactory().getReleaseRatio();
                    if (ticks >= NATIVE_SPEAR_PRE_RELEASE_START && releaseRatio > NATIVE_SPEAR_PRE_RELEASE_START) {
                        float progress = Math.clamp((ticks - NATIVE_SPEAR_PRE_RELEASE_START) / (releaseRatio
                                - NATIVE_SPEAR_PRE_RELEASE_START), 0f, 1f);
                        float targetPitch = computeSpearTargetPitch();
                        float pitchDiff = targetPitch - NATIVE_SPEAR_BASE_PITCH;
                        float pitchAdjustment = progress * pitchDiff;
                        adjustedTransform.set(localTransform)
                                .translate(-NATIVE_SPEAR_GRIP_OFFSET_X, 0f, 0f)
                                .rotate(-pitchAdjustment, PITCH_AXIS)
                                .translate(NATIVE_SPEAR_GRIP_OFFSET_X, 0f, progress * NATIVE_SPEAR_LATERAL_OFFSET_Z);
                        dest.mul(socketTransform).mul(adjustedTransform);
                        return;
                    }
                }
                dest.mul(socketTransform).mul(localTransform);
            }
        }
    }
}
