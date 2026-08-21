package com.oddlabs.tt.simulation.model;

import com.oddlabs.tt.simulation.landscape.World;
import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * Container and lifecycle manager for all simulation-side resource supply managers.
 */
public class SupplyManagers {
    private final Map<SupplyType, SupplyManager> supply_managers;

    public final void debugSpawn() {
        supply_managers.values().forEach(SupplyManager::debugSpawnSupply);
    }

    public SupplyManagers(World world) {
        EnumMap<SupplyType, SupplyManager> map = new EnumMap<>(SupplyType.class);
        map.put(SupplyType.WOOD, new SupplyManager(world));
        map.put(SupplyType.ROCK, new SupplyManager(world));
        map.put(SupplyType.IRON, new SupplyManager(world));
        map.put(SupplyType.RUBBER, new RubberSupplyManager(world));
        supply_managers = map;
    }

    public final @Nullable SupplyManager getSupplyManager(SupplyType type) {
        return supply_managers.get(type);
    }
}
