package com.oddlabs.tt.ai;

import com.oddlabs.tt.base.global.AppConfig;
import com.oddlabs.tt.simulation.model.Race;
import com.oddlabs.tt.simulation.model.Selectable;
import com.oddlabs.tt.simulation.model.Unit;
import com.oddlabs.tt.simulation.pathfinder.CountOccupantScanFilter;
import com.oddlabs.tt.simulation.pathfinder.FindOccupantFilter;
import com.oddlabs.tt.simulation.player.Player;
import com.oddlabs.tt.simulation.player.PlayerInterface;

/// Base decision-making capabilities and utility computations for player chieftains.
public abstract sealed class ChieftainAI permits NativeChieftainAI, VikingChieftainAI {
  private static final class ChieftainAIHolder {
    private static final ChieftainAI VIKING_AI = new VikingChieftainAI();
    private static final ChieftainAI NATIVE_AI = new NativeChieftainAI();
  }

  public static ChieftainAI forRace(Race race) {
    return switch (race) {
      case VIKINGS -> ChieftainAIHolder.VIKING_AI;
      case NATIVES -> ChieftainAIHolder.NATIVE_AI;
    };
  }

  public abstract void decide(Unit chieftain, PlayerInterface playerInterface);

  protected final int numEnemyUnits(Player owner) {
    int count = 0;
    for (Player player : owner.getWorld().getPlayers()) {
      if (owner.isEnemy(player)) {
        count += player.getUnits().size();
      }
    }
    return count;
  }

  protected final int getNumEnemyUnitsClose(Unit chieftain, float hitRadius) {
    return getNumEnemyUnitsClose(chieftain, hitRadius, Unit.class);
  }

  protected final <S extends Selectable<?>> int getNumEnemyUnitsClose(Unit chieftain, float hitRadius, Class<S> type) {
    if (AppConfig.LEGACY_DETERMINISTIC) {
      FindOccupantFilter<S> filter =
          new FindOccupantFilter<>(chieftain.getPositionX(), chieftain.getPositionY(), hitRadius, chieftain, type);
      chieftain.getUnitGrid().scan(filter, chieftain.getGridX(), chieftain.getGridY());
      int numEnemyUnitsClose = 0;
      for (Selectable<?> s : filter.getResult()) {
        if (s.isDead()) {
          continue;
        }

        float dx = s.getPositionX() - chieftain.getPositionX();
        float dy = s.getPositionY() - chieftain.getPositionY();
        float squaredDist = dx * dx + dy * dy;
        if (chieftain.getOwner().isEnemy(s.getOwner()) && squaredDist < hitRadius * hitRadius) {
          numEnemyUnitsClose++;
        }
      }
      return numEnemyUnitsClose;
    }
    CountOccupantScanFilter<S> filter =
        new CountOccupantScanFilter<>(
            chieftain.getPositionX(),
            chieftain.getPositionY(),
            hitRadius,
            chieftain,
            type,
            s -> s.isAlive() && chieftain.getOwner().isEnemy(s.getOwner()));
    chieftain.getUnitGrid().scan(filter, chieftain.getGridX(), chieftain.getGridY());
    return filter.getCount();
  }

  protected final int getNumFriendlyUnitsClose(Unit chieftain, float hitRadius) {
    if (AppConfig.LEGACY_DETERMINISTIC) {
      FindOccupantFilter<Selectable<?>> filter =
          new FindOccupantFilter<>(
              chieftain.getPositionX(), chieftain.getPositionY(), hitRadius, chieftain, Selectable.genericClass());
      chieftain.getUnitGrid().scan(filter, chieftain.getGridX(), chieftain.getGridY());
      int numFriendlyUnitsClose = 0;
      for (Selectable<?> s : filter.getResult()) {
        if (s.isDead()) {
          continue;
        }

        float dx = s.getPositionX() - chieftain.getPositionX();
        float dy = s.getPositionY() - chieftain.getPositionY();
        float squaredDist = dx * dx + dy * dy;
        if (!chieftain.getOwner().isEnemy(s.getOwner()) && squaredDist < hitRadius * hitRadius) {
          numFriendlyUnitsClose++;
        }
      }
      return numFriendlyUnitsClose;
    }
    var filter =
        new CountOccupantScanFilter<>(
            chieftain.getPositionX(),
            chieftain.getPositionY(),
            hitRadius,
            chieftain,
            Selectable.genericClass(),
            s -> s.isAlive() && !chieftain.getOwner().isEnemy(s.getOwner()));
    chieftain.getUnitGrid().scan(filter, chieftain.getGridX(), chieftain.getGridY());
    return filter.getCount();
  }
}
