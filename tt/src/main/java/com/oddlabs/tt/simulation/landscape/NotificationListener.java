package com.oddlabs.tt.simulation.landscape;

import com.oddlabs.tt.simulation.model.Selectable;
import com.oddlabs.tt.simulation.model.Target;
import org.jspecify.annotations.NonNull;

/**
 * Listener interface for observing simulation events such as combat notifications,
 * target registrations, terrain patch edits, and environmental occurrences.
 */
public interface NotificationListener {
    default void newAttackNotification(@NonNull Selectable<?> target) {
    }

    default void newSelectableNotification(@NonNull Selectable<?> target) {
    }

    default void registerTarget(@NonNull Target target) {
    }

    default void unregisterTarget(@NonNull Target target) {
    }

    default void patchesEdited(int patch_x0, int patch_y0, int patch_x1, int patch_y1) {
    }

    default void gamespeedChanged(int speed) {
    }

    default void playerGamespeedChanged() {
    }

    default void treeFelled(AbstractTreeGroup.@NonNull TreeType treeType, float x, float y, float z) {
    }
}
