package com.oddlabs.tt.client.delegate;

import com.oddlabs.tt.client.camera.GameCamera;
import com.oddlabs.tt.gui.MouseButton;
import com.oddlabs.tt.simulation.model.Action;
import com.oddlabs.tt.client.viewer.WorldViewer;

/**
 * Handles the placement of a beacon in the world.
 */
public final class BeaconDelegate extends TargetDelegate {
    public BeaconDelegate(WorldViewer viewer, GameCamera camera) {
        super(viewer, camera, Action.DEFAULT);
    }

    @Override
    public void mousePressed(MouseButton button, int x, int y) {
        getViewer().getPicker().pickLocation(getCamera().getState()).ifPresent(landscape_hit -> {
            getViewer().getPeerHub().sendBeacon(landscape_hit.x(), landscape_hit.y());
        });
        pop();
    }
}
