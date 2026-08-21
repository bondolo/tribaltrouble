package com.oddlabs.tt.client.render;

import com.oddlabs.tt.engine.util.DebugRender;
import com.oddlabs.tt.simulation.landscape.HeightMap;
import com.oddlabs.tt.simulation.pathfinder.BezierPath;
import com.oddlabs.tt.simulation.pathfinder.GridPathNode;
import com.oddlabs.tt.simulation.pathfinder.PathTracker;
import com.oddlabs.tt.simulation.pathfinder.Region;
import com.oddlabs.tt.simulation.pathfinder.RegionNode;
import com.oddlabs.tt.simulation.pathfinder.UnitGrid;
import com.oddlabs.util.Color;
import org.jspecify.annotations.Nullable;

/**
 * Renders debug visualisations for pathfinder simulation state using {@link DebugRender}.
 *
 * <p>All rendering logic that previously lived inside {@code simulation.pathfinder} classes
 * is centralised here, keeping the simulation domain free of rendering dependencies.
 */
public final class PathfinderDebugRenderer {

    private static final float PATH_OFFSET = 2f;
    private static final float[] samplePoint = new float[2];
    private static final float[] sampleDir = new float[2];

    private PathfinderDebugRenderer() {
    }

    /**
     * Renders the Bezier curve for a {@link PathTracker}s current path in white,
     * the grid path in red, and the region path in blue.
     */
    public static void renderPathTracker(PathTracker tracker) {
        HeightMap heightmap = tracker.getUnitGrid().getHeightMap();
        renderBezierPath(tracker.getBezierPath(), heightmap);

        float next_node_x = UnitGrid.coordinateFromGrid(tracker.getNextGridX());
        float next_node_y = UnitGrid.coordinateFromGrid(tracker.getNextGridY());
        float prev_x = next_node_x;
        float prev_y = next_node_y;
        float prev_z = heightmap.getNearestHeight(prev_x, prev_y) + PATH_OFFSET;

        GridPathNode node = tracker.getGridPath();
        while (node != null) {
            float next_x = prev_x + HeightMap.METERS_PER_UNIT_GRID * node.getDirection().getDirectionX();
            float next_y = prev_y + HeightMap.METERS_PER_UNIT_GRID * node.getDirection().getDirectionY();
            float z = heightmap.getNearestHeight(next_x, next_y) + PATH_OFFSET;
            DebugRender.drawLine(prev_x, prev_y, prev_z, next_x, next_y, z, 1f, 0f, 0f);
            prev_x = next_x;
            prev_y = next_y;
            prev_z = z;
            node = (GridPathNode) node.getParent();
        }

        RegionNode region_node = tracker.getRegionPath();
        boolean first = true;
        while (region_node != null) {
            float x = UnitGrid.coordinateFromGrid(region_node.getRegion().getGridX());
            float y = UnitGrid.coordinateFromGrid(region_node.getRegion().getGridY());
            float z = heightmap.getNearestHeight(x, y) + PATH_OFFSET;
            if (!first) {
                DebugRender.drawLine(prev_x, prev_y, prev_z, x, y, z, 0f, 0f, 1f);
            }
            prev_x = x;
            prev_y = y;
            prev_z = z;
            first = false;
            region_node = (RegionNode) region_node.getParent();
        }
    }

    /**
     * Debug visualization shows the curve as a white line (enable with UNIT_GRID mode).
     */
    private static void renderBezierPath(BezierPath bezierPath, HeightMap heightmap) {
        float prev_x = 0;
        float prev_y = 0;
        float prev_z = 0;
        boolean first = true;
        for (float t = 0f; t < 1f; t += .01f) {
            bezierPath.sampleCurve(t, samplePoint, sampleDir);
            float x = samplePoint[0];
            float y = samplePoint[1];
            float z = heightmap.getHeight(x, y) + 0.5f;
            if (!first) {
                DebugRender.drawLine(prev_x, prev_y, prev_z, x, y, z, 1f, 1f, 1f);
            }
            prev_x = x;
            prev_y = y;
            prev_z = z;
            first = false;
        }
    }

