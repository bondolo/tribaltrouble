package com.oddlabs.tt.client.camera;

import com.oddlabs.tt.engine.render.CameraState;

import com.oddlabs.tt.client.delegate.SelectionDelegate;
import com.oddlabs.tt.gui.Label;
import com.oddlabs.tt.input.GameAction;
import com.oddlabs.tt.input.InputEvent;
import com.oddlabs.tt.input.InputPhase;
import com.oddlabs.tt.engine.render.state.FogInfo;
import com.oddlabs.tt.engine.render.state.RadialFogInfo;
import com.oddlabs.tt.base.util.Utils;
import com.oddlabs.tt.simulation.landscape.LandscapeTarget;
import com.oddlabs.tt.simulation.pathfinder.UnitGrid;
import com.oddlabs.util.Color;
import org.joml.Vector2fc;

import java.util.EnumSet;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * Camera for showing the island overview map.
 */
public final class MapCamera extends Camera {
    private static final float MAP_THRESHOLD = .1f;
    private static final float MAP_TIME_FACTOR = 1000f;
    private static final float SMOOTHNESS_FACTOR = 200f;

    private static final Set<GameAction> BLOCKED_ACTIONS = EnumSet.of(
            GameAction.CAMERA_PAN_LEFT,
            GameAction.CAMERA_PAN_RIGHT,
            GameAction.CAMERA_PAN_UP,
            GameAction.CAMERA_PAN_DOWN,
            GameAction.CAMERA_PITCH_UP,
            GameAction.CAMERA_PITCH_DOWN,
            GameAction.CAMERA_ROTATE_LEFT,
            GameAction.CAMERA_ROTATE_RIGHT,
            GameAction.CAMERA_ZOOM_IN,
            GameAction.CAMERA_ZOOM_OUT,
            GameAction.CAMERA_RESET,
            GameAction.CAMERA_FIRST_PERSON,
            GameAction.CAMERA_ZOOM_MODE
    );

    private enum MapMode {
        TO_MAP,
        IN_MAP,
        FROM_MAP
    }

    private final SelectionDelegate delegate;
    private final GameCamera game_camera;
    private final CameraState original_camera_state;
    private final float vertical_clearance;
    private final float horizontal_distance;
    private final Label label;

    private MapMode map_mode = MapMode.TO_MAP;
    private float fogTime = 0f;
    private boolean has_goto_target = false;
    private float goto_x = 0f;
    private float goto_y = 0f;

    public MapCamera(SelectionDelegate delegate, GameCamera old_camera) {
        this.game_camera = old_camera;
        original_camera_state = old_camera.getState();
        FogInfo radialFog = new RadialFogInfo(Color.Linear.WHITE, 0.25f);
        CameraState mapCameraState = new CameraState(radialFog);
        mapCameraState.set(old_camera.getState());
        mapCameraState.setFog(radialFog);
        mapCameraState.setNoDetailMode(true);
        super(old_camera.getLandscapeEnvironment(), mapCameraState, old_camera.getAnimationManager());
        this.delegate = delegate;
        this.label = new Label(Utils.getBundleString(ResourceBundle.getBundle(MapCamera.class.getName()),
                "map_mode"), delegate.getGUIRoot().getSkin().getHeadlineFont());

        float cam_x0 = original_camera_state.getTargetX();
        float cam_y0 = original_camera_state.getTargetY();
        float cam_z0 = original_camera_state.getTargetZ();
        float ground_z0 = Math.max(getLandscapeEnvironment().getHeight(cam_x0, cam_y0),
                getLandscapeEnvironment().getSeaLevelMeters());
        float clearance = cam_z0 - ground_z0;
        vertical_clearance = Math.clamp(clearance, 20f, GameCamera.MAX_Z);

        float vert_angle = original_camera_state.getTargetVertAngle();
        float tanAlpha = (float) Math.tan(Math.max(-vert_angle, (float) Math.toRadians(20.0)));
        float max_h = Math.min(getLandscapeEnvironment().getMetersPerWorld() / 4f, 150f);
        horizontal_distance = Math.clamp(vertical_clearance / tanAlpha, 15f, max_h);

        setSmoothnessFactor(SMOOTHNESS_FACTOR);
    }

