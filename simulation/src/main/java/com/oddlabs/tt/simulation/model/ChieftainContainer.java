package com.oddlabs.tt.simulation.model;

/// Manages chieftain training state and progress inside a quarters building.
public final class ChieftainContainer {
  private static final float PROGRESS_AMOUNT = 1f / 40f;

  private final Building building;
  private final Runnable onResetReproduction;

  private boolean training = false;
  private float progress = 0f;

  public ChieftainContainer(Building building, Runnable onResetReproduction) {
    this.building = building;
    this.onResetReproduction = onResetReproduction;
  }

  public float getBuildProgress() {
    return progress;
  }

  public void startTraining() {
    training = true;
    onResetReproduction.run();
  }

  public void stopTraining() {
    training = false;
    progress = 0f;
    onResetReproduction.run();
  }

  public boolean isTraining() {
    return training;
  }

  public void progress() {
    progress += PROGRESS_AMOUNT;
    if (progress >= 1f) {
      building.deployChieftain();
    }
  }
}
