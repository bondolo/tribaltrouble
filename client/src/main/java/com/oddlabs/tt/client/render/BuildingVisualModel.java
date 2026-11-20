package com.oddlabs.tt.client.render;

import com.oddlabs.tt.audio.AudioImplementation;
import com.oddlabs.tt.simulation.model.Building;

/// {@link VisualModel} implementation for buildings managing damage and production accessories.
public final class BuildingVisualModel extends SelectableVisualModel<Building> {
  public BuildingVisualModel(Building building, AudioImplementation audio) {
    super(building);
    float hitOffsetZ = building.getHitOffsetZ();
    addAccessory(new BuildingDamagedAccessory(building, hitOffsetZ, audio));
    addAccessory(new BuildingProductionAccessory(building, audio));
  }

  @Override
  public float getSelectionRadius() {
    var template = selectable.getTemplate();
    return switch (selectable.getBuildStage()) {
      case START -> template.getStartSelectionRadius();
      case HALFBUILT -> template.getHalfbuiltSelectionRadius();
      case UNPLACED, BUILT -> template.getBuiltSelectionRadius();
    };
  }

  @Override
  public float getSelectionHeight() {
    var template = selectable.getTemplate();
    return switch (selectable.getBuildStage()) {
      case START -> template.getStartSelectionHeight();
      case HALFBUILT -> template.getHalfbuiltSelectionHeight();
      case UNPLACED, BUILT -> template.getBuiltSelectionHeight();
    };
  }
}
