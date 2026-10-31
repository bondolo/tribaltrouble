package com.oddlabs.tt.simulation.model;

import com.oddlabs.tt.base.geom.BoundingBox;
import com.oddlabs.util.LinkedList;
import com.oddlabs.util.ListElement;
import org.jspecify.annotations.Nullable;

/**
 * Base class for all world entities that exist within the quadtree.
 */
public abstract class Element<T extends Element<T>> extends BoundingBox implements ListElement<T> {
    private final AbstractElementNode<T> element_root;
    private @Nullable AbstractElementNode<T> node_parent;

    private @Nullable LinkedList<T> parent;
    private @Nullable T next = null;
    private @Nullable T prior = null;

    private static final float MAX_INTERPOLATION_DELTA = 5f;

    private float x;
    private float y;
    private float render_pos_z;

    private float prev_x;
    private float prev_y;
    private float prev_z;

    private float dir_x = 1f;
    private float dir_y = 0f;

    private float prev_dir_x = 1f;
    private float prev_dir_y = 0f;

    protected Element(AbstractElementNode<T> element_root) {
        this.element_root = element_root;
    }

    protected abstract T self();

    public void register() {
        node_parent = element_root.insertElement(self());
        assert node_parent != null;
    }

    protected final void reregister() {
        node_parent = element_root.reinsertElement(self());
        assert node_parent != null;
    }

    public final boolean isRegistered() {
        return node_parent != null;
    }

    public void remove() {
        if (node_parent != null) {
            node_parent.removeElement(self());
            node_parent = null;
        }
    }

    public final float getDirectionX() {
        return dir_x;
    }

    public final float getDirectionY() {
        return dir_y;
    }

    public final void setDirection(float dir_x, float dir_y) {
        this.dir_x = dir_x;
        this.dir_y = dir_y;
    }

    public void setPosition(float x, float y) {
//		assert World.getHeightMap().isInside(x, y): x + " " + y;
        this.x = x;
        this.y = y;
    }

    public void setPositionZ(float z) {
        render_pos_z = z;
    }

    public final float getPositionX() {
        return x;
    }

    public final float getPositionY() {
        return y;
    }

    public final float getPositionZ() {
        return render_pos_z;
    }

    /**
     * Snapshots current position and direction as the previous tick transform baseline.
     */
    public final void capturePreviousTransform() {
        prev_x = x;
        prev_y = y;
        prev_z = render_pos_z;
        prev_dir_x = dir_x;
        prev_dir_y = dir_y;
    }

    /// Interpolates a scalar coordinate between the previous tick baseline and current position.
    protected static float interpolatePosition(float current, float prev, float alpha) {
        if (alpha >= 1f) {
            return current;
        }
        float delta = current - prev;
        return Math.abs(delta) > MAX_INTERPOLATION_DELTA ? current : prev + alpha * delta;
    }

    public final float getInterpolatedPositionX(float alpha) {
        return interpolatePosition(x, prev_x, alpha);
    }

    public final float getInterpolatedPositionY(float alpha) {
        return interpolatePosition(y, prev_y, alpha);
    }

    public final float getInterpolatedPositionZ(float alpha) {
        return interpolatePosition(render_pos_z, prev_z, alpha);
    }

    public final float getInterpolatedAngle(float alpha) {
        float currAngle = (float) Math.atan2(dir_y, dir_x);
        if (alpha >= 1f) {
            return currAngle;
        }
        float prevAngle = (float) Math.atan2(prev_dir_y, prev_dir_x);
        float diff = (currAngle - prevAngle) % ((float) (2 * Math.PI));
        if (diff > Math.PI) {
            diff -= (float) (2 * Math.PI);
        } else if (diff < -Math.PI) {
            diff += (float) (2 * Math.PI);
        }
        return prevAngle + alpha * diff;
    }

    /**
     * Returns true if the element is finished and can be removed from the world.
     *
     * @return true if the element is finished.
     */
    public boolean isFinished() {
        return false;
    }

    @Override
    public final void setListOwner(@Nullable LinkedList<T> parent) {
        this.parent = parent;
    }

    @Override
    public final @Nullable LinkedList<T> getListOwner() {
        return parent;
    }

    @Override
    public final void setPrior(@Nullable T prior) {
        this.prior = prior;
    }

    @Override
    public final void setNext(@Nullable T next) {
        this.next = next;
    }

    @Override
    public final @Nullable T getPrior() {
        return prior;
    }

    @Override
    public final @Nullable T getNext() {
        return next;
    }
}
