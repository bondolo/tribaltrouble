package com.oddlabs.tt.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.oddlabs.matchmaking.Game;
import com.oddlabs.net.JitterConfig;
import com.oddlabs.net.TickTimeManager;
import com.oddlabs.tt.ai.AdvancedAI;
import com.oddlabs.tt.ai.PassiveAI;
import com.oddlabs.tt.simulation.landscape.FlatLandscape;
import com.oddlabs.tt.simulation.landscape.Landscape;
import com.oddlabs.tt.simulation.landscape.WorldParameters;
import com.oddlabs.tt.simulation.model.Race;
import com.oddlabs.tt.simulation.player.AIDifficulty;
import com.oddlabs.tt.simulation.player.Player;
import com.oddlabs.tt.simulation.player.PlayerInfo;
import com.oddlabs.tt.simulation.player.PlayerSlot;
import com.oddlabs.tt.simulation.player.SlotType;
import com.oddlabs.tt.simulation.player.UnitInfo;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/// Integration tests for running multiple headless simulation instances in the same JVM process.
final class HeadlessMultiplayerHarnessTest {

  private static Landscape createTestLandscape(int size) {
    return createTestLandscape(size, 2);
  }

  private static Landscape createTestLandscape(int size, int numPlayers) {
    return FlatLandscape.create(size, FlatLandscape.generateSteppedStartingLocations(numPlayers, 20, 35.0f, 15.0f));
  }

  private static List<UnitInfo> createDefaultUnitInfos() {
    return List.of(new UnitInfo(false, false, 0, false, 5, 0, 0, 0), new UnitInfo(false, false, 0, false, 5, 0, 0, 0));
  }

  private static List<PlayerSlot> createTestSlots(
      AIDifficulty difficulty0, String name0, AIDifficulty difficulty1, String name1) {
    List<PlayerSlot> slots = new ArrayList<>(2);
    PlayerSlot slot0 = new PlayerSlot(0);
    slot0.setType(SlotType.AI);
    slot0.setAIDifficulty(difficulty0);
    slot0.setInfo(new PlayerInfo(0, Race.VIKINGS, name0));
    slot0.setNetworkPeer(true);
    slots.add(slot0);

    PlayerSlot slot1 = new PlayerSlot(1);
    slot1.setType(SlotType.AI);
    slot1.setAIDifficulty(difficulty1);
    slot1.setInfo(new PlayerInfo(1, Race.NATIVES, name1));
    slot1.setNetworkPeer(true);
    slots.add(slot1);
    return slots;
  }

