package com.oddlabs.tt.simulation.model;


/**
 * Factory class for constructing UnitSupplyContainers.
 */
public final class UnitSupplyContainerFactory extends SupplyContainerFactory {

    public UnitSupplyContainerFactory(int max_resource_count) {
        super(max_resource_count);
    }

    @Override
    public SupplyContainer createContainer(Selectable<?> selectable) {
        return new UnitSupplyContainer(getMaxResourceCount());
    }
}
