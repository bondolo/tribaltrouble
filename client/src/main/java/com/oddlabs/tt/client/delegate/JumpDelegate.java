package com.oddlabs.tt.client.delegate;

import com.oddlabs.tt.client.camera.GameCamera;
import com.oddlabs.tt.client.camera.JumpCamera;
import com.oddlabs.tt.input.InputEvent;
import com.oddlabs.tt.client.viewer.WorldViewer;
import org.jspecify.annotations.Nullable;

/**
 * Input delegate that controls the camera during automated jump sequences and executes a callback upon completion.
 */
public final class JumpDelegate extends InGameDelegate<JumpCamera> {
    private final @Nullable Runnable runnable;

    public JumpDelegate(WorldViewer viewer, GameCamera old_camera, float x, float y) {
        this(viewer, old_camera, x, y, null);
    }

    public JumpDelegate(WorldViewer viewer, GameCamera old_camera, float x, float y,
            @Nullable Runnable runnable) {
        this(viewer, new JumpCamera(viewer.getGUIRoot(), viewer.getPicker(), old_camera, x, y), runnable);
    }

    public JumpDelegate(WorldViewer viewer, GameCamera old_camera, float x, float y,
            float meters_per_second, float max_seconds) {
        this(viewer, old_camera, x, y, meters_per_second, max_seconds, null);
    }

    public JumpDelegate(WorldViewer viewer, GameCamera old_camera, float x, float y,
            float meters_per_second, float max_seconds, @Nullable Runnable runnable) {
        this(viewer, new JumpCamera(viewer.getGUIRoot(), viewer.getPicker(), old_camera, x, y, meters_per_second,
                max_seconds), runnable);
    }

    private JumpDelegate(WorldViewer viewer, JumpCamera camera, @Nullable Runnable runnable) {
        super(viewer, camera);
        camera.setOnComplete(this::pop);
        this.runnable = runnable;
    }

    @Override
    public void handleInput(InputEvent event) {
        event.consume();
    }

    @Override
    public void mouseScrolled(int amount) {
    }

    @Override
    public void doRemove() {
        super.doRemove();
        if (runnable != null) {
            runnable.run();
        }
    }
}
