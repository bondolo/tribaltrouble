package com.oddlabs.tt.engine.render;


import com.oddlabs.tt.base.geom.BoundingBox;
import com.oddlabs.tt.engine.util.DebugRender;
import com.oddlabs.tt.simulation.model.Building;
import com.oddlabs.tt.simulation.model.Model;
import com.oddlabs.tt.simulation.model.SceneryModel;
import com.oddlabs.tt.simulation.model.SupplyModel;
import com.oddlabs.tt.simulation.model.Unit;
import com.oddlabs.util.Color;
import org.jspecify.annotations.Nullable;

/**
 * Common rendering operations, including coordinate transformations,
 * distance calculations, and debug visualization.
 */
public final class RenderTools {

    public static final Color.Linear BOUNDS_COLOR_UNIT = Color.Linear.CYAN;
    public static final Color.Linear BOUNDS_COLOR_BUILDING = Color.Linear.MAGENTA;
    public static final Color.Linear BOUNDS_COLOR_SUPPLY = new Color.Linear(1f, 0.6f, 0f, 1f);
    public static final Color.Linear BOUNDS_COLOR_SCENERY = new Color.Linear(0.4f, 0.8f, 0.2f, 1f);
    public static final Color.Linear BOUNDS_COLOR_DEFAULT = Color.Linear.WHITE;

    public static float getEyeDistanceSquared(BoundingBox box, float camera_x, float camera_y,
            float camera_z) {
        float distx = camera_x - box.getCX();
        float disty = camera_y - box.getCY();
        float distz = camera_z - box.getCZ();
        return distx * distx + disty * disty + distz * distz;
    }

    public static float getCameraDistanceXYSquared(BoundingBox box, float camera_x, float camera_y) {
        float dx = camera_x - box.getCX();
        float dy = camera_y - box.getCY();
        return dx * dx + dy * dy;
    }

    public static void draw(BoundingBox box) {
        draw(box, BOUNDS_COLOR_DEFAULT);
    }

    public static void draw(@Nullable Model model) {
        if (model == null || !DebugFlags.isBoundsEnabled(BoundingMode.PLAYERS)) {
            return;
        }
        switch (model) {
            case Unit _ -> draw(model, BOUNDS_COLOR_UNIT);
            case Building _ -> draw(model, BOUNDS_COLOR_BUILDING);
            case SupplyModel _ -> draw(model, BOUNDS_COLOR_SUPPLY);
            case SceneryModel _ -> draw(model, BOUNDS_COLOR_SCENERY);
            default -> draw(model, BOUNDS_COLOR_DEFAULT);
        }
    }

    public static void draw(BoundingBox box, Color color) {
        DebugRender.drawBox(box, color);
    }

    public static void draw(BoundingBox box, BoundingMode bound_type, Color color) {
        if (DebugFlags.isBoundsEnabled(bound_type)) {
            draw(box, color);
        }
    }

    private RenderTools() {
    }
}
