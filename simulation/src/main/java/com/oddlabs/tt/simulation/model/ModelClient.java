package com.oddlabs.tt.simulation.model;

import org.jspecify.annotations.NonNull;

/**
 * Callbacks that the simulation can trigger on the client-side state.
 */
public interface ModelClient extends ClientState, AutoCloseable {
    float DURATION_CHICKEN_CLUCK = 0.8f;
    float DURATION_UNIT_DEATH = 1.5f;
    float DURATION_HARVEST = 1.0f;
    float DURATION_REPAIR = 1.0f;

    /**
     * Spawns a visual sound emoji/thought accessory on the model.
     *
     * @param emoji The emoji type of the thought.
     * @param duration The duration to display the thought.
     * @param audioDistance The maximum distance at which the sound/visual is audible.
     */
    void addVisualSound(@NonNull EmojiType emoji, float duration, float audioDistance);

    /**
     * Handles presentation audio and visual effects when a unit harvests a supply.
     *
     * @param supplyType The supply type harvested.
     */
    default void onHarvest(@NonNull SupplyType supplyType) {
    }

    /**
     * Handles presentation audio and visual effects when a unit repairs a building.
     */
    default void onRepair() {
    }

    /**
     * Handles presentation audio and visual effects when a building receives damage.
     */
    default void onBuildingHit() {
    }

    /**
     * Handles presentation audio and visual effects when a unit dies.
     *
     * @param race The race of the unit.
     * @param unitType The visual type of the unit.
     * @param pitchRange The pitch variation range for the death sound.
     */
    default void onUnitDeath(@NonNull Race race, @NonNull UnitVisualType unitType, float pitchRange) {
    }

    /**
     * Handles presentation audio for a melee hit.
     *
     * @param targetX Destination X.
     * @param targetY Destination Y.
     * @param targetZ Destination Z.
     * @param pitchRange Pitch variation range.
     */
    default void onMeleeHit(float targetX, float targetY, float targetZ, float pitchRange) {
    }

    /**
     * Handles presentation audio and visual effects for chicken idle cluck.
     */
    default void onChickenCluck() {
    }

    /**
     * Handles presentation audio for chicken pecking or flying.
     */
    default void onChickenPeck() {
    }

    /**
     * Handles presentation audio for chicken death.
     */
    default void onChickenDeath() {
    }

    /**
     * Spawns a visual lightning strike from this model to the specified destination coordinate.
     *
     * @param targetX The destination X coordinate.
     * @param targetY The destination Y coordinate.
     * @param targetZ The destination Z coordinate.
     */
    void addLightningStrike(float targetX, float targetY, float targetZ);

    /**
     * Spawns a visual sonic blast expanding shockwave ring from this model.
     *
     * @param targetX The origin X coordinate.
     * @param targetY The origin Y coordinate.
     * @param targetZ The origin Z coordinate.
     * @param radius The maximum shockwave radius.
     * @param duration The duration of the shockwave expansion in seconds.
     */
    void addSonicBlast(float targetX, float targetY, float targetZ, float radius, float duration);

    /**
     * Cleans up any resources (like active audio loops) associated with the client-side model.
     */
    @Override
    void close();
}
