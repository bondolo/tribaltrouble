package com.oddlabs.tt.simulation.tutorial.trigger;

import com.oddlabs.tt.simulation.tutorial.Tutorial;

import com.oddlabs.tt.camera.MapCamera;
import org.jspecify.annotations.NonNull;

public final class MapModeTrigger extends TutorialTrigger {
    public MapModeTrigger() {
        super(.1f, 1f, "map_mode");
    }

    @Override
    public void run(@NonNull Tutorial tutorial) {
        if (tutorial.getViewer().getDelegate().getCamera() instanceof MapCamera)
            tutorial.next(new FromMapModeTrigger());
    }
}
