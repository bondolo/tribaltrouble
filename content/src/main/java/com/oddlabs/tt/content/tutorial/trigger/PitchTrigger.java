package com.oddlabs.tt.content.tutorial.trigger;

import com.oddlabs.tt.content.tutorial.Tutorial;

import com.oddlabs.tt.client.camera.GameCamera;

public final class PitchTrigger extends TutorialTrigger {
    private final boolean[] pitch_dirs = new boolean[2];

    public PitchTrigger() {
        super(.1f, 2f, "pitch");
    }

    @Override
    public void run(Tutorial tutorial) {
        GameCamera camera = tutorial.getViewer().getCamera();
        if (camera.pitchUp()) {
            pitch_dirs[0] = true;
        }
        if (camera.pitchDown()) {
            pitch_dirs[1] = true;
        }
        for (boolean pitchDir : pitch_dirs) {
            if (!pitchDir)
                return;
        }
        tutorial.next(new FirstPersonCameraTrigger());
    }
}
