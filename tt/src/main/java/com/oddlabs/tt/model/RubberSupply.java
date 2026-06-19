package com.oddlabs.tt.model;


import com.oddlabs.tt.animation.Animated;
import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.pathfinder.Movable;
import com.oddlabs.tt.pathfinder.PathTracker;
import com.oddlabs.tt.pathfinder.Region;
import com.oddlabs.tt.pathfinder.TargetTrackerAlgorithm;
import com.oddlabs.tt.pathfinder.UnitGrid;
import com.oddlabs.tt.resource.AudioAssets;
import org.jspecify.annotations.NonNull;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Represents a rubber resource, visually represented as a chicken.
 */
public final class RubberSupply extends SupplyModel implements Animated, Movable {
    private static final float MIN_TREE_FALL_HEIGHT = 4f;
    private static final float MAX_TREE_FALL_HEIGHT = 8f;
    private static final float METERS_PER_SECOND = 8f;

    private static final int INITIAL_SUPPLIES = 1;
    private static final int MAX_MOVE_GRIDS = 5;

    public enum Animation {
        IDLING(1f / (50f / 25f)),
        PECKING(1f / (120f / 50f)),
        DYING(1f / (150f / 50f)),
        RUNNING(METERS_PER_SECOND),
        FLYING(METERS_PER_SECOND);

        private final float speed;

        Animation(float speed) {
            this.speed = speed;
        }

        public float getSpeed() {
            return speed;
        }
    }

    private final @NonNull PathTracker path_tracker;
    private final int start_grid_x;
    private final int start_grid_y;
    private final float spawn_x;
    private final float spawn_y;

    private final @NonNull RubberGroup group;

    private float anim_time = 0;
    private @NonNull Animation animation = Animation.IDLING;
    private boolean is_hit = false;

    private boolean spawning = true;
    private float spawnProgress = 0f;
    private static final float SPAWN_LIMIT = 3.0f;
    private final float spawn_offset_z;

    public RubberSupply(@NonNull World world, int grid_x, int grid_y,
            float x, float y, @NonNull RubberGroup group, float spawn_x, float spawn_y) {
        var spawn_z = world.getRandom().nextFloat(MIN_TREE_FALL_HEIGHT, MAX_TREE_FALL_HEIGHT);
        super(world, 2f, grid_x, grid_y, x, y, 0f, INITIAL_SUPPLIES, false,
                world.getLandscapeResources().getChickenBounds());
        this.spawn_offset_z = spawn_z;
        this.path_tracker = new PathTracker(world.getUnitGrid(), this);
        this.group = group;
        start_grid_x = grid_x;
        start_grid_y = grid_y;
        this.spawn_x = spawn_x;
        this.spawn_y = spawn_y;

        float dx = x - spawn_x;
        float dy = y - spawn_y;
        float inv_len = 1f / (float) Math.hypot(dx, dy);
        setDirection(dx * inv_len, dy * inv_len);
        setNewAnimation(Animation.FLYING);
    }

    @Override
    public @NonNull SupplyType getSupplyType() {
        return SupplyType.RUBBER;
    }

    @Override
    protected float getZError() {
        return getLandscapeError();
    }

    @Override
    public float getShadowDiameter() {
        return 1.2f;
    }

    @Override
    public @NonNull Supply respawn() {
        throw new UnsupportedOperationException("RubberSupply cannot respawn");
    }

    @Override
    public @NonNull PathTracker getTracker() {
        return path_tracker;
    }

    @Override
    public boolean isMoving() {
        return animation == Animation.FLYING || animation == Animation.RUNNING;
    }

    @Override
    public void free() {
        getWorld().getUnitGrid().freeGrid(getGridX(), getGridY(), this);
    }

    @Override
    public void occupy() {
        getWorld().getUnitGrid().occupyGrid(getGridX(), getGridY(), this);
    }

    @Override
    public void setGridPosition(int grid_x, int grid_y) {
        Region current_region = getWorld().getUnitGrid().getRegion(getGridX(), getGridY());
        Region new_region = getWorld().getUnitGrid().getRegion(grid_x, grid_y);
        if (current_region != new_region) {
            current_region.unregisterObject(RubberSupply.class, this);
            new_region.registerObject(RubberSupply.class, this);
        }
        super.setGridPosition(grid_x, grid_y);
    }

