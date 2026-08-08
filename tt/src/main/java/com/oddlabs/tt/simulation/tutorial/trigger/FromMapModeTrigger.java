package com.oddlabs.tt.simulation.tutorial.trigger;

import com.oddlabs.tt.simulation.tutorial.Tutorial;

import com.oddlabs.tt.client.camera.GameCamera;
import org.jspecify.annotations.NonNull;

public final class FromMapModeTrigger extends TutorialTrigger {
    public FromMapModeTrigger() {
        super(.1f, 1f, "from_map_mode");
    }

    @Override
    public void run(@NonNull Tutorial tutorial) {
        if (tutorial.getViewer().getDelegate().getCamera() instanceof GameCamera)
            tutorial.next(new SelectUnitTrigger());
    }
}
