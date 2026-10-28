package com.oddlabs.tt.simulation.pathfinder;

import com.oddlabs.tt.simulation.model.Selectable;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Scan filter locating occupants matching a specific type within a circular radius.
 */
public final class FindOccupantFilter<S extends Selectable<?>> implements ScanFilter {

    private final float x;
    private final float y;
    private final float radius;
    private final @Nullable Selectable<?> src;
    private final Class<? extends S> type;
    private final List<S> result = new ArrayList<>();

    public FindOccupantFilter(float x, float y, float radius, @Nullable Selectable<?> src, Class<? extends S> type) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.src = src;
        this.type = type;
    }

    @Override
    public int getMinRadius() {
        return 0;
    }

    @Override
    public int getMaxRadius() {
        return UnitGrid.toGridCoordinate(radius);
    }

    @Override
    public boolean requiresOccupant() {
        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean filter(int grid_x, int grid_y, @Nullable Occupant occ) {
        if (occ != src && type.isInstance(occ)) {
            S s = (S) occ;
            float dx = s.getPositionX() - x;
            float dy = s.getPositionY() - y;
            float squared_dist = dx * dx + dy * dy;
            if (!result.contains(s) && squared_dist < radius * radius) {
                result.add(s);
                return true;
            }
        }
        return false;
    }

    public Iterable<S> getResult() {
        return result;
    }
}
