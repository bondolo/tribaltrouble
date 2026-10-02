package com.oddlabs.tt.engine.render;


import com.oddlabs.tt.base.event.StateChecksum;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Evaluates periodic wind sway rotation for vegetation.
 */
public final class WaveAnimation {
    private final float waveScale;
    private final Vector3f wave_dir = new Vector3f(0, 0, 1);
    private final Vector3f up_vec = new Vector3f(0, 0, 1);
    private final Vector3f rot_axis = new Vector3f(0, 0, 1);

    public WaveAnimation(float waveScale) {
        this.waveScale = waveScale;
    }

    private float x;
    private float y;
    private float rot_angle = 0;
    private int time = 0;

    public void mulRotation(Matrix4f matrix) {
        matrix.rotate(rot_angle, rot_axis.x, rot_axis.y, rot_axis.z);
    }

    public void updateChecksum(StateChecksum checksum) {
        checksum.update(rot_angle);
    }

    public void setTime(float t) {
        time = (int) (t * 1000);
        initWaveDir();
        computeRotation();
    }

    private void initWaveDir() {
        x = waveScale * 0.5f * (float) Math.cos(time * 0.001f);
        y = waveScale * (float) Math.sin(time * 0.001f);
        wave_dir.set(x, y, 1);
        wave_dir.normalize();
    }

    private void computeRotation() {
        wave_dir.cross(up_vec, rot_axis);
        float length = rot_axis.length();
        if (length > 1e-6f) {
            rot_angle = (float) Math.asin(length);
            float inv_length = 1f / length;
            rot_axis.mul(inv_length);
        } else {
            rot_angle = 0;
            rot_axis.set(0, 0, 1);
        }
    }
}
