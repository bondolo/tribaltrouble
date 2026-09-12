package com.oddlabs.tt.content.tutorial.trigger;

import com.oddlabs.tt.content.tutorial.Tutorial;

import com.oddlabs.tt.client.delegate.FirstPersonDelegate;

/** Tutorial trigger waiting for the player to enter first-person camera mode. */
final class FirstPersonCameraTrigger extends TutorialTrigger {
    FirstPersonCameraTrigger() {
        super(.1f, 2f, "fpc");
    }

    @Override
    public void run(Tutorial tutorial) {
        if (tutorial.getViewer().getGUIRoot().getDelegate() instanceof FirstPersonDelegate) {
            tutorial.next(new MapModeTrigger());
        }
    }
}
