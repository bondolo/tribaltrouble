package com.oddlabs.tt.simulation.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Unit tests verifying {@link WeaponTier} costs, build times, multipliers, and mappings.
 */
final class WeaponTierTest {

    @Test
    void testRockWeaponProperties() {
        WeaponTier tier = WeaponTier.ROCK;
        assertSame(SupplyType.ROCK, tier.getSupplyType());
        assertSame(DeployType.ROCK_WARRIOR, tier.getDeployType());
        assertEquals(40f, tier.getBuildTime());
        assertEquals(1, tier.getStatusMultiplier());

        Cost cost = tier.getCost();
        assertEquals(2, cost.getCost(SupplyType.WOOD));
        assertEquals(1, cost.getCost(SupplyType.ROCK));
        assertEquals(0, cost.getCost(SupplyType.IRON));
        assertEquals(0, cost.getCost(SupplyType.RUBBER));
    }

    @Test
    void testIronWeaponProperties() {
        WeaponTier tier = WeaponTier.IRON;
        assertSame(SupplyType.IRON, tier.getSupplyType());
        assertSame(DeployType.IRON_WARRIOR, tier.getDeployType());
        assertEquals(80f, tier.getBuildTime());
        assertEquals(3, tier.getStatusMultiplier());

        Cost cost = tier.getCost();
        assertEquals(2, cost.getCost(SupplyType.WOOD));
        assertEquals(0, cost.getCost(SupplyType.ROCK));
        assertEquals(1, cost.getCost(SupplyType.IRON));
        assertEquals(0, cost.getCost(SupplyType.RUBBER));
    }

    @Test
    void testRubberWeaponProperties() {
        WeaponTier tier = WeaponTier.RUBBER;
        assertSame(SupplyType.RUBBER, tier.getSupplyType());
        assertSame(DeployType.RUBBER_WARRIOR, tier.getDeployType());
        assertEquals(120f, tier.getBuildTime());
        assertEquals(8, tier.getStatusMultiplier());

        Cost cost = tier.getCost();
        assertEquals(2, cost.getCost(SupplyType.WOOD));
        assertEquals(1, cost.getCost(SupplyType.ROCK));
        assertEquals(1, cost.getCost(SupplyType.IRON));
        assertEquals(1, cost.getCost(SupplyType.RUBBER));
    }

    @Test
    void testFromDeployTypeMapping() {
        assertSame(WeaponTier.ROCK, WeaponTier.fromDeployType(DeployType.ROCK_WARRIOR));
        assertSame(WeaponTier.IRON, WeaponTier.fromDeployType(DeployType.IRON_WARRIOR));
        assertSame(WeaponTier.RUBBER, WeaponTier.fromDeployType(DeployType.RUBBER_WARRIOR));
        assertNull(WeaponTier.fromDeployType(DeployType.PEON));
        assertNull(WeaponTier.fromDeployType(DeployType.PEON_HARVEST_TREE));
    }
}
