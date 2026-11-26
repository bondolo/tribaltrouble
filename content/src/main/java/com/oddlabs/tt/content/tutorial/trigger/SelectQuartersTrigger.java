package com.oddlabs.tt.content.tutorial.trigger;

import com.oddlabs.tt.content.tutorial.Tutorial;

/// Tutorial trigger waiting for the player to select a quarters building.
final class SelectQuartersTrigger extends TutorialTrigger {
  SelectQuartersTrigger() {
    super(.1f, 0f, "select_quarters");
  }

  @Override
  public void run(Tutorial tutorial) {
    tutorial
        .getViewer()
        .getSelection()
        .getCurrentSelection()
        .getBuilding()
        .ifPresent(building -> {
          if (building.canReproduce()) tutorial.next(new UnitsInQuartersTrigger());
        });
  }
}
