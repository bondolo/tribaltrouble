package com.oddlabs.tt.simulation.landscape;

import com.oddlabs.tt.simulation.model.Selectable;
import com.oddlabs.tt.simulation.model.Target;

/**
 * Listener interface for observing simulation events such as combat notifications,
 * target registrations, terrain patch edits, and environmental occurrences.
 */
public interface NotificationListener {
    default void newAttackNotification(Selectable<?> target) {
    }

    default void newSelectableNotification(Selectable<?> target) {
    }

    default void registerTarget(Target target) {
    }

    default void unregisterTarget(Target target) {
    }

    default void patchesEdited(int patch_x0, int patch_y0, int patch_x1, int patch_y1) {
    }

    default void gamespeedChanged(int speed) {
    }

    default void playerGamespeedChanged() {
    }

    default void treeFelled(AbstractTreeGroup.TreeType treeType, float x, float y, float z) {
    }
}