  @Test
  void testMultiInstanceHeadlessSession() throws Exception {
    int gridSize = 64; // 2 patches of 16
    Landscape landscapeData = createTestLandscape(gridSize);
    WorldParameters worldParams = new WorldParameters("1", 5, 20);
    List<PlayerSlot> playerSlots =
        createTestSlots(AIDifficulty.NORMAL, "VikingPlayer", AIDifficulty.NORMAL, "NativePlayer");
    List<UnitInfo> unitInfos = createDefaultUnitInfos();

    try (HeadlessMultiplayerHarness harness =
        HeadlessMultiplayerHarness.create(landscapeData, worldParams, playerSlots, unitInfos)) {

      assertEquals(2, harness.getInstances().size());
      HeadlessSimulationInstance instance0 = harness.getInstance(0);
      HeadlessSimulationInstance instance1 = harness.getInstance(1);

      assertNotNull(instance0.getWorld());
      assertNotNull(instance1.getWorld());
      assertEquals(0, instance0.getPlayerIndex());
      assertEquals(1, instance1.getPlayerIndex());
      assertNotNull(instance0.getAI());
      assertNotNull(instance1.getAI());

      // Validate that both instances start with equal initial checksums
      assertEquals(instance0.getChecksum(), instance1.getChecksum(), "Initial checksums should match");

      // Attach rejected command observer across all players in all instances
      List<String> rejections = new java.util.concurrent.CopyOnWriteArrayList<>();
      for (HeadlessSimulationInstance instance : harness.getInstances()) {
        for (Player p : instance.getWorld().getPlayers()) {
          p.setCommandRejectedListener(
              (action, entityId, reason) -> rejections.add(action + ":" + entityId + ":" + reason));
        }
      }

      // Complete network session login handshake
      harness.awaitSynchronized(Duration.ofSeconds(5));
      assertTrue(instance0.isSynchronized(), "Instance 0 should be synchronized");
      assertTrue(instance1.isSynchronized(), "Instance 1 should be synchronized");

      // Verify ScopedValue binding is properly isolated
      assertFalse(PeerHub.CURRENT.isBound(), "PeerHub.CURRENT should not be bound outside instance execution");
      instance0.run(() -> {
        assertTrue(PeerHub.CURRENT.isBound());
        assertEquals(instance0.getPeerHub(), PeerHub.CURRENT.get());
        assertFalse(PeerHub.isWaitingForAck());
      });

      // Advance simulation in lockstep
      harness.runUntilTick(40, Duration.ofSeconds(10));
      assertTrue(instance0.getTick() >= 40, "Instance 0 should advance past tick 40");
      assertTrue(instance1.getTick() >= 40, "Instance 1 should advance past tick 40");
      harness.verifyChecksums();

      // Dispatch an action through standard game networking on Player 0's interface
      instance0.getPlayerInterface().setPreferredGamespeed(Game.GAMESPEED_NORMAL);

      // Continue lockstep ticking and verify that the command propagates deterministically
      harness.runUntilTick(80, Duration.ofSeconds(10));
      harness.verifyChecksums();
      assertEquals(instance0.getChecksum(), instance1.getChecksum(), "Checksums must match after network action");
      assertTrue(rejections.isEmpty(), "No commands should be rejected during clean headless session: " + rejections);
    }
  }

  @Test
  void testAiPeerHeadlessSession() throws Exception {
    int gridSize = 64;
    Landscape landscapeData = createTestLandscape(gridSize);
    WorldParameters worldParams = new WorldParameters("1", 5, 20);
    List<PlayerSlot> playerSlots = createTestSlots(AIDifficulty.EASY, "VikingBot1", AIDifficulty.NORMAL, "NativeBot2");
    List<UnitInfo> unitInfos = createDefaultUnitInfos();

    try (HeadlessMultiplayerHarness harness =
        HeadlessMultiplayerHarness.create(
            landscapeData, worldParams, playerSlots, unitInfos, com.oddlabs.net.TimeManager.DEFAULT)) {

      assertEquals(2, harness.getInstances().size());
      HeadlessSimulationInstance bot0 = harness.getInstance(0);
      HeadlessSimulationInstance bot1 = harness.getInstance(1);

      assertTrue(bot0.getAI() instanceof AdvancedAI);
      assertTrue(bot1.getAI() instanceof AdvancedAI);

      harness.awaitSynchronized(Duration.ofSeconds(5));
      assertTrue(bot0.isSynchronized());
      assertTrue(bot1.isSynchronized());

      // Run past the initial AI decision threshold (sleep_time is ~2-4s, i.e. 100-200 ticks at 50Hz)
      harness.runUntilTick(180, Duration.ofSeconds(15));
      harness.verifyChecksums();
      assertEquals(
          bot0.getChecksum(),
          bot1.getChecksum(),
          "Checksums must match after autonomous AI decisions and lockstep execution");
    }
  }

