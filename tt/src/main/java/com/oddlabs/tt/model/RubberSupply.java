package com.oddlabs.tt.model;


import com.oddlabs.tt.animation.Animated;
import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.pathfinder.*;
import com.oddlabs.tt.render.SpriteKey;
import com.oddlabs.tt.resource.AudioAssets;
import com.oddlabs.tt.util.Target;
import org.jspecify.annotations.NonNull;

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
    private final float spawn_z;

    private final @NonNull RubberGroup group;

    private float anim_time = 0;
    private @NonNull Animation animation = Animation.IDLING;
    private boolean is_hit = false;
    private boolean spawning;

    public RubberSupply(@NonNull World world, @NonNull SpriteKey sprite_renderer, int grid_x, int grid_y,
            float x, float y, @NonNull RubberGroup group, float spawn_x, float spawn_y) {
        var spawn_z = world.getRandom().nextFloat() * (MAX_TREE_FALL_HEIGHT - MIN_TREE_FALL_HEIGHT)
                + MIN_TREE_FALL_HEIGHT;
        super(world, sprite_renderer, 2f, grid_x, grid_y, x, y, spawn_z, 0, INITIAL_SUPPLIES, false);
        this.path_tracker = new PathTracker(world.getUnitGrid(), this);
        this.group = group;
        start_grid_x = grid_x;
        start_grid_y = grid_y;
        this.spawn_x = spawn_x;
        this.spawn_y = spawn_y;
        this.spawn_z = spawn_z;
        spawning = true;
        float dx = x - spawn_x;
        float dy = y - spawn_y;
        float inv_len = 1f / (float) Math.hypot(dx, dy);
        setDirection(dx * inv_len, dy * inv_len);
        setNewAnimation(Animation.FLYING);
        setShowShadow(false);
    }

    @Override
    public @NonNull SpriteKey getStatusSprite(@NonNull RacesResources resources) {
        return resources.getRubberStatusSprite();
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
    public void animateSpawn(float t, float progress) {
        anim_time += animation.getSpeed() * t;
        float x = spawn_x + (UnitGrid.coordinateFromGrid(getGridX()) - spawn_x) * progress;
        float y = spawn_y + (UnitGrid.coordinateFromGrid(getGridY()) - spawn_y) * progress;
        setPosition(x, y);
        offset_z = spawn_z - spawn_z * progress * progress;
        reinsert();
    }

    @Override
    public void spawnComplete() {
        offset_z = 0;
        spawning = false;
        setNewAnimation(Animation.IDLING);
        setShowShadow(true);
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
        return false;
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
    @SuppressWarnings("unchecked")
    public void setGridPosition(int grid_x, int grid_y) {
        Region current_region = getWorld().getUnitGrid().getRegion(getGridX(), getGridY());
        Region new_region = getWorld().getUnitGrid().getRegion(grid_x, grid_y);
        if (current_region != new_region) {
            current_region.unregisterObject((Class<RubberSupply>) getClass(), this);
            new_region.registerObject((Class<RubberSupply>) getClass(), this);
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
        if (spawning)
            return;
        anim_time += animation.getSpeed() * t;
        if (animation == Animation.FLYING || animation == Animation.RUNNING) {
            fly(t);
        } else if (!is_hit && anim_time >= 1f) {
            float random = getWorld().getRandom().nextFloat();
            if (random < .75) {
                setNewAnimation(Animation.IDLING);
                if (random < .05) {
                    getWorld().getAudio().newAudio(getPositionX(), getPositionY(), getPositionZ(),
                            AudioAssets.CHICKEN_IDLES[getWorld().getRandom().nextInt(
                                    AudioAssets.CHICKEN_IDLES.length)]);
                    RacesResources racesResources = getWorld().getRacesResources();
                    if (racesResources != null) {
                        var chickenThoughts = racesResources.getChickenEmojiSprites();
                        var thought = chickenThoughts[getWorld().getRandom().nextInt(chickenThoughts.length)];
                        ModelClient client = getClientState(ModelClient.class);
                        if (client != null) {
                            client.addVisualSound(thought,
                                    ModelClient.DURATION_CHICKEN_CLUCK, AudioAssets.AUDIO_DISTANCE_CHICKEN);
                        }
                    }
                }
            } else if (random < .85) {
                // fly
                int new_grid_x = start_grid_x + (int) ((getWorld().getRandom().nextFloat() * 2 - 1) * MAX_MOVE_GRIDS);
                int new_grid_y = start_grid_y + (int) ((getWorld().getRandom().nextFloat() * 2 - 1) * MAX_MOVE_GRIDS);
                Target target = getWorld().getUnitGrid().findGridTargets(new_grid_x, new_grid_y, 1, false)[0];
                path_tracker.setTarget(new TargetTrackerAlgorithm(getWorld().getUnitGrid(), 0f, target));
                float move_random = getWorld().getRandom().nextFloat();
                if (move_random < .25f) {
                    setNewAnimation(Animation.FLYING);
                    getWorld().getAudio().newAudio(getPositionX(), getPositionY(), getPositionZ(),
                            AudioAssets.CHICKEN_PECK);
                } else {
                    setNewAnimation(Animation.RUNNING);
                }
            } else {
                setNewAnimation(Animation.PECKING);
                if (random > .98f)
                    getWorld().getAudio().newAudio(getPositionX(), getPositionY(), getPositionZ(),
                            AudioAssets.CHICKEN_PECK);

            }
        }
    }

    private void fly(float t) {
        PathTracker.State state = path_tracker.animate(METERS_PER_SECOND * t);
        switch (state) {
            case OK, OK_INTERRUPTIBLE -> {
            }
            case DONE, BLOCKED, SOFTBLOCKED -> {
                setNewAnimation(Animation.IDLING);
            }
            default -> throw new IllegalStateException("Invalid tracker state: " + state);
        }
    }

    @Override
    public float getOffsetZ() {
        return offset_z;
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
            getWorld().getAudio().newAudio(getPositionX(), getPositionY(), getPositionZ(), AudioAssets.CHICKEN_DEATH);
            RacesResources racesResources = getWorld().getRacesResources();
            if (racesResources != null) {
                ModelClient client = getClientState(ModelClient.class);
                if (client != null) {
                    client.addVisualSound(getStatusSprite(racesResources),
                            ModelClient.DURATION_CHICKEN_DEATH, AudioAssets.AUDIO_DISTANCE_DEATH);
                }
            }
            group.remove(this);
        }
        return super.hit();
    }

    @Override
    public void register() {
        super.register();
        getWorld().getAnimationManagerGameTime().registerAnimation(this);
    }

    @Override
    public void remove() {
        getWorld().getAnimationManagerGameTime().removeAnimation(this);
        super.remove();
    }
}
