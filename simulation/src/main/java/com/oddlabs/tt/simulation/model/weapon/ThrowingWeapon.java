package com.oddlabs.tt.simulation.model.weapon;

import com.oddlabs.tt.base.animation.Animated;
import com.oddlabs.tt.simulation.model.Model;
import com.oddlabs.tt.simulation.model.ModelClient;
import com.oddlabs.tt.simulation.model.Selectable;
import com.oddlabs.tt.simulation.model.Unit;
import com.oddlabs.tt.simulation.model.WeaponVisualType;
import com.oddlabs.tt.simulation.player.Player;
import com.oddlabs.tt.simulation.model.BoundingBox;
import com.oddlabs.tt.base.event.StateChecksum;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Base {@link Model} class for all projectile weapons that are thrown through the world.
 */
public abstract sealed class ThrowingWeapon extends Model implements Animated permits RotatingThrowingWeapon,
        DirectedThrowingWeapon {
    /**
     * Multiplier for projectile arc exaggeration.
     */
    private static final float GRAVITY_MULTIPLIER = 3.0f;
    private static final float GRAVITY = -GRAVITY_MULTIPLIER * 9.82f;
    private static final float NO_DETAIL_SIZE = .5f;

    private static final float OFFSET_X = 1.316f;
    private static final float OFFSET_Y = -.347f;
    private static final float OFFSET_Z = 1.382f;

    private final boolean hit;
    private final @NonNull Unit src;
    /** rendering offset */
    private final float deterministic_z;

    /** the target of the weapon. Mutable because rubber weapons bounce and change targets **/
    private @NonNull Selectable<?> target;
    private float start_x;
    private float start_y;
    private float end_x;
    private float end_y;
    private float dir_x;
    private float dir_y;
    private float time_limit;
    private float time;
    private float z_speed;

    /** absolute height in the world */
    private float current_z;

    public ThrowingWeapon(boolean hit, @NonNull Unit src, @NonNull Selectable<?> target) {
        super(src.getOwner().getWorld());
        this.src = src;
        this.hit = hit;

        float x = src.getPositionX() + OFFSET_X * src.getDirectionX() - OFFSET_Y * src.getDirectionY();
        float y = src.getPositionY() + OFFSET_X * src.getDirectionY() - OFFSET_Y * src.getDirectionX();
        deterministic_z = OFFSET_Z + src.getMountOffset();
        current_z = getWorld().getHeightMap().getNearestHeight(x, y) + deterministic_z;

        setPosition(x, y, current_z - deterministic_z);

        setTarget(target);

        register();

        getWorld().getAnimationManagerGameTime().registerAnimation(this);

        // stats
        src.getOwner().weaponThrown();
    }

    public final @NonNull Unit getSrc() {
        return src;
    }

    public abstract @NonNull WeaponVisualType getWeaponVisualType();

    @Override
    protected @NonNull BoundingBox @Nullable [] getLocalBounds() {
        return null;
    }

    @Override
    public @NonNull String toString() {
        return "ThrowingWeapon: start_x = " + start_x + " | start_y = " + start_y + " | end_x = " + end_x
                + " | end_y = " + end_y + " | target = " + target + "  " + super.toString();
    }

    protected final void setTarget(@NonNull Selectable<?> target) {
        this.target = target;
        updateDirection();
        calcNumUpdatesAndZSpeed();
    }

    private void calcNumUpdatesAndZSpeed() {
        start_x = getPositionX();
        start_y = getPositionY();
        updateTarget();
        float dx = end_x - start_x;
        float dy = end_y - start_y;
        float len = (float) Math.hypot(dx, dy);
        time_limit = (len / getMetersPerSecond()) * getLoftFactor();
        time = 0;
        // current_z is already set to absolute start height
        float dest_z = getWorld().getHeightMap().getNearestHeight(end_x, end_y) + target.getHitOffsetZ();
        float dest_vec_z = dest_z - current_z;
        z_speed = (dest_vec_z) / time_limit - GRAVITY * time_limit / 2f;
    }

    protected abstract float getMetersPerSecond();

    protected abstract float getLoftFactor();

    private void updateTarget() {
        end_x = target.getPositionX();
        end_y = target.getPositionY();
    }

    private void updateDirection() {
        float dx = target.getPositionX() - getPositionX();
        float dy = target.getPositionY() - getPositionY();
        float len = (float) Math.max(Math.hypot(dx, dy), .01);
        float len_inv = 1f / len;
        dir_x = dx * len_inv;
        dir_y = dy * len_inv;
        setDirection(dir_x, dir_y);
    }

    @Override
    public final void updateChecksum(@NonNull StateChecksum checksum) {
        checksum.update(time);
    }

    @Override
    public final float getOffsetZ() {
        return deterministic_z;
    }

    @Override
    public void animate(float t) {
        if (time >= time_limit) {
            hitTarget(hit, getSrc().getOwner(), target);
            return;
        }

        if (hit) {
            updateTarget();
        }
        time += t;
        float progress = time / time_limit;

        float x;
        float y;
        if (progress < 1f) {
            x = start_x + (end_x - start_x) * progress;
            y = start_y + (end_y - start_y) * progress;
        } else {
            x = end_x;
            y = end_y;
        }

        current_z += z_speed * t;
        z_speed += GRAVITY * t;

        setPosition(x, y, current_z - deterministic_z);
    }

    protected void hitTarget(boolean hit, @NonNull Player owner, @NonNull Selectable<?> target) {
        getWorld().getAnimationManagerGameTime().removeAnimation(this);
        remove();
        if (hit)
            damageTarget(target);
    }

    protected final void damageTarget(@NonNull Selectable<?> target) {
        if (target instanceof Unit unit) {
            float pitchRange = unit.getTemplate().getDeathPitch();
            getClientState(ModelClient.class).ifPresent(client -> {
                client.onMeleeHit(target.getPositionX(), target.getPositionY(), target.getPositionZ(), pitchRange);
            });
        }
        target.hit(getDamage(), dir_x, dir_y, getSrc().getOwner());
    }

    protected abstract int getDamage();

    public final float getZSpeed() {
        return z_speed;
    }

    @Override
    public final float getNoDetailSize() {
        return NO_DETAIL_SIZE;
    }
}
