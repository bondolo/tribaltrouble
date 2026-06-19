package com.oddlabs.tt.render;

import com.oddlabs.tt.animation.Animated;
import com.oddlabs.tt.animation.AnimationManager;
import com.oddlabs.tt.audio.AudioParameters;
import com.oddlabs.tt.audio.AudioPlayer;
import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.model.BoundingBox;
import com.oddlabs.tt.model.Race;
import com.oddlabs.tt.model.WeaponVisualType;
import com.oddlabs.tt.model.snapshot.EntitySnapshot;
import com.oddlabs.tt.resource.AudioAssets;
import com.oddlabs.util.Color;
import org.joml.Matrix4f;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Client-side animation and model state representing a flying projectile weapon.
 */
public final class VisualWeapon implements ModelState<EntitySnapshot>, Animated {
    private static final float GRAVITY_MULTIPLIER = 3.0f;
    private static final float GRAVITY = -GRAVITY_MULTIPLIER * 9.82f;

    private final float startX;
    private final float startY;
    private final float endX;
    private final float endY;
    private final float destZ;
    private final float timeLimit;
    private final @NonNull WeaponVisualType weaponType;
    private final @NonNull Race race;
    private final Color.@NonNull Linear teamColor;
    private final boolean rotating;
    private final @NonNull RenderQueues queues;
    private final @NonNull AnimationManager manager;
    private final @NonNull BoundingBox bounds = new BoundingBox();
    private final @NonNull World world;
    private final @Nullable AudioPlayer audioPlayer;

    private float x;
    private float y;
    private float z;
    private float zSpeed;
    private float time;
    private float angle;
    private float dirX;
    private float dirY;
    private float angleVelocity;
    private float eyeDistanceSquared;

    public VisualWeapon(
            float startX, float startY, float startZ,
            float endX, float endY, float destZ,
            float zSpeed, float timeLimit,
            @NonNull WeaponVisualType weaponType, @NonNull Race race,
            Color.@NonNull Linear teamColor, boolean rotating,
            @NonNull World world,
            @NonNull RenderQueues queues, @NonNull AnimationManager manager) {
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
        this.destZ = destZ;
        this.zSpeed = zSpeed;
        this.timeLimit = timeLimit;
        this.weaponType = weaponType;
        this.race = race;
        this.teamColor = teamColor;
        this.rotating = rotating;
        this.world = world;
        this.queues = queues;
        this.manager = manager;

        this.x = startX;
        this.y = startY;
        this.z = startZ;
        this.time = 0f;
        this.angle = 0f;

        float dx = endX - startX;
        float dy = endY - startY;
        float len = (float) Math.max(Math.hypot(dx, dy), 0.01);
        this.dirX = dx / len;
        this.dirY = dy / len;

        if (rotating) {
            this.angleVelocity = switch (weaponType) {
                case ROCK -> 3f * 360f;
                case IRON -> 6f * 360f;
                case RUBBER -> 9f * 360f;
                default -> 0f;
            };
        }

        var visuals = VisualRegistry.getInstance().getWeaponVisuals(race, weaponType);
        var params = new AudioParameters(visuals.throwSound(),
                AudioAssets.AUDIO_RANK_WEAPON_ATTACK, AudioAssets.AUDIO_DISTANCE_WEAPON_ATTACK,
                AudioAssets.AUDIO_GAIN_WEAPON_ATTACK, AudioAssets.AUDIO_RADIUS_WEAPON_ATTACK,
                ThreadLocalRandom.current().nextFloat(.9f, 1.1f));
        this.audioPlayer = world.getAudio().newAudio(x, y, z, params);

        bounds.setBounds(x - 0.5f, x + 0.5f, y - 0.5f, y + 0.5f, z - 0.5f, z + 0.5f);
        queues.addVisualWeapon(this);
        manager.registerAnimation(this);
    }

    public @NonNull WeaponVisualType getWeaponType() {
        return weaponType;
    }

    public @NonNull Race getRace() {
        return race;
    }

    public float getDirX() {
        return dirX;
    }

    public float getDirY() {
        return dirY;
    }

    public float getAngle() {
        return angle;
    }

