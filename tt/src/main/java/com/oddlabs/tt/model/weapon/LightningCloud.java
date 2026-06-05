package com.oddlabs.tt.model.weapon;

import com.oddlabs.tt.audio.AudioParameters;
import com.oddlabs.tt.audio.AudioPlayer;
import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.model.PointEmitterModel;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.particle.CloudFunction;
import com.oddlabs.tt.particle.Emitter;
import com.oddlabs.tt.particle.Lightning;
import com.oddlabs.tt.particle.ParametricEmitter;
import com.oddlabs.tt.pathfinder.UnitGrid;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.resource.AudioAssets;
import com.oddlabs.tt.util.Target;
import com.oddlabs.util.Color;
import org.joml.Vector3f;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;

/**
 * Logic controller for the Lightning Cloud magic effect.
 */
public final class LightningCloud extends PointEmitterModel implements Magic {
    private static final int NUM_STRIKES = 6;
    private static final float SECONDS_BETWEEN_STRIKES = .125f;
    private static final float BRIGHTNESS = Color.toLinear(.2f);
    private static final Color.LinearDelta BRIGHTNESS_DELTA = new Color.LinearDelta(BRIGHTNESS, BRIGHTNESS, BRIGHTNESS,
            0);
    private static final float LIGHTNING_TIME = .1f;
    private static final Color.LinearDelta DELTA_COLOR = new Color.LinearDelta(0f, 0f, 0f, -1f / LIGHTNING_TIME);

    private final @NonNull Player owner;
    private final float seconds_per_hit;
    private final float meters_per_second;
    private final float hit_chance;
    private final int damage;
    private final float height;
    private final @NonNull AudioPlayer bubbling_sound;
    private AudioPlayer cloud_sound;

    private float seconds_to_live;
    private @Nullable Selectable<?> target = null;
    private @Nullable Selectable<?> prev_target = null;
    private float hit_timer = 0f;
    private int strike_counter = 0;
    private float lightning_timer = 0f;
    private boolean lighted = false;
    private boolean first_run = true;

    public LightningCloud(@NonNull World world, float offset_x, float offset_y, float offset_z, float seconds_to_live,
            float seconds_per_hit, float seconds_to_init, float meters_per_second, float hit_chance, int damage,
            float height, @NonNull Unit src) {
        super(world, createEmitter(world, offset_x, offset_y, offset_z, seconds_to_live, seconds_to_init, height, src));
        this.seconds_to_live = seconds_to_live;

        this.seconds_per_hit = seconds_per_hit;
        this.meters_per_second = meters_per_second;
        this.hit_chance = hit_chance;
        this.damage = damage;
        this.height = height;
        owner = src.getOwner();

        float start_x = src.getPositionX() + offset_x * src.getDirectionX() - offset_y * (-src.getDirectionY());
        float start_y = src.getPositionY() + offset_x * src.getDirectionY() + offset_y * src.getDirectionX();
        float start_z = world.getHeightMap().getNearestHeight(start_x, start_y) + height;
        setPosition(start_x, start_y);
        setPositionZ(start_z);

        bubbling_sound = world.getAudio().newAudio(getPositionX(), getPositionY(), world.getHeightMap()
                .getNearestHeight(getPositionX(), getPositionY()), AudioAssets.BUBBLING);
    }

    private static Emitter<?> createEmitter(@NonNull World world, float offset_x, float offset_y, float offset_z,
            float seconds_to_live, float seconds_to_init, float height, @NonNull Unit src) {
        float start_x = src.getPositionX() + offset_x * src.getDirectionX() - offset_y * (-src.getDirectionY());
        float start_y = src.getPositionY() + offset_x * src.getDirectionY() + offset_y * src.getDirectionX();
        Vector3f pos = new Vector3f(start_x, start_y, world.getHeightMap().getNearestHeight(start_x, start_y) + height);

        float alpha = .6f;
        float energy = seconds_to_live + seconds_to_init;
        return new ParametricEmitter(world, new CloudFunction(2.5f, .7f), pos,
                0f, offset_z, .5f, .5f, .2f,
                25, 100f,
                new Color.Standard(.4f, .4f, .4f, alpha).linear(), Color.LinearDelta.ZERO.alpha(-alpha / energy),
                new Vector3f(3f, 3f, 1f), new Vector3f(0f, 0f, 0f), energy,
                GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, world.getRacesResources().getSmokeTextures());
    }

