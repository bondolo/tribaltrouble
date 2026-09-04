package com.oddlabs.tt.client.camera;

import com.oddlabs.tt.client.viewer.WorldViewer;
import com.oddlabs.tt.engine.render.CameraState;
import com.oddlabs.tt.input.GameAction;
import com.oddlabs.tt.simulation.landscape.LandscapeEnvironment;

/**
 * First-person perspective camera controller.
 */
public final class FirstPersonCamera extends Camera {
    private static final float SCALE_HORIZ = .002f;
    private static final float SCALE_VERT = .002f;

    private final int last_x;
    private final int last_y;
    private final WorldViewer viewer;

    public FirstPersonCamera(WorldViewer viewer, LandscapeEnvironment heightmap, CameraState camera) {
        super(heightmap, camera, viewer.getAnimationManagerHighPrecision());
        this.viewer = viewer;
        var guiRoot = viewer.getGUIRoot();
        this.last_x = guiRoot.getMouseX();
        this.last_y = guiRoot.getMouseY();
    }

    @Override
    public void doAnimate(float t) {
        float dir_x = (float) Math.cos(getState().getTargetHorizAngle());
        float dir_y = (float) Math.sin(getState().getTargetHorizAngle());
        float left_dir_x = -dir_y;
        float left_dir_y = dir_x;

        float scrolling_x = 0;
        float scrolling_y = 0;
        var inputManager = viewer.getInputManager();
        if (inputManager.isActive(GameAction.CAMERA_PAN_LEFT) && !inputManager.isActive(GameAction.CAMERA_PAN_RIGHT))
            scrolling_x = -1f;
        else if (inputManager.isActive(GameAction.CAMERA_PAN_RIGHT) && !inputManager.isActive(
                GameAction.CAMERA_PAN_LEFT))
            scrolling_x = 1f;

        if (inputManager.isActive(GameAction.CAMERA_PAN_DOWN) && !inputManager.isActive(GameAction.CAMERA_PAN_UP))
            scrolling_y = -1f;
        else if (inputManager.isActive(GameAction.CAMERA_PAN_UP) && !inputManager.isActive(GameAction.CAMERA_PAN_DOWN))
            scrolling_y = 1f;

        float scroll_factor = getState().getTargetZ() * t;
        float new_x = getState().getTargetX() - (scrolling_x * left_dir_x + scrolling_y * -left_dir_y) * scroll_factor;
        float new_y = getState().getTargetY() - (scrolling_x * left_dir_y + scrolling_y * left_dir_x) * scroll_factor;

        if (new_x != getState().getTargetX() || new_y != getState().getTargetY()) {
            getState().setTargetX(new_x);
            getState().setTargetY(new_y);
            checkPosition();
        }
    }

    @Override
    public void mouseMoved(int x, int y) {
        // Ignore logical x/y; use physical coordinates from GUIRoot to maintain constant
        // rotation sensitivity and match PointerInput locking requirements.
        var guiRoot = viewer.getGUIRoot();
        int dx = guiRoot.getMouseX() - last_x;
        int dy = guiRoot.getMouseY() - last_y;
        getState().setTargetHorizAngle(getState().getTargetHorizAngle() - dx * SCALE_HORIZ);
        if (CameraSettings.from(viewer.getEngine().getSettings()).invert_camera_pitch)
            getState().setTargetVertAngle(getState().getTargetVertAngle() - dy * SCALE_VERT);
        else
            getState().setTargetVertAngle(getState().getTargetVertAngle() + dy * SCALE_VERT);

        guiRoot.setCursorPosition(last_x, last_y);
    }
}
