package com.oddlabs.tt.content.form;

import com.oddlabs.tt.client.viewer.WorldViewer;
import com.oddlabs.tt.simulation.landscape.World;

/**
 * In-game options menu allowing adjustment of game speed, sound, graphics, and controls during active gameplay.
 */
public final class InGameOptionsMenu extends AbstractOptionsMenu {
    private final WorldViewer viewer;

    public InGameOptionsMenu(WorldViewer viewer) {
        super(viewer.getGUIRoot(), viewer.getEngine());
        this.viewer = viewer;
        chooseGamespeed(getGamespeed());
    }

    private int getGamespeed() {
        int gamespeed = viewer.getLocalPlayer().getGamespeed();
        if (!World.isValidGamespeed(gamespeed)) {
            gamespeed = viewer.getWorld().getGamespeed();
        }
        return gamespeed;
    }

    @Override
    protected void changeGamespeed(int index) {
        super.changeGamespeed(index);
        viewer.getPeerHub().getPlayerInterface().setPreferredGamespeed(index);
    }
}
