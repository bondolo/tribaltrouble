package com.oddlabs.tt.content.tutorial.trigger;

import com.oddlabs.tt.content.tutorial.Tutorial;

import com.oddlabs.tt.client.camera.GameCamera;
import com.oddlabs.tt.client.viewer.WorldViewer;

/** Tutorial trigger waiting for the player to zoom the camera in and out. */
final class ZoomTrigger extends TutorialTrigger {
    private final boolean[] zoom_dirs = new boolean[2];

    ZoomTrigger(WorldViewer viewer) {
        super(0f, 2f, "zoom");
        viewer.getCamera().resetLastZoomFactor();
    }

    @Override
    public void run(Tutorial tutorial) {
        GameCamera camera = tutorial.getViewer().getCamera();
        if (camera.getLastZoomFactor() > 0f) {
            zoom_dirs[0] = true;
        } else if (camera.getLastZoomFactor() < 0f) {
            zoom_dirs[1] = true;
        }
        for (boolean zoomDir : zoom_dirs) {
            if (!zoomDir)
                return;
        }
        tutorial.next(new RotateTrigger());
    }
}
