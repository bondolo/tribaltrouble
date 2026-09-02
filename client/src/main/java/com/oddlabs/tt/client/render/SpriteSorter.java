package com.oddlabs.tt.client.render;

import com.oddlabs.tt.base.util.PocketList;
import com.oddlabs.tt.engine.render.CameraState;
import com.oddlabs.tt.engine.render.LODObject;
import com.oddlabs.tt.engine.render.PolyDetail;
import com.oddlabs.tt.engine.render.RenderConfig;

/**
 * Distance-based LOD polygon and point sprite sorting manager.
 */
final class SpriteSorter {
    public enum DetailMode {
        POINT,
        POLYGON
    }

    private static final int LOW_DETAIL_DIST = 200;

    private final PocketList<LODObject> sorted_models = new PocketList<>(LOW_DETAIL_DIST);
    private final int polycount_limit;

    private int used_polys = 0;

    public SpriteSorter(int graphic_detail) {
        this.polycount_limit = RenderConfig.UNIT_HIGH_POLY_COUNT[graphic_detail];
    }

    public DetailMode add(LODObject model, CameraState camera, boolean point) {
        if (point && camera.inNoDetailMode()) {
            model.markDetailPoint();
            return DetailMode.POINT;
        }
        used_polys += model.getTriangleCount(PolyDetail.LOW_POLY);

        float dist_squared = model.getEyeDistanceSquared();
        if (dist_squared >= LOW_DETAIL_DIST * LOW_DETAIL_DIST) {
            model.markDetailPolygon(PolyDetail.LOW_POLY);
        } else {
            addToPocket(dist_squared, model);
        }
        return DetailMode.POLYGON;
    }

    private void addToPocket(float dist_squared, LODObject model) {
        int dist = (int) Math.sqrt(dist_squared);
        sorted_models.add(dist, model);
    }

    public void distributeModels() {
        distributeHighPolygons();
        while (!sorted_models.isEmpty()) {
            LODObject model = sorted_models.removeBest();
            model.markDetailPolygon(PolyDetail.LOW_POLY);
        }
        sorted_models.clear();
        used_polys = 0;
    }

    private void distributeHighPolygons() {
        while (used_polys < polycount_limit) {
            if (!sorted_models.isEmpty()) {
                LODObject model = sorted_models.removeBest();
                used_polys -= model.getTriangleCount(PolyDetail.LOW_POLY);
                used_polys += model.getTriangleCount(PolyDetail.HIGH_POLY);
                model.markDetailPolygon(PolyDetail.HIGH_POLY);
            } else
                return;
        }
    }
}
