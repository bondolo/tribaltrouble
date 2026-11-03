package com.oddlabs.tt.engine.render;

import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders 2D circular map dots for entities when the camera is in strategic overview mode.
 */
public final class MapDotRenderer {
    private final InstancedSpriteRenderer instancedSpriteRenderer;
    private final List<ModelState<?>> mapDots = new ArrayList<>();
    private final Matrix4f tempMatrix = new Matrix4f();

    MapDotRenderer(InstancedSpriteRenderer instancedSpriteRenderer) {
        this.instancedSpriteRenderer = instancedSpriteRenderer;
    }

    public void add(ModelState<?> model) {
        mapDots.add(model);
    }

    public void render() {
        if (DebugFlags.draw_misc && !mapDots.isEmpty()) {
            SpriteList quadList = SpriteList.getQuadInstance();
            var batch = instancedSpriteRenderer.getBatch(quadList, instancedSpriteRenderer.getWhiteTexture(), null,
                    null, false, true, false, false);
            for (ModelState<?> model : mapDots) {
                if (DebugFlags.isBoundsEnabled(BoundingMode.PLAYERS)) {
                    RenderTools.draw(model.getModel());
                }
                float x = model.getModel().getPositionX();
                float y = model.getModel().getPositionY();
                float z = model.getModel().getPositionZ();
                float r = model.getNoDetailSize();
                tempMatrix.identity().translation(x, y, z + 0.1f).scale(r * 2);
                batch.addInstance(-1, tempMatrix,
                        model.getTeamColorR(), model.getTeamColorG(), model.getTeamColorB(), model.getTeamColorA(),
                        0f, 0f, 0f, 0f);
            }
        }
        mapDots.clear();
    }
}
