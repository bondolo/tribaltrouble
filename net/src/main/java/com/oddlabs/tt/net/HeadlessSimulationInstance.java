package com.oddlabs.tt.net;

import com.oddlabs.router.SessionID;
import com.oddlabs.tt.ai.AI;
import com.oddlabs.tt.base.animation.AnimationManager;
import com.oddlabs.tt.simulation.landscape.Landscape;
import com.oddlabs.tt.simulation.landscape.World;
import com.oddlabs.tt.simulation.landscape.WorldParameters;
import com.oddlabs.tt.simulation.player.DelegatingPlayerInterface;
import com.oddlabs.tt.simulation.player.Player;
import com.oddlabs.tt.simulation.player.PlayerConfig;
import com.oddlabs.tt.simulation.player.PlayerControllerType;
import com.oddlabs.tt.simulation.player.PlayerInfo;
import com.oddlabs.tt.simulation.player.PlayerInterface;
import com.oddlabs.tt.simulation.player.PlayerSlot;
import com.oddlabs.tt.simulation.player.SlotType;
import com.oddlabs.tt.simulation.player.UnitInfo;
import com.oddlabs.util.Color;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/// Isolated headless simulation instance managing a local world and peer network endpoint.
public final class HeadlessSimulationInstance implements AutoCloseable {
  public static final Color.Linear[] DEFAULT_PLAYER_COLORS =
      Arrays.stream(
              new Color.Standard[] {
                new Color.Standard(0xFFFFBF00), /* Orange */
                new Color.Standard(0xFF007FFF), /* Royal Blue */
                new Color.Standard(0xFFFF0040), /* Red */
                new Color.Standard(0xFF00FFBF), /* Teal */
                new Color.Standard(0xFFBF00FF), /* Purple */
                new Color.Standard(0xFFBFFF00) /* Lime */
              })
          .map(Color.Linear::new)
          .toArray(Color.Linear[]::new);

  private final int playerIndex;
  private final World world;
  private final Player localPlayer;
  private final @Nullable AI ai;
  private final Map<Player, AI> lockstepAis;
  private final AnimationManager animationManager;
  private final PeerHub peerHub;

  public HeadlessSimulationInstance(
      int playerIndex,
      World world,
      @Nullable AI ai,
      Map<Player, AI> lockstepAis,
      AnimationManager animationManager,
      PeerHub peerHub) {
    this.playerIndex = playerIndex;
    this.world = world;
    this.localPlayer = world.getPlayers().get(playerIndex);
    this.ai = ai;
    this.lockstepAis = Map.copyOf(lockstepAis);
    this.animationManager = animationManager;
    this.peerHub = peerHub;
  }

  public HeadlessSimulationInstance(
      int playerIndex, World world, @Nullable AI ai, AnimationManager animationManager, PeerHub peerHub) {
    this(playerIndex, world, ai, Map.of(), animationManager, peerHub);
  }

  /// Creates and initializes a headless simulation instance for the specified player slot.
  public static HeadlessSimulationInstance create(
      int playerIndex,
      WorldParameters worldParams,
      Landscape landscape,
      List<PlayerSlot> playerSlots,
      List<UnitInfo> unitInfos,
      com.oddlabs.net.NetworkSelector network,
      SessionID sessionId,
      @Nullable String routerAddress,
      int routerPort) {
    return create(
        playerIndex,
        worldParams,
        landscape,
        playerSlots,
        unitInfos,
        network,
        sessionId,
        routerAddress,
        routerPort,
        PeerHub.DEFAULT_TICKS_PER_CHECKSUM);
  }

