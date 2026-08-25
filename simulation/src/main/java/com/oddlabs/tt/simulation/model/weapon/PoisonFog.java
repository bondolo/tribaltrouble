package com.oddlabs.tt.simulation.model.weapon;

import com.oddlabs.tt.base.geom.BoundingBox;
import com.oddlabs.tt.simulation.model.Model;
import com.oddlabs.tt.simulation.model.Unit;
import com.oddlabs.tt.simulation.pathfinder.FindOccupantFilter;
import com.oddlabs.tt.simulation.pathfinder.UnitGrid;
import com.oddlabs.tt.simulation.player.Player;
import org.jspecify.annotations.Nullable;

/**
 * Logic controller for the Poison Fog magic effect.
 * Periodically spawns gas bursts and applies damage to units within its radius.
 */
public final class PoisonFog extends Model implements Magic {

    private static final int PARTICLES_PER_BURST = 4;
    private static final float SECONDS_BETWEEN_BURSTS = .15f;
    private static final float BURST_RADIUS = 2f;
    private static final float GAUSSIAN_LIMIT = 2.5f;


    private final float hit_radius;
    private final float hit_chance;
    private final float interval;
    private final int damage;
    private final Unit src;
    private final Player owner;
    private final float start_x;
    private final float start_y;
    private final float offset_z;
    private final float total_time;

    private float time = 0f;
    private int bursts = 0;
    private int num_hits = 0;

    public PoisonFog(float offset_x, float offset_y, float offset_z, float hit_radius, float hit_chance, float interval,
            float time, int damage, Unit src) {
        super(src.getOwner().getWorld());
        this.hit_radius = hit_radius;
        this.hit_chance = hit_chance;
        this.interval = interval;
        this.offset_z = offset_z;
        total_time = time;
        this.damage = damage;
        this.src = src;
        owner = src.getOwner();

        start_x = src.getPositionX() + offset_x * src.getDirectionX() - offset_y * (-src.getDirectionY());
        start_y = src.getPositionY() + offset_x * src.getDirectionY() + offset_y * src.getDirectionX();
        setPosition(start_x, start_y, owner.getWorld().getHeightMap().getNearestHeight(start_x, start_y));
        register();
        owner.getWorld().getAnimationManagerGameTime().registerAnimation(this);
    }

    public float getHitRadius() {
        return hit_radius;
    }

    public float getCloudOffsetZ() {
        return offset_z;
    }

    @Override
    public void remove() {
        super.remove();
        owner.getWorld().getAnimationManagerGameTime().removeAnimation(this);
        owner.getWorld().getNotificationListener().onModelRemoved(this);
    }

    @Override
    protected void onReinsert() {
        float x = getPositionX();
        float y = getPositionY();
        float z = getPositionZ();
        setBounds(x - hit_radius, x + hit_radius, y - hit_radius, y + hit_radius, z - 5f, z + 20f);
    }

    @Override
    protected BoundingBox @Nullable [] getLocalBounds() {
        return null;
    }

    @Override
    public void animate(float t) {
        time += t;
        if (time >= total_time) {
            remove();
            return;
        }

        if (bursts * SECONDS_BETWEEN_BURSTS < time) {
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
            if (!s.isDead() && ((owner.isEnemy(s.getOwner()) && owner.getWorld().getRandom().nextFloat() < hit_chance
                    * (1 - s.getDefenseChance()))
                    || (!owner.isEnemy(s.getOwner()) && owner.getWorld().getRandom().nextFloat() < (hit_chance / 4f)
                            * (1 - s.getDefenseChance())
                            && !owner.getChieftain().map(c -> s == c).orElse(false)))) {
                float inv_dist = 1f / ((float) Math.sqrt(squared_dist));
                s.hit(damage, dx * inv_dist, dy * inv_dist, owner);
            }
        }
    }

    @Override
    public void interrupt() {
        remove();
    }
}
