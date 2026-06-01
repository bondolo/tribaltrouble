package com.oddlabs.tt.model.weapon;

import com.oddlabs.tt.audio.AudioPlayer;
import com.oddlabs.tt.model.AccessorizableModel;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.particle.SonicBlastEffect;
import com.oddlabs.tt.pathfinder.FindOccupantFilter;
import com.oddlabs.tt.pathfinder.UnitGrid;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.render.SpriteKey;
import com.oddlabs.tt.resource.AudioAssets;
import org.joml.Vector3f;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;


/**
 * Logic controller for the Sonic Blast magic effect.
 */
public final class SonicBlast extends AccessorizableModel implements Magic {


    private final float hit_radius;
    private final float hit_chance_closest;
    private final float hit_chance_farthest;
    private final int damage_closest;
    private final int damage_farthest;
    private final float seconds;
    private final @NonNull Player owner;
    private final float start_x;
    private final float start_y;
    private final float start_z;
    private final @NonNull AudioPlayer lur;
    private final @NonNull AudioPlayer rumble;

    private float time = 0f;
    private final @NonNull Iterable<? extends Selectable<?>> blast_targets;
    private final @NonNull SonicBlastEffect sonicBlastEffect;

    private boolean first_ring_sent = false;

    public SonicBlast(float offset_x, float offset_y, float offset_z, float hit_radius, float hit_chance_closest,
            float hit_chance_farthest, int damage_closest, int damage_farthest, float seconds, @NonNull Unit src) {
        super(src.getOwner().getWorld());
        this.hit_radius = hit_radius;
        this.hit_chance_closest = hit_chance_closest;
        this.hit_chance_farthest = hit_chance_farthest;
        this.damage_closest = damage_closest;
        this.damage_farthest = damage_farthest;
        this.seconds = seconds;
        owner = src.getOwner();

        start_x = src.getPositionX() + offset_x * src.getDirectionX() - offset_y * (-src.getDirectionY());
        start_y = src.getPositionY() + offset_x * src.getDirectionY() + offset_y * src.getDirectionX();
        start_z = src.getPositionZ() + offset_z;

        setPosition(start_x, start_y);
        setPositionZ(start_z);
        register();

        var filter = new FindOccupantFilter<>(src.getPositionX(), src.getPositionY(), hit_radius, src, Selectable
                .genericClass());
        UnitGrid unit_grid = owner.getWorld().getUnitGrid();
        unit_grid.scan(filter, UnitGrid.toGridCoordinate(src.getPositionX()), UnitGrid.toGridCoordinate(src
                .getPositionY()));
        blast_targets = filter.getResult();

        sonicBlastEffect = new SonicBlastEffect(owner.getWorld(), new Vector3f(start_x, start_y, start_z), hit_radius,
                seconds);

        lur = owner.getWorld().getAudio().newAudio(start_x, start_y, start_z, AudioAssets.SONIC_BLAST_LUR[owner
                .getWorld().getRandom()
                .nextInt(AudioAssets.SONIC_BLAST_LUR.length)]);
        rumble = owner.getWorld().getAudio().newAudio(start_x, start_y, start_z, AudioAssets.SONIC_BLAST_RUMBLE);

        owner.getWorld().getAnimationManagerGameTime().registerAnimation(this);
    }

    @Override
    public void animate(float t) {
        time = Math.min(time + t, seconds);
        if (time >= seconds) {
            owner.getWorld().getAnimationManagerGameTime().removeAnimation(this);
        }

        if (!first_ring_sent) {
            first_ring_sent = true;

            owner.getWorld().getAudio().newAudio(start_x, start_y, start_z, AudioAssets.SONIC_BLAST);
            lur.stop(10.0f);
            rumble.stop(15.0f);
        }

        float current_radius = hit_radius * time / seconds;
        float squared_radius = current_radius * current_radius;

        var targets = blast_targets.iterator();
        while (targets.hasNext()) {
            var s = targets.next();
            float dx = s.getPositionX() - start_x;
            float dy = s.getPositionY() - start_y;
            float squared_dist = dx * dx + dy * dy;
            if (squared_dist < squared_radius) {
                if (!s.isDead()) {
                    float hit_chance = calculateValueFromCurrentRadius(current_radius, hit_chance_closest,
                            hit_chance_farthest);
                    if (owner.getWorld().getRandom().nextFloat() < hit_chance * (1 - s.getDefenseChance())) {
                        int damage = (int) calculateValueFromCurrentRadius(current_radius, damage_closest,
                                damage_farthest);
                        float inv_dist = 1f / ((float) Math.sqrt(squared_dist));
                        s.hit(damage, dx * inv_dist, dy * inv_dist, owner);
                    }
                }
                targets.remove();
            }
        }
        animateAccessories(t);
    }

    private float calculateValueFromCurrentRadius(float current_radius, float max, float min) {
        float base_factor = 4f / 7f;
        float error = (float) Math.pow(base_factor, hit_radius);
        float factor = (float) Math.pow(base_factor, current_radius);
        float result = (max - min + error) * factor + min - error;
        return result;
    }

    @Override
    public void interrupt() {
        lur.stop(15.0f);
        rumble.stop(15.0f);
        sonicBlastEffect.abort();
    }

    @Override
    public boolean isFinished() {
        return time >= seconds && sonicBlastEffect.isFinished();
    }

    @Override
    public @Nullable SpriteKey getSpriteRenderer() {
        return null;
    }

    public @NonNull SonicBlastEffect getSonicBlastEffect() {
        return sonicBlastEffect;
    }
}
