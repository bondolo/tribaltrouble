package com.oddlabs.tt.delegate;

import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.camera.GameCamera;
import com.oddlabs.tt.gui.MouseButton;
import com.oddlabs.tt.input.GameAction;
import com.oddlabs.tt.input.InputEvent;
import com.oddlabs.tt.input.InputPhase;
import com.oddlabs.tt.landscape.HeightMap;
import com.oddlabs.tt.landscape.LandscapeTarget;
import com.oddlabs.tt.model.Abilities;
import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.model.BuildingTemplate;
import com.oddlabs.tt.pathfinder.UnitGrid;
import com.oddlabs.tt.player.BuildingSiteScanFilter;
import com.oddlabs.tt.render.BuildingSiteRenderer;
import com.oddlabs.tt.render.LandscapeRenderer;
import com.oddlabs.tt.render.MatrixStack;
import com.oddlabs.tt.render.RenderQueues;
import com.oddlabs.tt.render.Renderer;
import com.oddlabs.tt.render.Sprite;
import com.oddlabs.tt.render.SpriteKey;
import com.oddlabs.tt.render.SpriteRenderer;
import com.oddlabs.tt.render.VisualRegistry;
import com.oddlabs.tt.render.shader.SpriteShader;
import com.oddlabs.tt.render.state.BlendMode;
import com.oddlabs.tt.render.state.CullMode;
import com.oddlabs.tt.render.state.DepthMode;
import com.oddlabs.tt.render.state.RenderContext;
import com.oddlabs.tt.viewer.WorldViewer;
import com.oddlabs.util.Color;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.logging.Logger;

/**
 * Handles the user interaction for placing a new building on the landscape.
 */
public final class PlacingDelegate extends ControllableCameraDelegate {
    private static final Logger logger = Logger.getLogger(PlacingDelegate.class.getName());
    private static final int GRID_RADIUS = 20;
    private static final Color.Linear GOOD_PLACEMENT = Color.Linear.WHITE.alpha(0.8f);
    private static final Color.Linear BAD_PLACEMENT = Color.Linear.RED.alpha(0.8f);

    private final BuildingSiteRenderer site_renderer = new BuildingSiteRenderer();
    private final SpriteShader spriteShader = new SpriteShader();
    private final int building_index;

    public PlacingDelegate(@NonNull WorldViewer viewer, @NonNull CameraState old_camera, int building_index) {
        super(viewer, new GameCamera(viewer, old_camera));
        this.building_index = building_index;
    }

    private @NonNull BuildingTemplate getTemplate() {
        return getViewer().getLocalPlayer().getRace().getBuildingTemplate(building_index);
    }

    public void placeObject() {
        getViewer().getPicker().pickLocation(getCamera().getState()).ifPresentOrElse(landscape_hit -> {
            int placing_grid_x = landscape_hit.getGridX();
            int placing_grid_y = landscape_hit.getGridY();
            if (Building.isPlacingLegal(getViewer().getWorld().getUnitGrid(), getTemplate(), placing_grid_x,
                    placing_grid_y)) {
                var peons = getViewer().getSelection().getCurrentSelection().filter(Abilities.BUILD);
                if (peons.length > 0) {
                    logger.info("placeObject: Placing building at " + placing_grid_x + "," + placing_grid_y);
                    getViewer().getPeerHub().getPlayerInterface().placeBuilding(peons, building_index, placing_grid_x,
                            placing_grid_y);
                } else {
                    logger.info("placeObject: No peons selected");
                }
                logger.info("placeObject: Popping delegate");
                pop();
            } else {
                logger.info("placeObject: Placement illegal");
            }
        }, () -> logger.info("placeObject: Pick failed (off map?)"));
    }

    @Override
    public void handleInput(@NonNull InputEvent event) {
        if (event.consumeAction(GameAction.UI_ACTIVATE)) {
            if (event.getPhase() == InputPhase.RELEASED) {
                placeObject();
            }
            event.consume();
            return;
        }

        if (event.getPhase() == InputPhase.PRESSED || event.getPhase() == InputPhase.REPEAT) {
            if (event.consumeAction(GameAction.UI_CANCEL)) {
                pop();
                event.consume();
                return;
            }
        }

        super.handleInput(event);
    }

    @Override
    public void mousePressed(@NonNull MouseButton button, int x, int y) {
        switch (button) {
            case LEFT -> placeObject();
            case RIGHT -> pop();
            default -> super.mousePressed(button, x, y);
        }
    }

