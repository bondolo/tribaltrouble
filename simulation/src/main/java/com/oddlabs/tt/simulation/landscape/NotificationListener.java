package com.oddlabs.tt.simulation.landscape;

import com.oddlabs.tt.simulation.model.Model;
import com.oddlabs.tt.simulation.model.Race;
import com.oddlabs.tt.simulation.model.Selectable;
import com.oddlabs.tt.simulation.model.SupplyType;
import com.oddlabs.tt.simulation.model.Target;
import com.oddlabs.tt.simulation.model.Unit;
import com.oddlabs.tt.simulation.model.UnitVisualType;

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

    default void onHarvest(Model model, SupplyType supplyType) {
    }

    default void onRepair(Model model) {
    }

    default void onBuildingHit(float x, float y, float z) {
    }

    default void onUnitDeath(Unit unit, UnitVisualType unitType, Race race) {
    }

    default void onUnitAttack(UnitVisualType unitType, Race race, float x, float y, float z) {
    }

    default void onChickenCluck(Model model) {
    }

    default void onChickenPeck(float x, float y, float z) {
    }

    default void onChickenDeath(Model model) {
    }

    default void onLightningStrike(float x, float y, float z) {
    }

    default void onSonicBlast(float targetX, float targetY, float targetZ, float radius, float duration) {
    }

    default void onWeaponThrow(float x, float y, float z) {
    }

    default void onModelRemoved(Model model) {
    }
}
