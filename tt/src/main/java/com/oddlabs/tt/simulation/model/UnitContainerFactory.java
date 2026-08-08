package com.oddlabs.tt.simulation.model;

public sealed interface UnitContainerFactory permits MountUnitContainerFactory, ReproduceUnitContainerFactory,
        WorkerUnitContainerFactory {
    UnitContainer createContainer(Building building);
}
