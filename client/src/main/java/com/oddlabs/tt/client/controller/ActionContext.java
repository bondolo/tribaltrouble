package com.oddlabs.tt.client.controller;

import com.oddlabs.tt.client.camera.GameCamera;
import com.oddlabs.tt.client.delegate.CameraDelegate;
import com.oddlabs.tt.client.viewer.WorldViewer;
import com.oddlabs.tt.simulation.model.SupplyType;

/**
 * Context provided to action controllers to dispatch player delegates and trigger view updates.
 */
public interface ActionContext {
    WorldViewer getViewer();

    GameCamera getCamera();

    void pushDelegate(CameraDelegate<?> delegate);

    void adjustQuartersPeon(boolean pressed, boolean decrement, boolean batch);

    void adjustArmorySupply(SubmenuType type, SupplyType supply, boolean pressed, boolean decrement,
            boolean batch);

    void adjustArmoryPeon(boolean pressed, boolean decrement, boolean batch);

    void markNeedsUpdate();
}
