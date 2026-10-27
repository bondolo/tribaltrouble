package com.oddlabs.tt.simulation.model;

import org.jspecify.annotations.Nullable;

/**
 * Tier of finished weaponry manufactured in armories and equipped by warriors.
 */
public enum WeaponTier {
    /** Tier-1 rock weaponry (Rock Axe / Rock Spear). */
    ROCK(SupplyType.ROCK, DeployType.ROCK_WARRIOR, 40f, 1),

    /** Tier-2 iron weaponry (Iron Axe / Iron Spear). */
    IRON(SupplyType.IRON, DeployType.IRON_WARRIOR, 80f, 3),

    /** Tier-3 rubber weaponry (Rubber Axe / Rubber Spear). */
    RUBBER(SupplyType.RUBBER, DeployType.RUBBER_WARRIOR, 120f, 8);

    private final SupplyType supplyType;
    private final DeployType deployType;
    private final float buildTime;
    private final int statusMultiplier;

    WeaponTier(SupplyType supplyType, DeployType deployType, float buildTime, int statusMultiplier) {
        this.supplyType = supplyType;
        this.deployType = deployType;
        this.buildTime = buildTime;
        this.statusMultiplier = statusMultiplier;
    }

    /**
     * {@return the required raw resource supply type used to forge this weapon}
     */
    public SupplyType getSupplyType() {
        return supplyType;
    }

    /**
     * {@return the corresponding warrior deployment type equipped with this weapon tier}
     */
    public DeployType getDeployType() {
        return deployType;
    }

    /**
     * {@return the base time in seconds required to produce one unit of this weapon tier}
     */
    public float getBuildTime() {
        return buildTime;
    }

    /**
     * {@return the status value weight multiplier contributed by this weapon tier}
     */
    public int getStatusMultiplier() {
        return statusMultiplier;
    }

    /**
     * Maps a warrior deploy type to its corresponding weapon tier.
     *
     * @param deployType the warrior deploy type
     * @return the matching weapon tier, or null if not a warrior deploy type
     */
    public static @Nullable WeaponTier fromDeployType(DeployType deployType) {
        return switch (deployType) {
            case ROCK_WARRIOR -> ROCK;
            case IRON_WARRIOR -> IRON;
            case RUBBER_WARRIOR -> RUBBER;
            default -> null;
        };
    }
}
