package com.oddlabs.tt.landscape;

import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.Target;
import com.oddlabs.tt.model.WeaponVisualType;
import com.oddlabs.tt.model.MagicVisualType;
import com.oddlabs.tt.model.Race;
import com.oddlabs.util.Color;
import org.jspecify.annotations.NonNull;

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

    default void weaponThrown(
            float startX, float startY, float startZ,
            float endX, float endY, float destZ,
            float zSpeed, float timeLimit,
            @NonNull WeaponVisualType weaponType, @NonNull Race race,
            Color.@NonNull Linear teamColor, boolean rotating) {
    }

    default void magicEffectSpawned(
            float x, float y, float z,
            @NonNull MagicVisualType type,
            float radius, float duration,
            Color.@NonNull Linear color) {
    }

    default void emitterSpawned(com.oddlabs.tt.render.particle.@NonNull Emitter<?> emitter, boolean isCollapse) {
    }

    default void lightningStrikeSpawned(
            float srcX, float srcY, float srcZ,
            float dstX, float dstY, float dstZ) {
    }
}
