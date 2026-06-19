package com.oddlabs.tt.model.snapshot;

import com.oddlabs.tt.model.BoundingBox;
import org.jspecify.annotations.NonNull;

/**
 * Unified read-only snapshot interface for a world entity.
 */
public sealed interface EntitySnapshot permits
        VisualSnapshots.UnitSnapshot,
        VisualSnapshots.BuildingSnapshot,
        VisualSnapshots.SupplySnapshot,
        VisualSnapshots.ScenerySnapshot,
        VisualSnapshots.EffectSnapshot {
    int id();

    float x();

    float y();

    float z();

    float dirX();

    float dirY();

    @NonNull
    BoundingBox bounds();

    int animation();

    float animationTicks();
}
