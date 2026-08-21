package com.oddlabs.tt.simulation.model;


public final class ReproduceUnitContainerFactory implements UnitContainerFactory {
    @Override
    public UnitContainer createContainer(Building building) {
        return new ReproduceUnitContainer(building);
    }
}