    @Override
    public void doAnimate(float dt) {
        float mapmodeDelay = CameraSettings.from(delegate.getGUIRoot().getGUI().getSettings()).mapmode_delay;
        float factor = dt * 1000f / Math.max(
                dt * 1000f, mapmodeDelay
                        * MAP_TIME_FACTOR);
        float dx;
        float dy;
        float dz;
        float da;
        float meters_per_world = getLandscapeEnvironment().getMetersPerWorld();
        float map_x = meters_per_world / 2f;
        float map_y = meters_per_world / 2f;

        // Calculate altitude based on aspect ratio and FOV to ensure the whole island fits.
        // We use the diagonal of the world as the bounding sphere radius.
        float aspect = (float) delegate.getGUIRoot().getWidth() / delegate.getGUIRoot().getHeight();
        float fovy = Camera.calculateDynamicFOV(0, aspect, FOVMode.DIAGONAL); // Height is independent of target Z for DIAGONAL mode
        float fovx = 2.0f * (float) Math.atan(Math.tan(Math.toRadians(fovy) * 0.5f) * aspect);

        // Required distance to fit the island width and height
        float dist_h = (meters_per_world * 0.5f) / (float) Math.tan(fovx * 0.5f);
        float dist_v = (meters_per_world * 0.5f) / (float) Math.tan(Math.toRadians(fovy) * 0.5f);

        // Add 10% margin to prevent tight clipping at edges
        float map_z = Math.max(dist_h, dist_v) * 1.1f;

        float start_z = original_camera_state.getTargetZ();

        // Calculate transition progress (0.0 = at start, 1.0 = at map)
        float current_z = getState().getTargetZ();
        float total_dist = map_z - start_z;
        float progress = (Math.abs(total_dist) > 0.001f)
                ? Math.clamp((current_z - start_z) / total_dist, 0f, 1f)
                : 1f;

        // Base organic fog pulse (sum of sines for non-predictable period)
        fogTime += dt;
        float pulse = (float) (Math.sin(fogTime * 0.4125f) * 0.5 +
                Math.sin(fogTime * 0.8625f) * 0.3 +
                Math.sin(fogTime * 1.7625f) * 0.2);
        float baseDensity = 0.30f + pulse * 0.12f;

        // Apply transition
        // 0% to 25%: Keep original distance fog to hide horizon near ground
        // 25% to 100%: Fade in Radial Fog (vignette)
        if (progress < 0.25f) {
            getState().setFog(original_camera_state.getFog());
        } else {
            float fade = (progress - 0.25f) / 0.75f;
            // Radius shrinks from 1.5x to 1.0x as we ascend
            float radiusScale = 1.5f - (0.5f * fade);
            getState().setFog(new RadialFogInfo(Color.Linear.WHITE, baseDensity * fade, radiusScale));
        }

        switch (map_mode) {
            case TO_MAP -> {
                dx = map_x - original_camera_state.getTargetX();
                dy = map_y - original_camera_state.getTargetY();
                dz = map_z - original_camera_state.getTargetZ();
                CameraState state = getState();
                state.setTargetX(getState().getTargetX() + dx * factor);
                state.setTargetY(getState().getTargetY() + dy * factor);
                state.setTargetZ(getState().getTargetZ() + dz * factor);
                if (state.getTargetZ() > map_z - MAP_THRESHOLD) {
                    state.setTargetX(map_x);
                    state.setTargetY(map_y);
                    state.setTargetZ(map_z);
                    changeMode(MapMode.IN_MAP);
                }

                da = CameraState.MIN_ANGLE - original_camera_state.getTargetVertAngle();
                state.setTargetVertAngle(state.getTargetVertAngle() + da * factor);
            }
            case IN_MAP -> {
            }
            case FROM_MAP -> {
                dx = original_camera_state.getTargetX() - map_x;
                dy = original_camera_state.getTargetY() - map_y;
                dz = original_camera_state.getTargetZ() - map_z;
                CameraState camera_state = getState();
                camera_state.setTargetX(getState().getTargetX() + dx * factor);
                camera_state.setTargetY(getState().getTargetY() + dy * factor);
                camera_state.setTargetZ(getState().getTargetZ() + dz * factor);
                if (camera_state.getTargetZ() <= original_camera_state.getTargetZ()) {
                    camera_state.setTargetX(original_camera_state.getTargetX());
                    camera_state.setTargetY(original_camera_state.getTargetY());
                    camera_state.setTargetZ(original_camera_state.getTargetZ());
                    camera_state.setTargetVertAngle(original_camera_state.getTargetVertAngle());
                    checkPosition();
                    syncToOriginalCamera();
                    delegate.exitMapMode();
                    break;
                }

                da = original_camera_state.getTargetVertAngle() - CameraState.MIN_ANGLE;
                camera_state.setTargetVertAngle(getState().getTargetVertAngle() + da * factor);
            }
        }
    }