    /**
     * Renders occupied grid cells as yellow crosses for the area around (x, y).
     */
    public static void renderUnitGrid(UnitGrid unitGrid, float x, float y) {
        HeightMap heightmap = unitGrid.getHeightMap();
        final int RADIUS = 30;
        final float OFFSET = 2f;
        final float HALF = 0.5f;
        int center_x = UnitGrid.toGridCoordinate(x);
        int center_y = UnitGrid.toGridCoordinate(y);
        int start_x = Math.max(0, center_x - RADIUS);
        int end_x = Math.min(unitGrid.getGridSize(), center_x + RADIUS);
        int start_y = Math.max(0, center_y - RADIUS);
        int end_y = Math.min(unitGrid.getGridSize(), center_y + RADIUS);
        for (int gy = start_y; gy < end_y; gy++) {
            for (int gx = start_x; gx < end_x; gx++) {
                if (unitGrid.isGridOccupied(gx, gy)) {
                    int s = HeightMap.METERS_PER_UNIT_GRID;
                    float xf = (gx + .5f) * s;
                    float yf = (gy + .5f) * s;
                    float z = heightmap.getNearestHeight(xf, yf) + OFFSET;
                    Color.Linear c = Color.Linear.YELLOW;
                    DebugRender.drawLine(xf - HALF, yf - HALF, z, xf + HALF, yf + HALF, z, c.r(), c.g(), c.b());
                    DebugRender.drawLine(xf + HALF, yf - HALF, z, xf - HALF, yf + HALF, z, c.r(), c.g(), c.b());
                }
            }
        }
    }

    /**
     * Renders the region map (colour-coded by region identity) and region connectivity
     * graph for the area around (x, y).
     */
    public static void renderRegions(UnitGrid unitGrid, float x, float y) {
        HeightMap heightmap = unitGrid.getHeightMap();
        final int RADIUS = 30;
        int center_x = UnitGrid.toGridCoordinate(x);
        int center_y = UnitGrid.toGridCoordinate(y);
        int start_x = Math.max(0, center_x - RADIUS);
        int end_x = Math.min(unitGrid.getGridSize(), center_x + RADIUS);
        int start_y = Math.max(0, center_y - RADIUS);
        int end_y = Math.min(unitGrid.getGridSize(), center_y + RADIUS);
        @Nullable Region last_region = null;
        for (int gy = start_y; gy < end_y; gy++) {
            for (int gx = start_x; gx < end_x; gx++) {
                float xf = UnitGrid.coordinateFromGrid(gx);
                float yf = UnitGrid.coordinateFromGrid(gy);
                float zf = heightmap.getNearestHeight(xf, yf) + 2f;
                @Nullable Region region = unitGrid.getRegion(gx, gy);
                if (region == null) {
                    DebugRender.drawPoint(xf, yf, zf, 3f,
                            Color.Linear.RED.r(), Color.Linear.RED.g(), Color.Linear.RED.b());
                } else {
                    last_region = region;
                    Color c = new Color.Linear(DebugRender.debug_colors[region.hashCode()
                            % DebugRender.debug_colors.length]);
                    DebugRender.drawPoint(xf, yf, zf, 3f, c.r(), c.g(), c.b());
                }
            }
        }
        if (last_region != null) {
            renderRegionConnections(last_region, heightmap);
            resetRegionConnectionVisited(last_region);
        }
    }

    private static void renderRegionConnections(Region region, HeightMap heightmap) {
        if (region.isVisited()) return;
        region.markVisited();
        for (Region neighbour : region.getNeighbours()) {
            float x1 = UnitGrid.coordinateFromGrid(region.getGridX());
            float y1 = UnitGrid.coordinateFromGrid(region.getGridY());
            float z1 = heightmap.getNearestHeight(x1, y1) + 2f;
            float x2 = UnitGrid.coordinateFromGrid(neighbour.getGridX());
            float y2 = UnitGrid.coordinateFromGrid(neighbour.getGridY());
            float z2 = heightmap.getNearestHeight(x2, y2) + 2f;
            DebugRender.drawLine(x1, y1, z1, x2, y2, z2, 1f, 1f, 0f);
            renderRegionConnections(neighbour, heightmap);
        }
    }

    private static void resetRegionConnectionVisited(Region region) {
        if (!region.isVisited()) return;
        region.clearVisited();
        for (Region neighbour : region.getNeighbours()) {
            resetRegionConnectionVisited(neighbour);
        }
    }
}
