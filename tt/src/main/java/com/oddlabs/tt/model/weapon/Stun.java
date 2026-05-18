package com.oddlabs.tt.model.weapon;

import com.oddlabs.tt.audio.Assets;
import com.oddlabs.tt.audio.AudioParameters;
import com.oddlabs.tt.audio.AudioPlayer;
import com.oddlabs.tt.model.Abilities;
import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.model.MountUnitContainer;
import com.oddlabs.tt.model.PointEmitterModel;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.particle.Emitter;
import com.oddlabs.tt.particle.RandomVelocityEmitter;
import com.oddlabs.tt.pathfinder.FindOccupantFilter;
import com.oddlabs.tt.pathfinder.UnitGrid;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.render.Renderer;
import com.oddlabs.util.Color;
import org.joml.Vector3f;
import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;

import java.util.Arrays;

/**
 * Logic controller for the Stun magic effect.
 */
public final class Stun extends PointEmitterModel implements Magic {
    private static final @NonNull AudioParameters [] STUN_AUDIO = Arrays.stream(Assets.SFX_LUR_STUNS)
            .map(audio -> new AudioParameters(audio, Assets.AUDIO_RANK_MAGIC,
                    Assets.AUDIO_DISTANCE_MAGIC, Assets.AUDIO_GAIN_STUN_LUR, Assets.AUDIO_RADIUS_STUN_LUR))
            .toArray(AudioParameters[]::new);
    private final @NonNull Unit src;
    private final float offset_x;
    private final float offset_y;
    private final float offset_z;
    private final float hit_radius;
    private final float stun_time_closest;
    private final float stun_time_farthest;
    private final @NonNull Player owner;
    private final @NonNull AudioPlayer sound;

    private final @NonNull Iterable<? extends Selectable<?>> target_list;

    public Stun(float offset_x, float offset_y, float offset_z, float hit_radius, float stun_time_closest, float stun_time_farthest, @NonNull Unit src) {
        super(src.getOwner().getWorld(), createEmitter(offset_x, offset_y, offset_z, src));
        this.src = src;
        this.offset_x = offset_x;
        this.offset_y = offset_y;
        this.offset_z = offset_z;
        this.hit_radius = hit_radius;
        this.stun_time_closest = stun_time_closest;
        this.stun_time_farthest = stun_time_farthest;
        this.owner = src.getOwner();

        float start_x = src.getPositionX() + offset_x * src.getDirectionX() - offset_y * (-src.getDirectionY());
        float start_y = src.getPositionY() + offset_x * src.getDirectionY() + offset_y * src.getDirectionX();
        float z = src.getPositionZ() + offset_z;

        var filter = new FindOccupantFilter<>(src.getPositionX(), src.getPositionY(), hit_radius, src, Selectable.genericClass());
//		FindOccupantFilter filter = new FindOccupantFilter(src.getPositionX(), src.getPositionY(), hit_radius, src, Unit.class);
        UnitGrid unit_grid = owner.getWorld().getUnitGrid();
        unit_grid.scan(filter, UnitGrid.toGridCoordinate(src.getPositionX()), UnitGrid.toGridCoordinate(src.getPositionY()));
        target_list = filter.getResult();

        sound = owner.getWorld().getAudio().newAudio(start_x, start_y, z, STUN_AUDIO[getWorld().getRandom().nextInt(STUN_AUDIO.length)]);
    }

    private static Emitter<?> createEmitter(float offset_x, float offset_y, float offset_z, @NonNull Unit src) {
        Player owner = src.getOwner();
        float start_x = src.getPositionX() + offset_x * src.getDirectionX() - offset_y * (-src.getDirectionY());
        float start_y = src.getPositionY() + offset_x * src.getDirectionY() + offset_y * src.getDirectionX();
        float z = src.getPositionZ() + offset_z;
        float alpha = 12f;
        float energy = 4f;
        return new RandomVelocityEmitter(owner.getWorld(), new Vector3f(start_x, start_y, z), 0f, 0f,
                .001f, .001f, .5f, (float) Math.PI,
                -1, 35f,
                new Vector3f(0f, 0f, 6f), new Vector3f(0f, 0f, -2f),
                new Color.Standard(1f, 1f, 1f, alpha), new Color.Standard(0f, 0f, 0f, -alpha / energy),
                new Vector3f(.3f, .3f, .3f), new Vector3f(.025f, .025f, .025f), energy, 1f,
                GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                owner.getWorld().getRacesResources().getNoteTextures());
    }

    private float logic_timer = 0f;
    private boolean logic_done = false;

    @Override
    public void animate(float t) {
        if (!src.isDead()) {
            float x = src.getPositionX() + offset_x * src.getDirectionX() - offset_y * (-src.getDirectionY());
            float y = src.getPositionY() + offset_x * src.getDirectionY() + offset_y * src.getDirectionX();
            float z = src.getPositionZ() + offset_z;
            setPosition(x, y);
            setPositionZ(z);
            reinsert();
        }

        if (!logic_done) {
            for (Selectable<?> selectable : target_list) {
                Unit unit = null;
                if (selectable instanceof Unit unit1) {
                    unit = unit1;
                } else if (selectable instanceof Building building) {
                    if (!building.isDead() && building.getAbilities().hasAbilities(Abilities.ATTACK)) {
                        MountUnitContainer muc = (MountUnitContainer) building.getUnitContainer();
                        if (muc.getNumSupplies() > 0) {
                            unit = muc.getUnit();
                        }
                    }
                }

                if (unit == null || unit.isDead())
                    continue;

                float dx = unit.getPositionX() - getPositionX();
                float dy = unit.getPositionY() - getPositionY();
                float squared_dist = dx * dx + dy * dy;
                if (owner.isEnemy(unit.getOwner()) && squared_dist < hit_radius * hit_radius) {
                    float dist = (float) Math.sqrt(squared_dist);
                    float time = calculateValueFromCurrentRadius(dist, stun_time_closest, stun_time_farthest);
                    unit.stun(time);
                }
            }
            logic_done = true;
        }

        logic_timer += t;
        if (logic_timer > 1.5f) {
            emitter.done();
        }
        
        super.animate(t);
    }

    private float calculateValueFromCurrentRadius(float current_radius, float max, float min) {
        float base_factor = 6f / 7f;
        float error = (float) Math.pow(base_factor, hit_radius);
        float factor = (float) Math.pow(base_factor, current_radius);
        float result = (max - min + error) * factor + min - error;
        return result;
    }

    @Override
    public void interrupt() {
        emitter.done();
        sound.stop(.3f, 1.0f);
    }
}
