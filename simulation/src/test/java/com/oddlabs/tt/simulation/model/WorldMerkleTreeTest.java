package com.oddlabs.tt.simulation.model;

import com.oddlabs.tt.simulation.landscape.FlatLandscape;
import com.oddlabs.tt.simulation.landscape.Landscape;
import com.oddlabs.tt.simulation.landscape.SpawnLocation;
import com.oddlabs.tt.simulation.landscape.World;
import com.oddlabs.tt.simulation.landscape.WorldParameters;
import com.oddlabs.tt.simulation.model.WorldMerkleTree.BuildingNode;
import com.oddlabs.tt.simulation.model.WorldMerkleTree.Difference;
import com.oddlabs.tt.simulation.model.WorldMerkleTree.Murmur3Hasher;
import com.oddlabs.tt.simulation.model.WorldMerkleTree.PlayerNode;
import com.oddlabs.tt.simulation.model.WorldMerkleTree.UnitNode;
import com.oddlabs.tt.simulation.player.Player;
import com.oddlabs.tt.simulation.player.PlayerConfig;
import com.oddlabs.tt.simulation.player.PlayerControllerType;
import com.oddlabs.tt.simulation.player.PlayerInfo;
import com.oddlabs.tt.simulation.player.UnitInfo;
import com.oddlabs.util.Color;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Unit tests for {@link WorldMerkleTree} and {@link Murmur3Hasher}.
@NullMarked
final class WorldMerkleTreeTest {

    @Test
    void testMurmur3DeterminismAndAvalanche() {
        Murmur3Hasher hasherA = new Murmur3Hasher();
        Murmur3Hasher hasherB = new Murmur3Hasher();

        hasherA.putInt(42);
        hasherA.putLong(0x123456789ABCDEF0L);
        hasherA.putFloat(3.5f);
        hasherA.putBoolean(true);
        hasherA.putEnum(UnitType.PEON);
        hasherA.putString("IdleController");

        hasherB.putInt(42);
        hasherB.putLong(0x123456789ABCDEF0L);
        hasherB.putFloat(3.5f);
        hasherB.putBoolean(true);
        hasherB.putEnum(UnitType.PEON);
        hasherB.putString("IdleController");

        long digestA = hasherA.digest();
        long digestB = hasherB.digest();
        assertEquals(digestA, digestB, "Identical inputs must produce identical digests");

        hasherB.reset(0L);
        hasherB.putInt(43);
        hasherB.putLong(0x123456789ABCDEF0L);
        hasherB.putFloat(3.5f);
        hasherB.putBoolean(true);
        hasherB.putEnum(UnitType.PEON);
        hasherB.putString("IdleController");

        long flippedDigest = hasherB.digest();
        assertNotEquals(digestA, flippedDigest);
        int bitDiff = Long.bitCount(digestA ^ flippedDigest);
        assertTrue(bitDiff >= 16, "Expected strong avalanche (>=16 bits changed), got " + bitDiff);

        long h0 = Murmur3Hasher.fmix64(0L);
        long h1 = Murmur3Hasher.fmix64(1L);
        assertNotEquals(h0, h1);
        assertTrue(Long.bitCount(h0 ^ h1) >= 16, "fmix64 should avalanche on 1-bit input delta");

        hasherA.reset(0L);
        hasherA.putString(null);
        long nullStrHash = hasherA.digest();
        hasherA.reset(0L);
        hasherA.putString("");
        long emptyStrHash = hasherA.digest();
        hasherA.reset(0L);
        hasherA.putString("abcd");
        long len4StrHash = hasherA.digest();
        hasherA.reset(0L);
        hasherA.putString("abcde");
        long len5StrHash = hasherA.digest();

        assertNotEquals(nullStrHash, emptyStrHash);
        assertNotEquals(emptyStrHash, len4StrHash);
        assertNotEquals(len4StrHash, len5StrHash);
    }

    @Test
    void testIdenticalWorldsProduceIdenticalTreesAndEmptyDiff() {
        World worldA = createSeededWorld();
        World worldB = createSeededWorld();

        for (int i = 0; i < 25; i++) {
            worldA.tick();
            worldB.tick();
        }

        WorldMerkleTree treeA = WorldMerkleTree.capture(worldA);
        WorldMerkleTree treeB = WorldMerkleTree.capture(worldB);

        assertEquals(treeA.rootHash(), treeB.rootHash());
        assertEquals(treeA.tick(), treeB.tick());
        assertEquals(treeA.subsystems().hash(), treeB.subsystems().hash());
        assertTrue(WorldMerkleTree.diff(treeA, treeB).isEmpty());
    }

