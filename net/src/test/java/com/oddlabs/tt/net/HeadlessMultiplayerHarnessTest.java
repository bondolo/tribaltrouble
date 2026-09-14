package com.oddlabs.tt.net;

import com.oddlabs.matchmaking.Game;
import com.oddlabs.tt.simulation.landscape.HeightMap;
import com.oddlabs.tt.simulation.landscape.LandscapeData;
import com.oddlabs.tt.simulation.landscape.WorldParameters;
import com.oddlabs.tt.simulation.model.Race;
import com.oddlabs.tt.simulation.model.Terrain;
import com.oddlabs.tt.simulation.player.PlayerInfo;
import com.oddlabs.tt.simulation.player.PlayerSlot;
import com.oddlabs.tt.simulation.player.UnitInfo;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for running multiple headless simulation instances in the same JVM process.
 */
class HeadlessMultiplayerHarnessTest {

    private record TestLandscapeData(
                                     int size,
                                     float[] heightmap,
                                     boolean[][] accessGrid,
                                     byte[][] buildGrid,
                                     float[][] startingLocations) implements LandscapeData {

        static TestLandscapeData create(int size) {
            float[] heightmap = new float[size * size];
            Arrays.fill(heightmap, 5.0f);

            boolean[][] accessGrid = new boolean[size][size];
            for (boolean[] row : accessGrid) {
                Arrays.fill(row, true);
            }

            byte[][] buildGrid = new byte[size][size];

            float[][] startingLocations = new float[2][40];
            for (int i = 0; i < 20; i++) {
                startingLocations[0][2 * i] = 10.0f + (i % 5) * 1.5f;
                startingLocations[0][2 * i + 1] = 10.0f + (i / 5f) * 1.5f;

                startingLocations[1][2 * i] = 40.0f + (i % 5) * 1.5f;
                startingLocations[1][2 * i + 1] = 40.0f + (i / 5f) * 1.5f;
            }

            return new TestLandscapeData(size, heightmap, accessGrid, buildGrid, startingLocations);
        }

        @Override
        public Terrain terrain() {
            return Terrain.NATIVE;
        }

        @Override
        public int metersPerWorld() {
            return size * HeightMap.METERS_PER_UNIT_GRID;
        }

        @Override
        public float seaLevelMeters() {
            return 0.0f;
        }

        @Override
        public List<int[]> trees() {
            return List.of();
        }

        @Override
        public List<int[]> palmTrees() {
            return List.of();
        }

        @Override
        public List<int[]> rocks() {
            return List.of();
        }

        @Override
        public List<int[]> iron() {
            return List.of();
        }

        @Override
        public float[][] plants() {
            return new float[0][0];
        }
    }

    @Test
    void testMultiInstanceHeadlessSession() throws Exception {
        int gridSize = 32; // 2 patches of 16
        LandscapeData landscapeData = TestLandscapeData.create(gridSize);
        WorldParameters worldParams = new WorldParameters(Game.GAMESPEED_FAST, "1", 5, 20);

        PlayerSlot[] playerSlots = new PlayerSlot[2];
        playerSlots[0] = new PlayerSlot(0);
        playerSlots[0].setType(PlayerSlot.HUMAN);
        playerSlots[0].setInfo(new PlayerInfo(0, Race.VIKINGS, "VikingPlayer"));

        playerSlots[1] = new PlayerSlot(1);
        playerSlots[1].setType(PlayerSlot.HUMAN);
        playerSlots[1].setInfo(new PlayerInfo(1, Race.NATIVES, "NativePlayer"));

        UnitInfo[] unitInfos = new UnitInfo[2];
        unitInfos[0] = new UnitInfo(false, false, 0, false, 5, 0, 0, 0);
        unitInfos[1] = new UnitInfo(false, false, 0, false, 5, 0, 0, 0);

        try (HeadlessMultiplayerHarness harness = HeadlessMultiplayerHarness.create(
                landscapeData,
                worldParams,
                playerSlots,
                unitInfos)) {

            assertEquals(2, harness.getInstances().size());
            HeadlessSimulationInstance instance0 = harness.getInstance(0);
            HeadlessSimulationInstance instance1 = harness.getInstance(1);

            assertNotNull(instance0.getWorld());
            assertNotNull(instance1.getWorld());
            assertEquals(0, instance0.getPlayerIndex());
            assertEquals(1, instance1.getPlayerIndex());

            // Validate that both instances start with equal initial checksums
            assertEquals(instance0.getChecksum(), instance1.getChecksum(), "Initial checksums should match");

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
        }
    }

    @Test
    void testAiPeerHeadlessSession() throws Exception {
        int gridSize = 32;
        LandscapeData landscapeData = TestLandscapeData.create(gridSize);
        WorldParameters worldParams = new WorldParameters(Game.GAMESPEED_FAST, "1", 5, 20);

        PlayerSlot[] playerSlots = new PlayerSlot[2];
        playerSlots[0] = new PlayerSlot(0);
        playerSlots[0].setType(PlayerSlot.HUMAN);
        playerSlots[0].setInfo(new PlayerInfo(0, Race.VIKINGS, "VikingHuman"));

        playerSlots[1] = new PlayerSlot(1);
        playerSlots[1].setType(PlayerSlot.AI);
        playerSlots[1].setAIDifficulty(PlayerSlot.AI_NORMAL);
        playerSlots[1].setInfo(new PlayerInfo(1, Race.NATIVES, "NativeBot"));

        UnitInfo[] unitInfos = new UnitInfo[2];
        unitInfos[0] = new UnitInfo(false, false, 0, false, 5, 0, 0, 0);
        unitInfos[1] = new UnitInfo(false, false, 0, false, 5, 0, 0, 0);

        try (HeadlessMultiplayerHarness harness = HeadlessMultiplayerHarness.create(
                landscapeData,
                worldParams,
                playerSlots,
                unitInfos,
                com.oddlabs.net.TimeManager.DEFAULT,
                true)) {

            assertEquals(2, harness.getInstances().size());
            HeadlessSimulationInstance humanInstance = harness.getInstance(0);
            HeadlessSimulationInstance botInstance = harness.getInstance(1);

            harness.awaitSynchronized(Duration.ofSeconds(5));
            assertTrue(humanInstance.isSynchronized());
            assertTrue(botInstance.isSynchronized());

            harness.runUntilTick(50, Duration.ofSeconds(10));
            harness.verifyChecksums();
            assertEquals(humanInstance.getChecksum(), botInstance.getChecksum());
        }
    }
}
