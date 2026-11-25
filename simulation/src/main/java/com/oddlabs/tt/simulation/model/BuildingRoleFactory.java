package com.oddlabs.tt.simulation.model;

/// Factory creating the operational {@link BuildingRole} when a building completes construction.
@FunctionalInterface
public interface BuildingRoleFactory {
  BuildingRole createRole(Building building);
}