  /// Creates and initializes a headless simulation instance with an explicit checksum interval.
  public static HeadlessSimulationInstance create(
      int playerIndex,
      WorldParameters worldParams,
      Landscape landscape,
      List<PlayerSlot> playerSlots,
      List<UnitInfo> unitInfos,
      com.oddlabs.net.NetworkSelector network,
      SessionID sessionId,
      @Nullable String routerAddress,
      int routerPort,
      int ticksPerChecksum) {
    List<PlayerInfo> playerInfos = playerSlots.stream().map(PlayerSlot::getInfo).toList();

    final PeerHub[] peerHubRef = new PeerHub[1];
    List<PlayerConfig> playerConfigs = new java.util.ArrayList<>(playerInfos.size());
    for (int i = 0; i < playerInfos.size(); i++) {
      PlayerInterface iface =
          (i == playerIndex && playerSlots.get(i).isNetworkPeer())
              ? new DelegatingPlayerInterface(() -> peerHubRef[0].getPlayerInterface())
              : null;
      playerConfigs.add(
          new PlayerConfig(
              playerInfos.get(i),
              DEFAULT_PLAYER_COLORS[i % DEFAULT_PLAYER_COLORS.length],
              iface,
              PlayerControllerType.fromSlot(playerSlots.get(i)),
              unitInfos.get(i),
              null));
    }

    World world = new World(worldParams, landscape, playerConfigs);

    Player localPlayer = world.getPlayers().get(playerIndex);
    AnimationManager animManager = new AnimationManager();
    StallHandler stallHandler =
        new StallHandler() {
          @Override
          public void stopStall() {}

          @Override
          public void processStall(int tick) {}

          @Override
          public void peerhubFailed() {}
        };

    PeerHub peerHub =
        new PeerHub(
            animManager,
            true,
            false,
            localPlayer,
            playerSlots,
            network,
            null,
            null,
            null,
            world.getDistributableTable(),
            sessionId,
            stallHandler,
            routerAddress,
            routerPort,
            ticksPerChecksum);
    peerHubRef[0] = peerHub;

    PlayerSlot localSlot = playerSlots.get(playerIndex);
    if (!localSlot.isNetworkPeer()) {
      throw new IllegalArgumentException(
          "Headless simulation instances require a network peer slot (got non-peer slot for player "
              + playerIndex
              + ")");
    }

    AI ai = null;
    if (localSlot.getType() == SlotType.AI) {
      ai = AI.create(localPlayer, localSlot.getAIDifficulty());
      if (ai != null) {
        world.addTickListener(ai::tick);
      }
    }

    Map<Player, AI> lockstepAis = new LinkedHashMap<>();
    for (int k = 0; k < playerSlots.size(); k++) {
      if (k == playerIndex) {
        continue;
      }
      PlayerSlot slotK = playerSlots.get(k);
      if (!slotK.isNetworkPeer() && slotK.getType() == SlotType.AI) {
        Player otherPlayer = world.getPlayers().get(k);
        AI lockstepAi = AI.create(otherPlayer, slotK.getAIDifficulty());
        if (lockstepAi != null) {
          lockstepAis.put(otherPlayer, lockstepAi);
          world.addTickListener(lockstepAi::tick);
        }
      }
    }

    return new HeadlessSimulationInstance(playerIndex, world, ai, lockstepAis, animManager, peerHub);
  }

  public int getPlayerIndex() {
    return playerIndex;
  }

  public World getWorld() {
    return world;
  }

  public Player getLocalPlayer() {
    return localPlayer;
  }

  public @Nullable AI getAI() {
    return ai;
  }

  public Optional<AI> getAI(Player player) {
    if (player.equals(localPlayer)) {
      return Optional.ofNullable(ai);
    }
    return Optional.ofNullable(lockstepAis.get(player));
  }

  public PeerHub getPeerHub() {
    return peerHub;
  }

  public PlayerInterface getPlayerInterface() {
    return peerHub.getPlayerInterface();
  }

  public AnimationManager getAnimationManager() {
    return animationManager;
  }

  public int getTick() {
    return world.getTick();
  }

  public int getChecksum() {
    return world.getChecksum();
  }

  public boolean isSynchronized() {
    return peerHub.isSynchronized();
  }

  public boolean isAlive() {
    return peerHub.isAlive(localPlayer);
  }

  public void animate(float dt) {
    if (PeerHub.CURRENT.isBound() && PeerHub.CURRENT.get() == peerHub) {
      animationManager.runAnimations(dt);
    } else {
      ScopedValue.where(PeerHub.CURRENT, peerHub).run(() -> animationManager.runAnimations(dt));
    }
  }

  /// Executes an arbitrary action within the scope of this instance's active PeerHub.
  public void run(Runnable action) {
    ScopedValue.where(PeerHub.CURRENT, peerHub).run(action);
  }

  @Override
  public void close() {
    peerHub.close();
  }
}
