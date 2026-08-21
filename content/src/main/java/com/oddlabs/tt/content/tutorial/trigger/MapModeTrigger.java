package com.oddlabs.tt.content.tutorial.trigger;

import com.oddlabs.tt.content.tutorial.Tutorial;

import com.oddlabs.tt.client.camera.MapCamera;

public final class MapModeTrigger extends TutorialTrigger {
    public MapModeTrigger() {
        super(.1f, 1f, "map_mode");
    }

    @Override
    public void run(Tutorial tutorial) {
        if (tutorial.getViewer().getDelegate().getCamera() instanceof MapCamera)
            tutorial.next(new FromMapModeTrigger());
    }
}