    @Override
    public void render3D(@NonNull LandscapeRenderer renderer, @NonNull RenderQueues queues, @NonNull CameraState state,
            @NonNull MatrixStack modelViewStack, @NonNull MatrixStack projectionStack) {
        var hit = getViewer().getPicker().pickLocation(getCamera().getState());
        if (hit.isEmpty()) {
            return;
        }
        var landscape_hit = hit.get();
        int placing_grid_x = landscape_hit.getGridX() - (getTemplate().getPlacingSize() - 1);
        int placing_grid_y = landscape_hit.getGridY() - (getTemplate().getPlacingSize() - 1);
        int placing_center_grid_x = landscape_hit.getGridX();
        int placing_center_grid_y = landscape_hit.getGridY();

        float center_x = HeightMap.METERS_PER_UNIT_GRID * (placing_grid_x + (getTemplate().getPlacingSize() - .5f));
        float center_y = HeightMap.METERS_PER_UNIT_GRID * (placing_grid_y + (getTemplate().getPlacingSize() - .5f));

        UnitGrid unit_grid = getViewer().getWorld().getUnitGrid();
        BuildingSiteScanFilter filter = new BuildingSiteScanFilter(unit_grid, getTemplate(), GRID_RADIUS, false);
        unit_grid.scan(filter, placing_center_grid_x, placing_center_grid_y);
        List<LandscapeTarget> target_list = filter.getResult();

        RenderContext context = Renderer.getRenderer().getRenderContext();
        site_renderer.renderSites(context, queues, renderer, modelViewStack, projectionStack, target_list, center_x,
                center_y, 2 * GRID_RADIUS);

        com.oddlabs.tt.util.GLUtils.checkGLError("Placing: After renderSites");

        SpriteKey built_key = VisualRegistry.getInstance().getBuildingVisuals(
                getViewer().getLocalPlayer().getRace().getRaceType(),
                getTemplate().getVisualType()
        ).built();
        SpriteRenderer built_renderer = queues.getRenderer(built_key);
        Sprite sprite = built_renderer.getSpriteList().getSprite(0);

        try (var _ = spriteShader.use()) {
            spriteShader.setUniform(SpriteShader.Uniforms.DESATURATE, 0.3f);
            sprite.setupShaderUniforms(context, spriteShader, 0, false);
            spriteShader.setUniform(SpriteShader.Uniforms.MODULATE_COLOR, true);
            spriteShader.setUniform(SpriteShader.Uniforms.ALPHA_TEST_VALUE, 0.5f);

            var placeColor = Building.isPlacingLegal(unit_grid, getTemplate(), placing_center_grid_x,
                    placing_center_grid_y)
                            ? GOOD_PLACEMENT : BAD_PLACEMENT;
            spriteShader.setUniform(SpriteShader.Uniforms.COLOR, placeColor);

            float z = getViewer().getWorld().getHeightMap().getNearestHeight(center_x, center_y);

            modelViewStack.push();
            modelViewStack.translate(center_x, center_y, z);
            spriteShader.setUniform(SpriteShader.Uniforms.MODEL_VIEW_MATRIX, modelViewStack.current());

            try (var _ = context.withCullMode(CullMode.BACK)) {
                // Pass 1: Depth Prime (Write Depth, No Color)
                try (var _ = context.withDepthMode(DepthMode.READ_WRITE); var _ = context.withColorMask(false, false,
                        false, false); var _ = context.withBlendMode(BlendMode.NONE)) {

                    sprite.renderShader(spriteShader, 0, 0f, built_renderer.getSpriteList());
                }

                // Pass 2: Color Render (No Depth Write, Equal Depth)
                try (var _ = context.withDepthMode(DepthMode.READ_ONLY); var _ = context.withColorMask(true, true, true,
                        true); var _ = context.withBlendMode(BlendMode.ALPHA)) {

                    sprite.renderShader(spriteShader, 0, 0f, built_renderer.getSpriteList());
                }
            } finally {
                spriteShader.setUniform(SpriteShader.Uniforms.DESATURATE, 0.0f);
                spriteShader.setUniform(SpriteShader.Uniforms.MODULATE_COLOR, false);
                spriteShader.setUniform(SpriteShader.Uniforms.ALPHA_TEST_VALUE, 0.3f);
            }

            modelViewStack.pop();
        }
    }
}
