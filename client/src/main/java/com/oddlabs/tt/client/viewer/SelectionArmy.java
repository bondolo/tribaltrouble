package com.oddlabs.tt.client.viewer;

import static com.oddlabs.tt.simulation.model.Ability.BUILD;
import static com.oddlabs.tt.simulation.model.Ability.MAGIC;

import com.oddlabs.tt.simulation.model.Army;
import com.oddlabs.tt.simulation.model.Building;
import com.oddlabs.tt.simulation.model.Selectable;
import com.oddlabs.tt.simulation.model.Unit;
import com.oddlabs.tt.simulation.player.Player;
import java.util.Collection;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/// Tracks the local player's currently selected units, builders, chieftain, and building.
public final class SelectionArmy extends Army {

  private final Player local_player;
  private int num_units;
  private int num_builders;
  private @Nullable Unit chieftain;
  private @Nullable Building building;

  SelectionArmy(Player local_player) {
    this.local_player = local_player;
  }

  public int getNumBuilders() {
    return num_builders;
  }

  public int getNumUnits() {
    return num_units;
  }

  public Optional<Unit> getChieftain() {
    return Optional.ofNullable(chieftain);
  }

  public Optional<Building> getBuilding() {
    return Optional.ofNullable(building);
  }

  private void update() {
    num_units = 0;
    num_builders = 0;
    chieftain = null;
    building = null;
    for (Selectable<?> s : getSet()) {
      if (s.getOwner() != local_player) continue;
      if (s.hasAbility(BUILD)) num_builders++;
      else if (s.hasAbility(MAGIC)) chieftain = (Unit) s;
      if (s instanceof Building building1) {
        building = building1;
      } else {
        num_units++;
      }
    }
  }

  @Override
  public void clear() {
    super.clear();
    update();
  }

  @Override
  public void remove(Selectable<?> selectable) {
    super.remove(selectable);
    update();
  }

  @Override
  public void add(Selectable<?> selectable) {
    super.add(selectable);
    update();
  }

  @Override
  public void addAll(Collection<? extends Selectable<?>> selectable) {
    super.addAll(selectable);
    update();
  }
}
