package com.oddlabs.tt.content.tutorial.trigger;

import com.oddlabs.tt.content.tutorial.Tutorial;

import com.oddlabs.tt.simulation.model.BuildingType;

import com.oddlabs.tt.client.camera.GameCamera;
import com.oddlabs.tt.simulation.player.Player;

public final class ScrollTrigger extends TutorialTrigger {
    private final boolean[] scroll_dirs = new boolean[4];

    public ScrollTrigger(Player player) {
        super(.1f, 2f, "scroll");
        player.enableMoving(false);
        player.enableRepairing(false);
        player.enableAttacking(false);
        player.enableBuilding(BuildingType.QUARTERS, false);
        player.enableBuilding(BuildingType.ARMORY, false);
        player.enableBuilding(BuildingType.TOWER, false);
        player.enableChieftains(false);
    }

    @Override
    public void run(Tutorial tutorial) {
        GameCamera camera = tutorial.getViewer().getCamera();
        if (camera.getScrollX() > 0) {
            scroll_dirs[0] = true;
        } else if (camera.getScrollX() < 0) {
            scroll_dirs[1] = true;
        }
        if (camera.getScrollY() > 0) {
            scroll_dirs[2] = true;
        } else if (camera.getScrollY() < 0) {
            scroll_dirs[3] = true;
        }
        for (boolean scrollDir : scroll_dirs) {
            if (!scrollDir)
                return;
        }
        tutorial.next(new ZoomTrigger(tutorial.getViewer()));
    }
}
