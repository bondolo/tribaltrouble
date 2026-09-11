package com.oddlabs.tt.simulation.model.weapon;

import com.oddlabs.tt.base.animation.Animated;
import com.oddlabs.tt.base.geom.BoundingBox;
import com.oddlabs.tt.simulation.model.Model;
import com.oddlabs.tt.simulation.model.Selectable;
import com.oddlabs.tt.simulation.model.Unit;
import com.oddlabs.tt.simulation.model.WeaponVisualType;
import com.oddlabs.tt.simulation.player.Player;
import com.oddlabs.tt.base.event.StateChecksum;
import org.jspecify.annotations.Nullable;

/**
 * Base {@link Model} class for all projectile weapons that are thrown through the world.
 */
public abstract sealed class ThrowingWeapon extends Model implements Animated permits RotatingThrowingWeapon,
        DirectedThrowingWeapon {
    private final boolean hit;
    private final Unit src;

    /** the target of the weapon. Mutable because rubber weapons bounce and change targets **/
    private Selectable<?> target;
    private float start_x;
    private float start_y;
    private final float start_z;
    private float end_x;
    private float end_y;
    private float dest_z;
    private float dir_x;
    private float dir_y;
    private float time_limit;
    private float time;

    public ThrowingWeapon(boolean hit, Unit src, Selectable<?> target, float offsetX, float offsetY, float offsetZ) {
        super(src.getOwner().getWorld());
        this.src = src;
        this.hit = hit;

        float x = src.getPositionX() + offsetX * src.getDirectionX() - offsetY * src.getDirectionY();
        float y = src.getPositionY() + offsetX * src.getDirectionY() + offsetY * src.getDirectionX();
        start_z = src.getPositionZ() + offsetZ;

        setPosition(x, y, start_z);

        setTarget(target);

        register();

        getWorld().getAnimationManagerGameTime().registerAnimation(this);

        // stats
        src.getOwner().weaponThrown();
        src.getOwner().getWorld().getNotificationListener().onWeaponThrow(x, y, start_z);
    }

    public final Unit getSrc() {
        return src;
    }

    public abstract WeaponVisualType getWeaponVisualType();

    @Override
    protected BoundingBox @Nullable [] getLocalBounds() {
        return null;
    }

    @Override
    public String toString() {
        return "ThrowingWeapon: start_x = " + start_x + " | start_y = " + start_y + " | end_x = " + end_x
                + " | end_y = " + end_y + " | target = " + target + "  " + super.toString();
    }

    protected final void setTarget(Selectable<?> target) {
        this.target = target;
        updateDirection();
        calcNumUpdates();
    }

    private void calcNumUpdates() {
        start_x = getPositionX();
        start_y = getPositionY();
        updateTarget();
        float dx = end_x - start_x;
        float dy = end_y - start_y;
        float len = (float) Math.hypot(dx, dy);
        time_limit = len / getMetersPerSecond();
        time = 0;
    }

    public abstract float getMetersPerSecond();

    private void updateTarget() {
        end_x = target.getPositionX();
        end_y = target.getPositionY();
        dest_z = getWorld().getHeightMap().getNearestHeight(end_x, end_y) + target.getHitOffsetZ();
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
    public final void updateChecksum(StateChecksum checksum) {
        checksum.update(time);
    }

    @Override
    public void animate(float dt) {
        if (time >= time_limit) {
            hitTarget(hit, getSrc().getOwner(), target);
            return;
        }

        if (hit) {
            updateTarget();
        }
        time += dt;
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

        float z = start_z + (dest_z - start_z) * Math.min(progress, 1f);
        setPosition(x, y, z);
    }

    protected void hitTarget(boolean hit, Player owner, Selectable<?> target) {
        getWorld().getAnimationManagerGameTime().removeAnimation(this);
        remove();
        if (hit)
            damageTarget(target);
    }

    protected final void damageTarget(Selectable<?> target) {
        if (target instanceof Unit unitTarget) {
            getSrc().getWorld().getNotificationListener().onUnitAttack(unitTarget.getTemplate().getVisualType(),
                    unitTarget.getOwner().getRaceInfo().getRaceType(), target.getPositionX(), target.getPositionY(),
                    target.getPositionZ());
        }
        target.hit(getDamage(), dir_x, dir_y, getSrc().getOwner());
    }

    protected abstract int getDamage();

    public final float getStartZ() {
        return start_z;
    }

    public final float getDestZ() {
        return dest_z;
    }

    public final float getTime() {
        return time;
    }

    public final float getTimeLimit() {
        return time_limit;
    }
}
