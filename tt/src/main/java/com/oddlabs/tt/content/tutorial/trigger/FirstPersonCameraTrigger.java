package com.oddlabs.tt.content.tutorial.trigger;

import com.oddlabs.tt.content.tutorial.Tutorial;

import com.oddlabs.tt.client.delegate.FirstPersonDelegate;
import org.jspecify.annotations.NonNull;

public final class FirstPersonCameraTrigger extends TutorialTrigger {
    public FirstPersonCameraTrigger() {
        super(.1f, 2f, "fpc");
    }

    @Override
    public void run(@NonNull Tutorial tutorial) {
        if (tutorial.getViewer().getGUIRoot().getDelegate() instanceof FirstPersonDelegate) {
            tutorial.next(new MapModeTrigger());
        }
    }
}
