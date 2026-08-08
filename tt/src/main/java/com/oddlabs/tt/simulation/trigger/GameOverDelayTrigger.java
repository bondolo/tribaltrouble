package com.oddlabs.tt.simulation.trigger;

import com.oddlabs.tt.core.animation.TimerAnimation;
import com.oddlabs.tt.core.animation.Updatable;
import com.oddlabs.tt.client.camera.Camera;
import com.oddlabs.tt.delegate.GameStatsDelegate;
import com.oddlabs.tt.viewer.WorldViewer;
import org.jspecify.annotations.NonNull;

public final class GameOverDelayTrigger implements Updatable<TimerAnimation> {

    private final @NonNull WorldViewer viewer;
    private final @NonNull Camera camera;
    private final @NonNull String label_str;

    public GameOverDelayTrigger(@NonNull WorldViewer viewer, @NonNull Camera camera, @NonNull String label_str) {
        this.viewer = viewer;
        this.camera = camera;
        this.label_str = label_str;
        var delay_timer = new TimerAnimation(viewer.getWorld().getAnimationManagerRealTime(), this, 1.5f);
        delay_timer.start();
    }

    @Override
    public void update(@NonNull TimerAnimation anim) {
        anim.stop();
        viewer.getGUIRoot().pushDelegate(new GameStatsDelegate(viewer, camera, label_str));
    }
}
