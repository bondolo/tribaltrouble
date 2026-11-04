package com.oddlabs.tt.simulation.landscape;

import com.oddlabs.tt.simulation.model.Building;
import com.oddlabs.tt.simulation.model.Model;
import com.oddlabs.tt.simulation.model.Selectable;
import com.oddlabs.tt.simulation.model.SupplyType;
import com.oddlabs.tt.simulation.model.Target;
import com.oddlabs.tt.simulation.model.Unit;

/**
 * Listener interface for observing simulation events such as combat notifications,
 * target registrations, terrain patch edits, and environmental occurrences.
 */
public interface NotificationListener {

    /**
     * No-op placeholder listener used during {@link World} construction and teardown.
     *
     * Events fired while the {@link World} is being constructed are absorbed by this listener
     * to prevent callbacks on an uninitialised handler. Once the real handler is installed via
     * {@code setNotificationListener()}, missed target registrations are replayed in bulk.
     * During teardown, {@code close()} restores this listener to prevent stale callbacks
     * after the {@link World} is disposed.
     */
    NotificationListener EMPTY = new NotificationListener() {
    };

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

    default void onHeightEdited(int gridX, int gridY, float height) {
    }

    default void gamespeedChanged(int speed) {
    }

    default void playerGamespeedChanged() {
    }

    default void treeFelled(TreeSupply tree) {
    }

    default void onHarvest(Model model, SupplyType supplyType) {
    }

    default void onRepair(Model model) {
    }

    default void onBuildingHit(Building building) {
    }

    default void onUnitDeath(Unit unit) {
    }

    default void onUnitAttack(Unit attacker, Unit target) {
    }

    default void onChickenCluck(Model model) {
    }

    default void onChickenPeck(Model model) {
    }

    default void onChickenDeath(Model model) {
    }

    default void onLightningStrike(Model model, Target target) {
    }

    default void onSonicBlast(Model model) {
    }

    default void onModelRemoved(Model model) {
    }
}
