package com.oddlabs.tt.model.weapon;

import com.oddlabs.tt.audio.AbstractAudioPlayer;
import com.oddlabs.tt.audio.AudioParameters;
import com.oddlabs.tt.audio.AudioPlayer;
import com.oddlabs.tt.global.Settings;
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
import com.oddlabs.tt.util.Target;
import com.oddlabs.util.Color;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.joml.Vector4fc;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;

/**
 * Logic controller for the Lightning Cloud magic effect.
 */
public final class LightningCloud extends PointEmitterModel implements Magic {
    private static final int NUM_STRIKES = 6;
    private static final float SECONDS_BETWEEN_STRIKES = .125f;
    private static final float BRIGHTNESS = .2f;
    private static final float LIGHTNING_TIME = .1f;
    private static final Vector4fc DELTA_COLOR = new Vector4f(0f, 0f, 0f, -1f / LIGHTNING_TIME);

    private final @NonNull Player owner;
    private final float seconds_per_hit;
    private final float meters_per_second;
    private final float hit_chance;
    private final int damage;
    private final float height;
    private final @Nullable AbstractAudioPlayer bubbling_sound;
    private AbstractAudioPlayer cloud_sound;

    private float seconds_to_live;
    private @Nullable Selectable<?> target = null;
    private @Nullable Selectable<?> prev_target = null;
    private float hit_timer = 0f;
    private int strike_counter = 0;
    private float lightning_timer = 0f;
    private boolean lighted = false;
    private boolean first_run = true;

    public LightningCloud(@NonNull World world, float offset_x, float offset_y, float offset_z, float seconds_to_live, float seconds_per_hit, float seconds_to_init, float meters_per_second, float hit_chance, int damage, float height, @NonNull Unit src) {
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

        bubbling_sound = world.getAudio().newAudio(new AudioParameters<>(world.getRacesResources().getBubblingSound(), getPositionX(), getPositionY(), world.getHeightMap().getNearestHeight(getPositionX(), getPositionY()),
                AudioPlayer.AUDIO_RANK_MAGIC,
                AudioPlayer.AUDIO_DISTANCE_MAGIC,
                AudioPlayer.AUDIO_GAIN_BUBBLING,
                AudioPlayer.AUDIO_RADIUS_BUBBLING,
                1f, true, false));
    }

    private static Emitter<?> createEmitter(@NonNull World world, float offset_x, float offset_y, float offset_z, float seconds_to_live, float seconds_to_init, float height, @NonNull Unit src) {
        float start_x = src.getPositionX() + offset_x * src.getDirectionX() - offset_y * (-src.getDirectionY());
        float start_y = src.getPositionY() + offset_x * src.getDirectionY() + offset_y * src.getDirectionX();
        Vector3f pos = new Vector3f(start_x, start_y, world.getHeightMap().getNearestHeight(start_x, start_y) + height);

        return new ParametricEmitter(world, new CloudFunction(2.5f, .7f), pos,
                0f, 0f, .5f, .5f, .2f,
                25, 100f,
                new Vector4f(.4f, .4f, .4f, .6f), new Vector4f(0f, 0f, 0f, 0f),
                new Vector3f(3f, 3f, 1f), new Vector3f(0f, 0f, 0f), seconds_to_live + seconds_to_init,
                GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, world.getRacesResources().getSmokeTextures());
    }

    @Override
    public void animate(float t) {
        if (first_run) {
            cloud_sound = owner.getWorld().getAudio().newAudio(new AudioParameters<>(owner.getWorld().getRacesResources().getCloudSound(), getPositionX(), getPositionY(), getPositionZ(),
                    AudioPlayer.AUDIO_RANK_MAGIC,
                    AudioPlayer.AUDIO_DISTANCE_MAGIC,
                    AudioPlayer.AUDIO_GAIN_CLOUD,
                    AudioPlayer.AUDIO_RADIUS_CLOUD,
                    1f, true, false));
            first_run = false;
            bubbling_sound.stop(.2f, Settings.getSettings().sound_gain);
        }
        cloud_sound.setPos(getPositionX(), getPositionY(), getPositionZ());
        seconds_to_live -= t;
        if (seconds_to_live <= 0f) {
            owner.getWorld().getAnimationManagerGameTime().removeAnimation(this);
            cloud_sound.stop(.2f, Settings.getSettings().sound_gain);
            remove();
        }
        lightning_timer -= t;
        if (lightning_timer <= 0 && lighted) {
            emitter.forceColorChange(-BRIGHTNESS, -BRIGHTNESS, -BRIGHTNESS, 0f);
            lighted = false;
        }

        hit_timer += t;

        if (hit_timer > seconds_per_hit) {
            if (target == null) {
                target = owner.findNearestEnemy(UnitGrid.toGridCoordinate(getPositionX()), UnitGrid.toGridCoordinate(getPositionY()), prev_target);
                if (target == null) {
                    target = owner.findNearestEnemy(UnitGrid.toGridCoordinate(getPositionX()), UnitGrid.toGridCoordinate(getPositionY()), null);
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
                if (!target.isDead() && owner.getWorld().getRandom().nextFloat() < hit_chance * (1 - target.getDefenseChance())) {
                    target.hit(damage, dx, dy, owner);
                }
                float x = target.getPositionX();
                float y = target.getPositionY();
                float z = owner.getWorld().getHeightMap().getNearestHeight(x, y);
                owner.getWorld().getAudio().newAudio(new AudioParameters<>(owner.getWorld().getRacesResources().getLightningSound(), x, y, z,
                        AudioPlayer.AUDIO_RANK_MAGIC,
                        AudioPlayer.AUDIO_DISTANCE_MAGIC,
                        AudioPlayer.AUDIO_GAIN_LIGHTNING,
                        AudioPlayer.AUDIO_RADIUS_LIGHTNING));
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
        } else if (prev_target != null && strike_counter < NUM_STRIKES - 1 && hit_timer > (strike_counter + 1) * SECONDS_BETWEEN_STRIKES) {
            strike(prev_target);
            strike(prev_target);
            strike_counter++;
        }
        super.animate(t);
    }

    private void strike(@NonNull Target target) {
        if (lightning_timer <= 0f) {
            emitter.forceColorChange(BRIGHTNESS, BRIGHTNESS, BRIGHTNESS, 0f);
            lightning_timer = LIGHTNING_TIME;
            lighted = true;
        }
        float x = target.getPositionX();
        float y = target.getPositionY();
        float z = owner.getWorld().getHeightMap().getNearestHeight(x, y);
        
        Vector3f cloudPos = new Vector3f(getPositionX(), getPositionY(), getPositionZ());
        Lightning lightning = new Lightning(owner.getWorld(), cloudPos, new Vector3f(x, y, z), .5f,
                15, Color.WHITE, DELTA_COLOR,
                owner.getWorld().getRacesResources().getLightningTexture(), LIGHTNING_TIME,
                owner.getWorld().getAnimationManagerGameTime());
        lightning.register();
    }

    @Override
    public void interrupt() {
        if (bubbling_sound != null)
            bubbling_sound.stop(.2f, Settings.getSettings().sound_gain);
        remove();
    }
}