    public boolean isRotating() {
        return rotating;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getZ() {
        return z;
    }

    @Override
    public void animate(float t) {
        time += t;
        float progress = Math.min(1f, time / timeLimit);

        x = startX + (endX - startX) * progress;
        y = startY + (endY - startY) * progress;
        z += zSpeed * t;
        zSpeed += GRAVITY * t;

        bounds.setBounds(x - 0.5f, x + 0.5f, y - 0.5f, y + 0.5f, z - 0.5f, z + 0.5f);

        if (audioPlayer != null) {
            audioPlayer.setPosition(x, y, z);
        }

        if (rotating) {
            angle += angleVelocity * t;
        } else {
            float dx = endX - startX;
            float dy = endY - startY;
            float horizDist = (float) Math.hypot(dx, dy);
            float horizSpeed = horizDist / timeLimit;
            angle = (float) Math.toDegrees(Math.atan2(zSpeed, horizSpeed));
        }

        if (time >= timeLimit) {
            queues.removeVisualWeapon(this);
            manager.removeAnimation(this);
            if (audioPlayer != null) {
                audioPlayer.stop();
            }
            var visuals = VisualRegistry.getInstance().getWeaponVisuals(race, weaponType);
            var hitParams = new com.oddlabs.tt.audio.AudioParameters(
                    visuals.hitSounds()[ThreadLocalRandom.current().nextInt(visuals
                            .hitSounds().length)],
                    AudioAssets.AUDIO_RANK_WEAPON_HIT, AudioAssets.AUDIO_DISTANCE_WEAPON_HIT,
                    AudioAssets.AUDIO_GAIN_WEAPON_HIT, AudioAssets.AUDIO_RADIUS_WEAPON_HIT,
                    ThreadLocalRandom.current().nextFloat(0.95f, 1.05f)
            );
            world.getAudio().newAudio(endX, endY, destZ, hitParams);
        }
    }

    public void updateEyeDistance(@NonNull CameraState camera) {
        eyeDistanceSquared = RenderTools.getEyeDistanceSquared(bounds, camera.getCurrentX(), camera.getCurrentY(),
                camera.getCurrentZ());
    }

    @Override
    public void markDetailPoint() {
        SpriteKey sprite = VisualRegistry.getInstance().getWeaponSprite(race, weaponType);
        queues.getRenderer(sprite).addToNoDetailList(this);
    }

    @Override
    public void markDetailPolygon(@NonNull PolyDetail level) {
        SpriteKey sprite = VisualRegistry.getInstance().getWeaponSprite(race, weaponType);
        queues.getRenderer(sprite).addToRenderList(level, this, false);
    }

    @Override
    public int getTriangleCount(@NonNull PolyDetail level) {
        SpriteKey sprite = VisualRegistry.getInstance().getWeaponSprite(race, weaponType);
        return queues.getRenderer(sprite).getTriangleCount(level);
    }

    @Override
    public float getEyeDistanceSquared() {
        return eyeDistanceSquared;
    }

    @Override
    public Color.@NonNull Linear getTeamColor() {
        return teamColor;
    }

    @Override
    public Color.@NonNull Linear getSelectionColor() {
        return Color.Linear.WHITE;
    }

    @Override
    public com.oddlabs.tt.model.Selectable.@NonNull VisualPattern getPattern() {
        return com.oddlabs.tt.model.Selectable.VisualPattern.FRIENDLY;
    }

    @Override
    public Color.@NonNull Linear getColor() {
        return Color.Linear.WHITE;
    }

    @Override
    public @Nullable EntitySnapshot getEntity() {
        return null;
    }

    @Override
    public float getNoDetailSize() {
        return 0.5f;
    }

    @Override
    public com.oddlabs.tt.model.@Nullable Target getTarget() {
        return null;
    }

    @Override
    public @NonNull Matrix4f getTransform(@NonNull Matrix4f dest) {
        float yawRad = (float) Math.atan2(dirY, dirX);
        if (rotating) {
            float spinRad = (float) Math.toRadians(angle);
            dest.translation(x, y, z)
                    .rotate(yawRad, 0f, 0f, 1f)
                    .rotate(spinRad, 0f, 1f, 0f);
        } else {
            float pitchRad = (float) Math.toRadians(angle);
            dest.translation(x, y, z)
                    .rotate(yawRad, 0f, 0f, 1f)
                    .rotate(-pitchRad, 0f, 1f, 0f);
        }
        return dest;
    }

    @Override
    public int getAnimation() {
        return 0;
    }

    @Override
    public float getAnimationTicks() {
        return 0f;
    }

    public @NonNull BoundingBox getBounds() {
        return bounds;
    }
}