  @Test
  void testMultiInstanceHeadlessSessionWithJitter() throws Exception {
    int gridSize = 64;
    Landscape landscapeData = createTestLandscape(gridSize);
    WorldParameters worldParams = new WorldParameters("1", 5, 20);
    List<PlayerSlot> playerSlots =
        createTestSlots(AIDifficulty.NORMAL, "VikingJitter0", AIDifficulty.NORMAL, "NativeJitter1");
    List<UnitInfo> unitInfos = createDefaultUnitInfos();

    TickTimeManager baseClock = new TickTimeManager();
    JitterConfig jitterConfig = JitterConfig.heavy(baseClock, 777L);

    try (HeadlessMultiplayerHarness harness =
        HeadlessMultiplayerHarness.createJittered(landscapeData, worldParams, playerSlots, unitInfos, jitterConfig)) {

      assertEquals(2, harness.getInstances().size());
      HeadlessSimulationInstance instance0 = harness.getInstance(0);
      HeadlessSimulationInstance instance1 = harness.getInstance(1);

      assertEquals(instance0.getChecksum(), instance1.getChecksum());

      harness.awaitSynchronized(Duration.ofSeconds(5));
      assertTrue(instance0.isSynchronized());
      assertTrue(instance1.isSynchronized());

      harness.runUntilTick(40, Duration.ofSeconds(10));
      harness.verifyChecksums();

      instance0.getPlayerInterface().setPreferredGamespeed(Game.GAMESPEED_NORMAL);

      harness.runUntilTick(80, Duration.ofSeconds(10));
      harness.verifyChecksums();
      assertEquals(instance0.getChecksum(), instance1.getChecksum());
    }
  }

  @Test
  void testAiPeerHeadlessSessionWithLaggyJitter() throws Exception {
    int gridSize = 64;
    Landscape landscapeData = createTestLandscape(gridSize);
    WorldParameters worldParams = new WorldParameters("1", 5, 20);
    List<PlayerSlot> playerSlots =
        createTestSlots(AIDifficulty.EASY, "VikingLagBot", AIDifficulty.NORMAL, "NativeLagBot");
    List<UnitInfo> unitInfos = createDefaultUnitInfos();

    TickTimeManager baseClock = new TickTimeManager();
    JitterConfig laggyConfig = JitterConfig.laggy(baseClock, 10101L);

    try (HeadlessMultiplayerHarness harness =
        HeadlessMultiplayerHarness.createJittered(landscapeData, worldParams, playerSlots, unitInfos, laggyConfig)) {

      assertEquals(2, harness.getInstances().size());
      HeadlessSimulationInstance bot0 = harness.getInstance(0);
      HeadlessSimulationInstance bot1 = harness.getInstance(1);

      harness.awaitSynchronized(Duration.ofSeconds(5));
      assertTrue(bot0.isSynchronized());
      assertTrue(bot1.isSynchronized());

      harness.runUntilTick(140, Duration.ofSeconds(15));
      harness.verifyChecksums();
      assertEquals(
          bot0.getChecksum(), bot1.getChecksum(), "Checksums must match under laggy network jitter conditions");
    }
  }

  @Test
  void testMultiInstanceConcurrentVirtualThreadStepping() throws Exception {
    int gridSize = 64;
    Landscape landscapeData = createTestLandscape(gridSize);
    WorldParameters worldParams = new WorldParameters("1", 5, 20);
    List<PlayerSlot> playerSlots =
        createTestSlots(AIDifficulty.NORMAL, "VikingConcurrent0", AIDifficulty.NORMAL, "NativeConcurrent1");
    List<UnitInfo> unitInfos = createDefaultUnitInfos();

    try (HeadlessMultiplayerHarness harness =
        HeadlessMultiplayerHarness.createVirtual(landscapeData, worldParams, playerSlots, unitInfos)) {

      assertEquals(2, harness.getInstances().size());
      HeadlessSimulationInstance instance0 = harness.getInstance(0);
      HeadlessSimulationInstance instance1 = harness.getInstance(1);

      harness.awaitSynchronized(Duration.ofSeconds(5));
      assertTrue(instance0.isSynchronized());
      assertTrue(instance1.isSynchronized());

      harness.runUntilTickConcurrent(100, Duration.ofSeconds(10));
      assertTrue(instance0.getTick() >= 100);
      assertTrue(instance1.getTick() >= 100);
      assertTrue(harness.areTicksAligned());
      harness.verifyChecksums();
      assertEquals(
          instance0.getChecksum(),
          instance1.getChecksum(),
          "State checksums must match after concurrent virtual thread stepping");
    }
  }

