package com.oddlabs.tt.model.weapon;

import com.oddlabs.tt.render.Renderer;

import com.oddlabs.tt.audio.Audio;
import com.oddlabs.tt.audio.AudioParameters;
import com.oddlabs.tt.audio.AudioPlayer;
import com.oddlabs.tt.global.Settings;
import com.oddlabs.tt.model.PointEmitterModel;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.particle.RandomVelocityEmitter;
import com.oddlabs.tt.pathfinder.FindOccupantFilter;
import com.oddlabs.tt.pathfinder.UnitGrid;
import com.oddlabs.tt.player.Player;
import com.oddlabs.util.Color;
import org.joml.Vector3f;
import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;

/**
 * Logic controller for the Poison Fog magic effect.
 * Periodically spawns gas bursts and applies damage to units within its radius.
 */
public final class PoisonFog implements Magic {
    public static final float OFFSET_Z = 1.1f;

    private static final int PARTICLES_PER_BURST = 4;
    private static final float SECONDS_BETWEEN_BURSTS = .15f;
    private static final float BURST_RADIUS = 2f;
    private static final float GAUSSIAN_LIMIT = 2.5f;
    private static final int MIN_BURSTS_PER_SOUND = 2;

    private static final AudioParameters<Audio> BUBBLING_AUDIO = new AudioParameters<>(
            AudioPlayer.SFX_BUBBLING, AudioPlayer.AUDIO_RANK_MAGIC,
            AudioPlayer.AUDIO_DISTANCE_MAGIC, AudioPlayer.AUDIO_GAIN_BUBBLING, AudioPlayer.AUDIO_RADIUS_BUBBLING,
            1f, true, false);

    private static final AudioParameters<Audio> GAS_AUDIO = new AudioParameters<>(AudioPlayer.SFX_GAS,
            AudioPlayer.AUDIO_RANK_GAS,
            AudioPlayer.AUDIO_DISTANCE_MAGIC,
            AudioPlayer.AUDIO_GAIN_GAS,
            AudioPlayer.AUDIO_RADIUS_GAS);

    private final float hit_radius;
    private final float hit_chance;
    private final float interval;
    private final int damage;
    private final @NonNull Unit src;
    private final @NonNull Player owner;
    private final float start_x;
    private final float start_y;
    private final float total_time;
    private final @NonNull AudioPlayer bubbling_sound;

    private int next_sound = 1;
    private float time = 0f;
    private int bursts = 0;
    private int num_hits = 0;
    private boolean first_run = true;

    public PoisonFog(float offset_x, float offset_y, float offset_z, float hit_radius, float hit_chance, float interval, float time, int damage, @NonNull Unit src) {
        this.hit_radius = hit_radius;
        this.hit_chance = hit_chance;
        this.interval = interval;
        total_time = time;
        this.damage = damage;
        this.src = src;
        owner = src.getOwner();

        start_x = src.getPositionX() + offset_x * src.getDirectionX() - offset_y * (-src.getDirectionY());
        start_y = src.getPositionY() + offset_x * src.getDirectionY() + offset_y * src.getDirectionX();

        bubbling_sound = owner.getWorld().getAudio().newAudio(start_x, start_y, owner.getWorld().getHeightMap().getNearestHeight(start_x, start_y), BUBBLING_AUDIO);
    }

    @Override
    public void animate(float t) {
        time += t;
        if (time >= total_time) {
            owner.getWorld().getAnimationManagerGameTime().removeAnimation(this);
        }
        if (first_run) {
            bubbling_sound.stop(.2f, Renderer.getRenderer().getSettings().sound_gain);
            first_run = false;
        }

        if (bursts * SECONDS_BETWEEN_BURSTS < time) {
            float gaussian = (float) (GAUSSIAN_LIMIT - Math.abs(Math.clamp(owner.getWorld().getRandom().nextGaussian(), -GAUSSIAN_LIMIT, GAUSSIAN_LIMIT))) / GAUSSIAN_LIMIT;
            float r = gaussian * (hit_radius - BURST_RADIUS - 5f);
            float a = owner.getWorld().getRandom().nextFloat() * (float) Math.PI * 2;
            float x = start_x + (float) Math.cos(a) * r;
            float y = start_y + (float) Math.sin(a) * r;
            float z = owner.getWorld().getHeightMap().getNearestHeight(x, y);
            float alpha = 8f;
            float energy = 2f;

            RandomVelocityEmitter emitter = new RandomVelocityEmitter(owner.getWorld(), new Vector3f(x, y, z), OFFSET_Z, owner.getWorld().getRandom().nextFloat() * (float) Math.PI * 2,
                    BURST_RADIUS, 0f, 0f, 0f,
                    PARTICLES_PER_BURST, PARTICLES_PER_BURST,
                    new Vector3f(0f, 0f, 0f), new Vector3f(0f, 0f, 0f),
                    new Color.Standard(1f, 1f, 1f, alpha), new Color.Standard(0f, 0f, 0f, -alpha / energy),
                    new Vector3f(0f, 0f, .25f), new Vector3f(3.5f, 3.5f, 0f), energy, 1f,
                    GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                    owner.getWorld().getRacesResources().getPoisonTextures());
            new PointEmitterModel(owner.getWorld(), emitter);

            if (bursts % next_sound == 0) {
                next_sound = MIN_BURSTS_PER_SOUND + owner.getWorld().getRandom().nextInt(5);
                owner.getWorld().getAudio().newAudio(x, y, z, GAS_AUDIO);
            }
            bursts++;
        }

        if ((num_hits + 1) * interval < time) {
            hitUnits(hit_radius);
            num_hits++;
        }
    }

    private void hitUnits(float radius) {
        FindOccupantFilter<Unit> filter = new FindOccupantFilter<>(start_x, start_y, radius, src, Unit.class);
        UnitGrid unit_grid = owner.getWorld().getUnitGrid();
        unit_grid.scan(filter, UnitGrid.toGridCoordinate(start_x), UnitGrid.toGridCoordinate(start_y));
        for (var s : filter.getResult()) {
            float dx = s.getPositionX() - start_x;
            float dy = s.getPositionY() - start_y;
            float squared_dist = dx * dx + dy * dy;
            if (!s.isDead() && ((owner.isEnemy(s.getOwner()) && owner.getWorld().getRandom().nextFloat() < hit_chance * (1 - s.getDefenseChance()))
                    || (!owner.isEnemy(s.getOwner()) && owner.getWorld().getRandom().nextFloat() < (hit_chance / 4f) * (1 - s.getDefenseChance())
                    && s != owner.getChieftain()))) {
                float inv_dist = 1f / ((float) Math.sqrt(squared_dist));
                s.hit(damage, dx * inv_dist, dy * inv_dist, owner);
            }
        }
    }

    @Override
    public void interrupt() {
        bubbling_sound.stop(.2f, Renderer.getRenderer().getSettings().sound_gain);
    }
}