    @Test
    void testMutatingSingleUnitPinpointsDivergenceAndPreservesUntouchedSubtrees() {
        World world = createSeededWorld();
        for (int i = 0; i < 5; i++) {
            world.tick();
        }

        WorldMerkleTree before = WorldMerkleTree.capture(world);

        Player player0 = world.getPlayers().getFirst();
        List<Unit> player0Units = player0.getUnits().getSet().stream()
                .filter(Unit.class::isInstance)
                .map(Unit.class::cast)
                .toList();
        assertTrue(player0Units.size() >= 2, "Expected at least 2 unmounted units in player 0");

        Unit targetUnit = player0Units.getFirst();
        Unit untouchedUnit = player0Units.get(1);
        int targetId = targetUnit.getId();
        int untouchedId = untouchedUnit.getId();

        targetUnit.stun(1.5f);

        WorldMerkleTree after = WorldMerkleTree.capture(world);

        assertNotEquals(before.rootHash(), after.rootHash());
        assertEquals(before.subsystems().hash(), after.subsystems().hash(),
                "Subsystems hash should remain unchanged when only unit controller/activity changes");
        assertEquals(before.players().get(1).hash(), after.players().get(1).hash(),
                "Untouched player 1 subtree hash must remain identical");

        PlayerNode p0Before = before.players().getFirst();
        PlayerNode p0After = after.players().getFirst();
        assertEquals(p0Before.header().hash(), p0After.header().hash());
        assertEquals(p0Before.buildingsHash(), p0After.buildingsHash());

        UnitNode untouchedBefore = findUnit(p0Before, untouchedId);
        UnitNode untouchedAfter = findUnit(p0After, untouchedId);
        assertEquals(untouchedBefore.hash(), untouchedAfter.hash(),
                "Untouched sibling unit hash must remain identical");

        List<Difference> diffs = WorldMerkleTree.diff(before, after);
        assertFalse(diffs.isEmpty());
        String expectedPrefix = "players[0].units[" + targetId + "].";
        assertTrue(diffs.stream().allMatch(d -> d.path().startsWith(expectedPrefix)),
                "All diffs should point to mutated unit " + targetId + ", got: " + diffs);
        assertTrue(diffs.stream().anyMatch(d -> d.path().equals(expectedPrefix + "activity")
                && d.expectedValue().equals(EntityActivity.IDLE.name())
                && d.actualValue().equals(EntityActivity.STUNNED.name())),
                "Expected activity diff from IDLE to STUNNED, got: " + diffs);
        assertTrue(diffs.stream().anyMatch(d -> d.path().equals(expectedPrefix + "currentController")
                && d.actualValue().equals("Stunned")),
                "Expected currentController diff to Stunned, got: " + diffs);
    }

    @Test
    void testMountedUnitAndBuildingStateAreCaptured() {
        World world = createSeededWorld();
        Player player0 = world.getPlayers().getFirst();
        Building tower = player0.getUnits().getSet().stream()
                .filter(Building.class::isInstance)
                .map(Building.class::cast)
                .filter(b -> b.getTemplate().getBuildingType() == BuildingType.TOWER)
                .findFirst()
                .orElseThrow();
        Unit warrior = new Unit(player0, 30f, 30f, null,
                player0.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_ROCK));
        UnitContainer container = tower.getRawUnitContainer();
        assertTrue(container instanceof MountUnitContainer);
        container.enter(warrior);

        WorldMerkleTree before = WorldMerkleTree.capture(world);
        PlayerNode p0Before = before.players().getFirst();
        UnitNode mountedNode = p0Before.units().stream()
                .filter(u -> u.mounted() && u.mountedBuildingId() == tower.getId())
                .findFirst()
                .orElseThrow();
        assertFalse(mountedNode.shouldDecide());
        assertEquals("INTERRUPTIBLE", mountedNode.lastBehaviourState());
        BuildingNode towerNode = p0Before.buildings().stream()
                .filter(b -> b.entityId() == tower.getId())
                .findFirst()
                .orElseThrow();
        assertEquals(EntityActivity.IDLE, towerNode.activity());
        assertEquals(1, towerNode.garrisonedUnits());

        Unit untouched = p0Before.units().stream()
                .filter(u -> !u.mounted())
                .findFirst()
                .map(u -> (Unit) world.getDistributableTable().getDistributable(u.entityId()))
                .orElseThrow();
        int prevHp = untouched.getHitPoints();
        untouched.hit(1, 1f, 0f, world.getPlayers().get(1));

        WorldMerkleTree after = WorldMerkleTree.capture(world);
        List<Difference> diffs = WorldMerkleTree.diff(before, after);
        assertTrue(diffs.stream().anyMatch(d -> d.path().equals("players[0].units[" + untouched.getId() + "].hitPoints")
                && d.expectedValue().equals(Integer.toString(prevHp))
                && d.actualValue().equals(Integer.toString(prevHp - 1))),
                "Expected hitPoints diff on hit unit, got: " + diffs);
    }

    private static UnitNode findUnit(PlayerNode player, int entityId) {
        return player.units().stream()
                .filter(u -> u.entityId() == entityId)
                .findFirst()
                .orElseThrow();
    }

    private static World createSeededWorld() {
        int gridSize = 64;
        List<List<SpawnLocation>> spawns = List.of(
                List.of(new SpawnLocation(24f, 24f)),
                List.of(new SpawnLocation(96f, 96f))
        );
        Landscape landscape = FlatLandscape.create(gridSize, spawns);
        WorldParameters worldParams = new WorldParameters("1", 5, 20);
        UnitInfo unitInfo = new UnitInfo(true, true, 1, true, 2, 1, 0, 0);

        List<PlayerConfig> configs = List.of(
                new PlayerConfig(
                        new PlayerInfo(0, Race.VIKINGS, "VikingPlayer"),
                        new Color.Linear(1f, 0f, 0f, 1f),
                        null,
                        PlayerControllerType.AI_STANDARD,
                        unitInfo,
                        null
                ),
                new PlayerConfig(
                        new PlayerInfo(1, Race.NATIVES, "NativePlayer"),
                        new Color.Linear(0f, 0f, 1f, 1f),
                        null,
                        PlayerControllerType.AI_STANDARD,
                        unitInfo,
                        null
                )
        );
        return new World(worldParams, landscape, configs);
    }
}
