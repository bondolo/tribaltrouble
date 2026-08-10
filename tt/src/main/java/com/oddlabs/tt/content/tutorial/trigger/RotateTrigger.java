package com.oddlabs.tt.content.tutorial.trigger;

import com.oddlabs.tt.content.tutorial.Tutorial;

import com.oddlabs.tt.client.camera.GameCamera;
import org.jspecify.annotations.NonNull;

public final class RotateTrigger extends TutorialTrigger {
    private final boolean[] rotate_dirs = new boolean[2];

    public RotateTrigger() {
        super(.1f, 2f, "rotate");
    }

    @Override
    public void run(@NonNull Tutorial tutorial) {
        GameCamera camera = tutorial.getViewer().getCamera();
        if (camera.rotateRight()) {
            rotate_dirs[0] = true;
        }
        if (camera.rotateLeft()) {
            rotate_dirs[1] = true;
        }
        for (boolean rotateDir : rotate_dirs) {
            if (!rotateDir)
                return;
        }
        tutorial.next(new PitchTrigger());
    }
}
