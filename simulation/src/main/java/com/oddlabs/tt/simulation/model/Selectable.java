package com.oddlabs.tt.simulation.model;

import com.oddlabs.tt.base.animation.Animated;
import com.oddlabs.tt.base.event.StateChecksum;
import com.oddlabs.tt.simulation.pathfinder.Occupant;
import com.oddlabs.tt.simulation.pathfinder.UnitGrid;
import com.oddlabs.tt.simulation.player.Player;
import org.jspecify.annotations.Nullable;

/// Represents a {@link Model} that can be selected, commanded. Base class for both {@link Unit} and {@link Building}.
public abstract sealed class Selectable<T extends Template> extends Model implements Target, Animated, ModelToolTip
    permits Unit, Building {

  private final Player owner;
  private final Abilities abilities = new Abilities(Abilities.NONE);
  private final T template;

  private boolean dead;

  private int grid_x;
  private int grid_y;

  protected Selectable(Player owner, T template) {
    super(owner.getWorld());
    this.owner = owner;
    this.template = template;
  }

  @Override
  public float getShadowDiameter() {
    return template.getShadowDiameter();
  }

  public final T getTemplate() {
    return template;
  }

  public float getDefenseChance() {
    return template.getDefenseChance();
  }

  public abstract boolean isEnabled();

  public abstract float getSelectionRadius();

  public abstract float getSelectionHeight();

  public float getHitOffsetZ() {
    return template.getHitOffsetZ(0);
  }

  @Override
  public final void animate(float dt) {
    capturePreviousTransform();
    doAnimate(dt);
    owner.getWorld().updateGlobalChecksum(grid_x + grid_y);
  }

  protected void doAnimate(float t) {}

  public abstract EntityActivity getActivity();

  public final UnitGrid getUnitGrid() {
    return owner.getWorld().getUnitGrid();
  }

  private static boolean isAdjacent(UnitGrid unit_grid, int grid_x, int grid_y, Occupant occ) {
    int t_x = occ.getGridX();
    int t_y = occ.getGridY();
    int dx = 0;
    int dy = 0;
    if (t_x > grid_x) dx = 1;
    else if (t_x < grid_x) dx = -1;
    if (t_y > grid_y) dy = 1;
    else if (t_y < grid_y) dy = -1;
    assert dx != 0 || dy != 0 : "occ = " + occ;
    return unit_grid.getOccupant(grid_x + dx, grid_y + dy) == occ;
  }

  public static boolean isCloseEnough(UnitGrid unit_grid, float max_dist, int grid_x, int grid_y, Target target) {
    if (max_dist == 0f && target instanceof Occupant occupant) {
      return isAdjacent(unit_grid, grid_x, grid_y, occupant);
    } else {
      int dx = grid_x - target.getGridX();
      int dy = grid_y - target.getGridY();
      int dist_squared = dx * dx + dy * dy;
      float max_dist_squared = max_dist * max_dist;
      return dist_squared <= max_dist_squared;
    }
  }

  @Override
  public final void register() {
    super.register();
    owner.getWorld().getAnimationManagerGameTime().registerAnimation(this);
    enable();
  }

  public final void initTarget(@Nullable Target target, Action action, boolean aggressive) {
    assert !isDead();
    if (target != null) {
      setTarget(target, action, aggressive);
    }
  }

  protected abstract void setTarget(Target target, Action action, boolean aggressive);

  public abstract AttackScanFilter.Priority getAttackPriority();

  public abstract int getStatusValue();

  public final Abilities getAbilities() {
    return abilities;
  }

  @Override
  public final int getGridX() {
    return grid_x;
  }

  @Override
  public final int getGridY() {
    return grid_y;
  }

  public final void setGridPosition(int grid_x, int grid_y) {
    assert !isDead();
    assert owner.getWorld().getHeightMap().isGridInside(grid_x, grid_y)
        : grid_x + " " + grid_y + " " + this.grid_x + " " + this.grid_y;
    this.grid_x = grid_x;
    this.grid_y = grid_y;
  }

  public final Player getOwnerNoCheck() {
    return owner;
  }

  public final Player getOwner() {
    return getOwnerNoCheck();
  }

  @Override
  public void remove() {
    super.remove();
    owner.getWorld().getAnimationManagerGameTime().removeAnimation(this);
  }

  @Override
  public final void updateChecksum(StateChecksum checksum) {
    /*		checksum.update(getGridX());
    checksum.update(getGridY());
    checksum.update(getPositionX());
    checksum.update(getPositionY());*/
  }

  protected final void disable() {
    owner.getWorld().unregisterTarget(this);
    owner.getUnits().remove(this);
  }

  protected final void enable() {
    owner.getWorld().registerTarget(this);
    owner.getUnits().add(this);
  }

  public void removeDying() {
    if (getId() != Distributable.NO_ID) {
      disable();
    }
    dead = true;
  }

  @Override
  public final boolean isDead() {
    return dead;
  }

  public void hit(int damage, float direction_x, float direction_y, Player attacker) {
    if (owner.isEnemy(attacker)) owner.getWorld().getNotificationListener().newAttackNotification(this);
  }

  public static <T extends Template> Class<Selectable<T>> genericClass() {
    //noinspection unchecked
    return (Class<Selectable<T>>) (Class<?>) Selectable.class;
  }
}
