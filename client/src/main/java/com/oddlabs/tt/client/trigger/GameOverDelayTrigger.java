package com.oddlabs.tt.client.trigger;

import com.oddlabs.tt.base.animation.TimerAnimation;
import com.oddlabs.tt.base.animation.Updatable;
import com.oddlabs.tt.client.camera.Camera;
import com.oddlabs.tt.client.delegate.GameStatsDelegate;
import com.oddlabs.tt.client.viewer.WorldViewer;

/**
 * Triggers the game statistics screen after a brief delay following game over.
 */
public final class GameOverDelayTrigger implements Updatable<TimerAnimation> {

    private final WorldViewer viewer;
    private final Camera camera;
    private final String label_str;

    public GameOverDelayTrigger(WorldViewer viewer, Camera camera, String label_str) {
        this.viewer = viewer;
        this.camera = camera;
        this.label_str = label_str;
        var delay_timer = new TimerAnimation(viewer.getWorld().getAnimationManagerRealTime(), this, 1.5f);
        delay_timer.start();
    }

    @Override
    public void update(TimerAnimation anim) {
        anim.stop();
        viewer.getGUIRoot().pushDelegate(new GameStatsDelegate(viewer, camera, label_str));
    }
}
