package com.oddlabs.tt.client.camera;

import com.oddlabs.tt.client.render.Picker;
import com.oddlabs.tt.gui.GUIRoot;
import org.joml.Vector2fc;
import org.jspecify.annotations.Nullable;

/**
 * A specialized camera that performs a transition jump from one camera
 * state to another, typically used for cinematic or focal transitions.
 */
public final class JumpCamera extends Camera {
    private static final float DEFAULT_METERS_PER_SECOND = 300f;
    private static final float DEFAULT_MAX_SECONDS = .5f;

    private final GUIRoot gui_root;
    private @Nullable Runnable onComplete;

    private final float dst_x;
    private final float dst_y;
    private final float dst_z;
    private final float dx;
    private final float dy;
    private final float dz;
    private final float factor;
    private final float z_accel;
    private float seconds;
    private float z_speed;
    private float temp_z;

    public JumpCamera(GUIRoot gui_root, Picker picker, GameCamera old_camera, float x, float y) {
        this(gui_root, picker, old_camera, x, y, DEFAULT_METERS_PER_SECOND, DEFAULT_MAX_SECONDS);
    }

    public JumpCamera(GUIRoot gui_root, Picker picker, GameCamera old_camera, float x, float y,
            float meters_per_second, float max_seconds) {
        super(old_camera.getLandscapeEnvironment(), old_camera.getState(), old_camera.getAnimationManager());
        this.gui_root = gui_root;
        picker.pickRotate(old_camera);
        Vector2fc target = old_camera.getRotationPoint();
        float target_z = getLandscapeEnvironment().getHeight(target.x(), target.y());
        float dx_to_landscape = target.x() - getState().getTargetX();
        float dy_to_landscape = target.y() - getState().getTargetY();
        float dz_to_landscape = target_z - getState().getTargetZ();
        float distance_to_landscape = (float) Math.sqrt(dx_to_landscape * dx_to_landscape + dy_to_landscape
                * dy_to_landscape + dz_to_landscape * dz_to_landscape);

        float dir_x = dx_to_landscape / distance_to_landscape;
        float dir_y = dy_to_landscape / distance_to_landscape;
        float dir_z = dz_to_landscape / distance_to_landscape;

        dst_x = (int) x - dir_x * distance_to_landscape;
        dst_y = (int) y - dir_y * distance_to_landscape;
        dst_z = getLandscapeEnvironment().getHeight((int) x, (int) y) - dir_z * distance_to_landscape;
        this.dx = dst_x - getState().getTargetX();
        this.dy = dst_y - getState().getTargetY();
        this.dz = dst_z - getState().getTargetZ();
        float distance_to_dst = (float) Math.sqrt(this.dx * this.dx + this.dy * this.dy + this.dz * this.dz);
        seconds = Math.min(distance_to_dst / meters_per_second, max_seconds);
        factor = 1f / seconds;
        z_accel = -(distance_to_dst / 32) / (seconds * .5f);
        z_speed = this.dz * factor - z_accel * (seconds / 2f);

        temp_z = getState().getTargetZ();
    }

    public void setOnComplete(@Nullable Runnable onComplete) {
        this.onComplete = onComplete;
    }

    @Override
    public void doAnimate(float dt) {
        if (seconds <= 0f) {
            getState().setTargetX(dst_x);
            getState().setTargetY(dst_y);
            getState().setTargetZ(dst_z);
            if (onComplete != null) {
                onComplete.run();
            }
            return;
        }
        seconds -= dt;
        getState().setTargetX(getState().getTargetX() + dx * factor * dt);
        getState().setTargetY(getState().getTargetY() + dy * factor * dt);
        temp_z += z_speed * dt;
        getState().setTargetZ(temp_z);
        z_speed += z_accel * dt;
        bounce(getState().getTargetX(), getState().getTargetY(), getState().getTargetZ(), gui_root.getWidth(),
                gui_root.getHeight());
    }
}