    @Override
    public void animate(float t) {
        if (first_run) {
            cloud_sound = owner.getWorld().getAudio().newAudio(getPositionX(), getPositionY(), getPositionZ(),
                    AudioAssets.LIGHTNING_CLOUD);
            first_run = false;
            bubbling_sound.stop(15.0f);
        }
        cloud_sound.setPosition(getPositionX(), getPositionY(), getPositionZ());
        seconds_to_live -= t;
        if (seconds_to_live <= 0f) {
            owner.getWorld().getAnimationManagerGameTime().removeAnimation(this);
            cloud_sound.stop(15.0f);
            remove();
        }
        lightning_timer -= t;
        if (lightning_timer <= 0 && lighted) {
            emitter.adjustColor(BRIGHTNESS_DELTA.negate());
            lighted = false;
        }

        hit_timer += t;

        if (hit_timer > seconds_per_hit) {
            if (target == null) {
                target = owner.findNearestEnemy(UnitGrid.toGridCoordinate(getPositionX()), UnitGrid.toGridCoordinate(
                        getPositionY()), prev_target);
                if (target == null) {
                    target = owner.findNearestEnemy(UnitGrid.toGridCoordinate(getPositionX()), UnitGrid
                            .toGridCoordinate(getPositionY()), null);
                    if (target == null) {
                        super.animate(t);
                        return;
                    }
                }
            }

            float dx = target.getPositionX() - getPositionX();
            float dy = target.getPositionY() - getPositionY();
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            dx /= dist;
            dy /= dist;
            if (dist < meters_per_second * t) {
                if (!target.isDead() && owner.getWorld().getRandom().nextFloat() < hit_chance * (1 - target
                        .getDefenseChance())) {
                    target.hit(damage, dx, dy, owner);
                }
                float x = target.getPositionX();
                float y = target.getPositionY();
                float z = owner.getWorld().getHeightMap().getNearestHeight(x, y);
                var params = new AudioParameters(
                        AudioAssets.SFX_FLASH, AudioAssets.AUDIO_RANK_MAGIC,
                        AudioAssets.AUDIO_DISTANCE_MAGIC, AudioAssets.AUDIO_GAIN_LIGHTNING,
                        AudioAssets.AUDIO_RADIUS_LIGHTNING);
                owner.getWorld().getAudio().newAudio(x, y, z, params);
                strike(target);
                strike(target);
                prev_target = target;
                target = null;
                hit_timer = 0f;
                strike_counter = 0;
            } else {
                float x = getPositionX() + dx * (meters_per_second * t);
                float y = getPositionY() + dy * (meters_per_second * t);
                float z = owner.getWorld().getHeightMap().getNearestHeight(x, y) + height;
                setPosition(x, y);
                setPositionZ(z);
                reinsert();
            }
        } else if (prev_target != null && strike_counter < NUM_STRIKES - 1 && hit_timer > (strike_counter + 1)
                * SECONDS_BETWEEN_STRIKES) {
                    strike(prev_target);
                    strike(prev_target);
                    strike_counter++;
                }
        super.animate(t);
    }

    private void strike(@NonNull Target target) {
        if (lightning_timer <= 0f) {
            emitter.adjustColor(BRIGHTNESS_DELTA);
            lightning_timer = LIGHTNING_TIME;
            lighted = true;
        }
        float x = target.getPositionX();
        float y = target.getPositionY();
        float z = owner.getWorld().getHeightMap().getNearestHeight(x, y);

        Vector3f cloudPos = new Vector3f(getPositionX(), getPositionY(), getPositionZ());
        Lightning lightning = new Lightning(owner.getWorld(), cloudPos, new Vector3f(x, y, z), .5f,
                15, Color.Linear.WHITE, DELTA_COLOR,
                owner.getWorld().getRacesResources().getLightningTexture(), LIGHTNING_TIME,
                owner.getWorld().getAnimationManagerGameTime());
        lightning.register();
    }

    @Override
    public void interrupt() {
        bubbling_sound.stop(15.0f);
        remove();
    }
}