    @Override
    public void markBlocking() {
    }

    public boolean isHit() {
        return is_hit;
    }

    @Override
    public void animate(float t) {
        animateClientState(t);
        if (spawning) {
            spawnProgress = Math.min(1.0f, spawnProgress + t / SPAWN_LIMIT);
            anim_time += animation.getSpeed() * t;
            float x = spawn_x + (UnitGrid.coordinateFromGrid(getGridX()) - spawn_x) * spawnProgress;
            float y = spawn_y + (UnitGrid.coordinateFromGrid(getGridY()) - spawn_y) * spawnProgress;
            setPosition(x, y);
            if (spawnProgress >= 1.0f) {
                spawning = false;
                setNewAnimation(Animation.IDLING);
                reinsert();
            }
            return;
        }
        anim_time += animation.getSpeed() * t;
        if (animation == Animation.FLYING || animation == Animation.RUNNING) {
            fly(t);
        } else if (!is_hit && anim_time >= 1f) {
            float random = getWorld().getRandom().nextFloat();
            if (random < .75) {
                setNewAnimation(Animation.IDLING);
                if (random < .05) {
                    getClientState(ModelClient.class).ifPresent(client -> {
                        client.playSound(AudioAssets.CHICKEN_IDLES[ThreadLocalRandom.current().nextInt(
                                AudioAssets.CHICKEN_IDLES.length)]);
                        client.addVisualSound(EmojiType.CHICKEN_CLUCK,
                                ModelClient.DURATION_CHICKEN_CLUCK, AudioAssets.AUDIO_DISTANCE_CHICKEN);
                    });
                }
            } else if (random < .85) {
                // move
                int max_grids = (int) MAX_MOVE_GRIDS;
                int new_grid_x = start_grid_x + getWorld().getRandom().nextInt(-max_grids, max_grids + 1);
                int new_grid_y = start_grid_y + getWorld().getRandom().nextInt(-max_grids, max_grids + 1);
                UnitGrid grid = getWorld().getUnitGrid();
                if (new_grid_x >= 0 && new_grid_y >= 0 && new_grid_x < grid.getGridSize() && new_grid_y < grid
                        .getGridSize()) {
                    Target target = grid.findGridTargets(new_grid_x, new_grid_y, 1, false)[0];
                    path_tracker.setTarget(new TargetTrackerAlgorithm(grid, 0f, target));
                    float move_random = getWorld().getRandom().nextFloat();
                    if (move_random < .25f) {
                        setNewAnimation(Animation.FLYING);
                        getClientState(ModelClient.class).ifPresent(client -> client.playSound(
                                AudioAssets.CHICKEN_PECK));
                    } else {
                        setNewAnimation(Animation.RUNNING);
                    }
                }
            } else {
                setNewAnimation(Animation.PECKING);
                if (random > .98f)
                    getClientState(ModelClient.class).ifPresent(client -> client.playSound(AudioAssets.CHICKEN_PECK));

            }
        }
    }

    private void fly(float t) {
        PathTracker.State state = path_tracker.animate(METERS_PER_SECOND * t);
        switch (state) {
            case OK, OK_INTERRUPTIBLE -> {
            }
            case DONE, BLOCKED, SOFTBLOCKED -> setNewAnimation(Animation.IDLING);
            default -> throw new IllegalStateException("Invalid tracker state: " + state);
        }
    }

    private void setNewAnimation(@NonNull Animation animation) {
        anim_time = 0;
        this.animation = animation;
    }

    @Override
    public int getAnimation() {
        // This method is called during super constructor before field is initialized.
        //noinspection ConstantValue
        return null != animation ? animation.ordinal() : 0;
    }

    @Override
    public float getAnimationTicks() {
        return anim_time;
    }

    @Override
    public boolean hit() {
        if (!is_hit) {
            is_hit = true;
            setNewAnimation(Animation.DYING);
            getClientState(ModelClient.class).ifPresent(client -> client.playSound(AudioAssets.CHICKEN_DEATH));
            group.remove(this);
        }
        return super.hit();
    }

    @Override
    public float getOffsetZ() {
        float slope = getSlopeOffset();
        if (spawning) {
            return (1.0f - spawnProgress * spawnProgress) * spawn_offset_z + slope;
        }
        return slope;
    }
}
