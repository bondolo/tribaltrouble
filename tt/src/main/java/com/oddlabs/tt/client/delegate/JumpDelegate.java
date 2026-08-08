package com.oddlabs.tt.client.delegate;

import com.oddlabs.tt.client.camera.GameCamera;
import com.oddlabs.tt.client.camera.JumpCamera;
import com.oddlabs.tt.client.input.InputEvent;
import com.oddlabs.tt.client.viewer.WorldViewer;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class JumpDelegate extends InGameDelegate<JumpCamera> {
    private final @Nullable Runnable runnable;

    public JumpDelegate(@NonNull WorldViewer viewer, @NonNull GameCamera old_camera, float x, float y) {
        super(viewer, null);
        setCamera(new JumpCamera(this, old_camera, x, y));
        runnable = null;
    }

    public JumpDelegate(@NonNull WorldViewer viewer, @NonNull GameCamera old_camera, float x, float y,
            float meters_per_second, float max_seconds) {
        this(viewer, old_camera, x, y, meters_per_second, max_seconds, null);
    }

    public JumpDelegate(@NonNull WorldViewer viewer, @NonNull GameCamera old_camera, float x, float y,
            float meters_per_second, float max_seconds, @Nullable Runnable runnable) {
        super(viewer, null);
        setCamera(new JumpCamera(this, old_camera, x, y, meters_per_second, max_seconds));
        this.runnable = runnable;
    }

    @Override
    public void handleInput(@NonNull InputEvent event) {
        event.consume();
    }

    @Override
    public void mouseScrolled(int amount) {
    }

    @Override
    public void doRemove() {
        super.doRemove();
        if (runnable != null)
            runnable.run();
    }
}
