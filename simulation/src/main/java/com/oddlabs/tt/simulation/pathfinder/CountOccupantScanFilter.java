package com.oddlabs.tt.simulation.pathfinder;

import com.oddlabs.tt.simulation.model.Selectable;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Scan filter that counts matching occupants within a radius with optional early termination limit.
 */
public final class CountOccupantScanFilter<S extends Selectable<?>> implements ScanFilter {
    private final float x;
    private final float y;
    private final float radius;
    private final @Nullable Selectable<?> src;
    private final Class<S> type;
    private final Predicate<S> predicate;
    private final int limit;
    private @Nullable Set<Selectable<?>> seenMultiTile = null;
    private int count = 0;

    public CountOccupantScanFilter(float x, float y, float radius, @Nullable Selectable<?> src,
            Class<S> type, Predicate<S> predicate) {
        this(x, y, radius, src, type, predicate, Integer.MAX_VALUE);
    }

    public CountOccupantScanFilter(float x, float y, float radius, @Nullable Selectable<?> src,
            Class<S> type, Predicate<S> predicate, int limit) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.src = src;
        this.type = type;
        this.predicate = predicate;
        this.limit = limit;
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
    @SuppressWarnings("unchecked")
    public boolean filter(int grid_x, int grid_y, @Nullable Occupant occ) {
        if (occ != src && type.isInstance(occ)) {
            S s = (S) occ;
            float dx = s.getPositionX() - x;
            float dy = s.getPositionY() - y;
            if (dx * dx + dy * dy < radius * radius && predicate.test(s)) {
                if (s.getSize() > 1f) {
                    if (seenMultiTile == null) {
                        seenMultiTile = new HashSet<>(4);
                    }
                    if (!seenMultiTile.add(s)) {
                        return false;
                    }
                }
                count++;
                return count >= limit;
            }
        }
        return false;
    }

    public int getCount() {
        return count;
    }

    public boolean hasMatch() {
        return count > 0;
    }
}