  @Test
  void testMultiInstanceHeadlessSessionWithPassiveAiWandering() throws Exception {
    int gridSize = 64;
    Landscape landscapeData = createTestLandscape(gridSize);
    WorldParameters worldParams = new WorldParameters("1", 5, 20);
    List<PlayerSlot> playerSlots =
        createTestSlots(AIDifficulty.PASSIVE_CAMPAIGN, "VikingPassive", AIDifficulty.PASSIVE_CAMPAIGN, "NativePassive");
    List<UnitInfo> unitInfos = createDefaultUnitInfos();

    try (HeadlessMultiplayerHarness harness =
        HeadlessMultiplayerHarness.create(
            landscapeData, worldParams, playerSlots, unitInfos, com.oddlabs.net.TimeManager.DEFAULT)) {

      assertEquals(2, harness.getInstances().size());
      HeadlessSimulationInstance bot0 = harness.getInstance(0);
      HeadlessSimulationInstance bot1 = harness.getInstance(1);

      assertTrue(bot0.getAI() instanceof PassiveAI);
      assertTrue(bot1.getAI() instanceof PassiveAI);

      harness.awaitSynchronized(Duration.ofSeconds(5));
      assertTrue(bot0.isSynchronized());
      assertTrue(bot1.isSynchronized());

      harness.runUntilTick(100, Duration.ofSeconds(10));
      harness.verifyChecksums();
      assertEquals(
          bot0.getChecksum(),
          bot1.getChecksum(),
          "Checksums must match after PassiveAI roaming decisions and lockstep execution");
    }
  }

  @Test
  void testMixedThreePlayerSessionWithHumanAndAiPeersAndLockstepAi() throws Exception {
    int gridSize = 64;
    Landscape landscapeData = createTestLandscape(gridSize, 3);
    WorldParameters worldParams = new WorldParameters("1", 5, 20);

    List<PlayerSlot> playerSlots = new ArrayList<>(3);

    // Player 0: Scripted Human peer (isNetworkPeer = true)
    PlayerSlot slot0 = new PlayerSlot(0);
    slot0.setType(SlotType.HUMAN);
    slot0.setInfo(new PlayerInfo(0, Race.VIKINGS, "ScriptedPeer"));
    playerSlots.add(slot0);

    // Player 1: Networked AI peer (isNetworkPeer = true)
    PlayerSlot slot1 = new PlayerSlot(1);
    slot1.setType(SlotType.AI);
    slot1.setAIDifficulty(AIDifficulty.EASY);
    slot1.setNetworkPeer(true);
    slot1.setInfo(new PlayerInfo(1, Race.NATIVES, "NetworkAiPeer"));
    playerSlots.add(slot1);

    // Player 2: Local Lockstep AI (isNetworkPeer = false)
    PlayerSlot slot2 = new PlayerSlot(2);
    slot2.setType(SlotType.AI);
    slot2.setAIDifficulty(AIDifficulty.PASSIVE_CAMPAIGN);
    slot2.setNetworkPeer(false);
    slot2.setInfo(new PlayerInfo(2, Race.VIKINGS, "LocalLockstepAi"));
    playerSlots.add(slot2);

    List<UnitInfo> unitInfos =
        List.of(
            new UnitInfo(false, false, 0, false, 5, 0, 0, 0),
            new UnitInfo(true, true, 0, false, 5, 0, 0, 0),
            new UnitInfo(true, true, 0, false, 5, 0, 0, 0));

    try (HeadlessMultiplayerHarness harness =
        HeadlessMultiplayerHarness.create(landscapeData, worldParams, playerSlots, unitInfos)) {

      // Only the 2 network peers (Player 0 and Player 1) have participant simulation instances
      assertEquals(2, harness.getInstances().size());
      HeadlessSimulationInstance humanPeer = harness.getInstance(0);
      HeadlessSimulationInstance aiPeer = harness.getInstance(1);

      assertEquals(0, humanPeer.getPlayerIndex());
      assertEquals(1, aiPeer.getPlayerIndex());

      // Human peer has no attached AI controller; AI peer has an AdvancedAI controller
      assertNull(humanPeer.getAI());
      assertTrue(aiPeer.getAI() instanceof AdvancedAI);

      // Both instances must have Player 2 running a local lockstep PassiveAI controller
      assertTrue(humanPeer.getAI(humanPeer.getWorld().getPlayers().get(2)).isPresent());
      assertTrue(humanPeer.getAI(humanPeer.getWorld().getPlayers().get(2)).get() instanceof PassiveAI);
      assertTrue(aiPeer.getAI(aiPeer.getWorld().getPlayers().get(2)).isPresent());
      assertTrue(aiPeer.getAI(aiPeer.getWorld().getPlayers().get(2)).get() instanceof PassiveAI);

      harness.awaitSynchronized(Duration.ofSeconds(5));
      assertTrue(humanPeer.isSynchronized());
      assertTrue(aiPeer.isSynchronized());

      // Advance simulation in lockstep
      harness.runUntilTick(40, Duration.ofSeconds(10));
      harness.verifyChecksums();

      // Dispatch a command through the scripted human peer's interface over the network
      humanPeer.getPlayerInterface().setPreferredGamespeed(Game.GAMESPEED_NORMAL);

      // Continue lockstep ticking past tick 180
      harness.runUntilTick(180, Duration.ofSeconds(15));
      harness.verifyChecksums();
      assertEquals(
          humanPeer.getChecksum(),
          aiPeer.getChecksum(),
          "Checksums must match across scripted human peer, network AI peer, and local lockstep AI");
    }
  }

