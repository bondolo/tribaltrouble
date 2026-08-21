package com.oddlabs.tt.client.delegate;

import com.oddlabs.tt.client.camera.GameCamera;
import com.oddlabs.tt.gui.MouseButton;
import com.oddlabs.tt.simulation.model.Action;
import com.oddlabs.tt.simulation.model.Building;
import com.oddlabs.tt.client.viewer.WorldViewer;

public final class RallyPointDelegate extends TargetDelegate {
    private final Building building;

    public RallyPointDelegate(WorldViewer viewer, GameCamera camera, Building building) {
        super(viewer, camera, Action.DEFAULT);
        this.building = building;
    }

    @Override
    public void mousePressed(MouseButton button, int x, int y) {
        if (building.isDead()) {
            pop();
            return;
        }
        getViewer().getPicker().pickRallyPoint(getCamera().getState(), x, y, building).ifPresent(target -> {
            if (building.isValidRallyPoint(target)) {
                getViewer().getPeerHub().getPlayerInterface().setRallyPoint(building, target);
            } else {
                getViewer().getPeerHub().getPlayerInterface().setRallyPoint(building, target.getGridX(), target
                        .getGridY());
            }
            pop();
        });
    }
}
