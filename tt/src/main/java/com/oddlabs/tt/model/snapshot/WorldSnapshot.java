package com.oddlabs.tt.model.snapshot;

import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * Snapshot of the entire simulation state at a specific tick.
 */
public record WorldSnapshot(
                            int tick,
                            @NonNull List<@NonNull EntitySnapshot> entities
) {
}