  @Test
  void testConfigurableTicksPerChecksum() throws Exception {
    Landscape landscape = createTestLandscape(128);
    WorldParameters worldParams = new WorldParameters("1", 5, 250);
    List<PlayerSlot> slots = createTestSlots(AIDifficulty.EASY, "BotA", AIDifficulty.EASY, "BotB");
    List<UnitInfo> unitInfos = createDefaultUnitInfos();

    assertThrows(
        IllegalArgumentException.class,
        () -> HeadlessMultiplayerHarness.create(landscape, worldParams, slots, unitInfos, new TickTimeManager(), 0));

    try (HeadlessMultiplayerHarness harness =
        HeadlessMultiplayerHarness.create(landscape, worldParams, slots, unitInfos, new TickTimeManager(), 1)) {
      assertEquals(1, harness.getInstance(0).getPeerHub().getTicksPerChecksum());
      assertEquals(1, harness.getInstance(1).getPeerHub().getTicksPerChecksum());

      harness.awaitSynchronized(Duration.ofSeconds(5));
      harness.runUntilTick(20, Duration.ofSeconds(10));
      harness.verifyChecksums();
    }
  }

  @Test
  void testVerifyChecksumsReportsMerkleDiffOnSemanticDesync() throws Exception {
    Landscape landscape = createTestLandscape(64);
    WorldParameters worldParams = new WorldParameters("1", 5, 20);
    List<PlayerSlot> slots = createTestSlots(AIDifficulty.EASY, "BotA", AIDifficulty.EASY, "BotB");
    List<UnitInfo> unitInfos = createDefaultUnitInfos();

    try (HeadlessMultiplayerHarness harness =
        HeadlessMultiplayerHarness.create(landscape, worldParams, slots, unitInfos)) {
      harness.awaitSynchronized(Duration.ofSeconds(5));
      harness.runUntilTick(20, Duration.ofSeconds(10));
      harness.verifyChecksums();

      // Advance only instance 1's PRNG without altering the legacy world checksum
      harness.getInstance(1).getWorld().getRandom().nextInt();

      AssertionError error = assertThrows(AssertionError.class, harness::verifyChecksums);
      assertTrue(
          error.getMessage().contains("subsystems.clock.randomSeed"),
          "Expected Merkle diff to pinpoint PRNG divergence, got: " + error.getMessage());
    }
  }
}