    private void changeMode(MapMode mode) {
        map_mode = mode;
        switch (mode) {
            case TO_MAP -> {
            }
            case IN_MAP -> {
                label.setPos((delegate.getGUIRoot().getWidth() - label.getWidth()) / 2, delegate.getGUIRoot()
                        .getHeight() - label.getHeight());
                delegate.addChild(label);
            }
            case FROM_MAP -> {
                label.remove();
            }
        }
    }

    private void syncToOriginalCamera() {
        CameraState camera_state = getState();
        original_camera_state.setCurrentX(camera_state.getTargetX());
        original_camera_state.setCurrentY(camera_state.getTargetY());
        original_camera_state.setCurrentZ(camera_state.getTargetZ());
        original_camera_state.setTargetX(camera_state.getTargetX());
        original_camera_state.setTargetY(camera_state.getTargetY());
        original_camera_state.setTargetZ(camera_state.getTargetZ());
        original_camera_state.setCurrentVertAngle(camera_state.getTargetVertAngle());
        original_camera_state.setTargetVertAngle(camera_state.getTargetVertAngle());
        original_camera_state.setCurrentHorizAngle(camera_state.getTargetHorizAngle());
        original_camera_state.setTargetHorizAngle(camera_state.getTargetHorizAngle());
        original_camera_state.setNoDetailMode(false);
        if (has_goto_target) {
            game_camera.setRotationPoint(new LandscapeTarget(
                    UnitGrid.toGridCoordinate(goto_x),
                    UnitGrid.toGridCoordinate(goto_y)
            ));
        }
    }

    public void mapGoto(float x, float y) {
        this.mapGoto(x, y, false);
    }

    public void mapGoto(float x, float y, boolean override) {
        if (map_mode == MapMode.IN_MAP || override) {
            this.goto_x = x;
            this.goto_y = y;
            this.has_goto_target = true;
            float yaw = original_camera_state.getTargetHorizAngle();
            float cam_x = x - (float) Math.cos(yaw) * horizontal_distance;
            float cam_y = y - (float) Math.sin(yaw) * horizontal_distance;
            float target_z = Math.max(getLandscapeEnvironment().getHeight(x, y),
                    getLandscapeEnvironment().getSeaLevelMeters());
            float cam_z = target_z + vertical_clearance;
            float min_z = Math.max(getLandscapeEnvironment().getHeight(cam_x, cam_y),
                    getLandscapeEnvironment().getSeaLevelMeters()) + 1.0f;
            cam_z = Math.clamp(Math.max(cam_z, min_z), min_z, GameCamera.MAX_Z);
            float landing_vert_angle = -(float) Math.atan2(cam_z - target_z, horizontal_distance);

            original_camera_state.setTargetX(cam_x);
            original_camera_state.setTargetY(cam_y);
            original_camera_state.setTargetZ(cam_z);
            original_camera_state.setTargetVertAngle(landing_vert_angle);
            changeMode(MapMode.FROM_MAP);
        }
    }

    @Override
    public void handleInput(InputEvent event) {
        // Block camera controls and delegate switches while in map mode
        for (GameAction action : BLOCKED_ACTIONS) {
            if (event.consumeAction(action)) {
                event.consume();
                return;
            }
        }

        if (event.getPhase() == InputPhase.PRESSED || event.getPhase() == InputPhase.REPEAT) {
            if (event.consumeAction(GameAction.CAMERA_MAP_MODE)) {
                changeMode((map_mode == MapMode.TO_MAP || map_mode == MapMode.IN_MAP) ? MapMode.FROM_MAP
                        : MapMode.TO_MAP);
                event.consume();
            }
        }
    }
}
