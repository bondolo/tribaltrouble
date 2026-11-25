package com.oddlabs.tt.simulation.model;

import com.oddlabs.tt.simulation.landscape.HeightMap;
import com.oddlabs.tt.simulation.landscape.RandomState;
import com.oddlabs.tt.simulation.landscape.World;
import com.oddlabs.tt.simulation.pathfinder.Occupant;
import com.oddlabs.tt.simulation.pathfinder.UnitGrid;
import com.oddlabs.tt.simulation.player.Player;
import com.oddlabs.tt.simulation.state.UnitState;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/// Hierarchical semantic state hash tree over deterministic simulation state for desync isolation.
public record WorldMerkleTree(long rootHash, int tick, SubsystemsNode subsystems, List<PlayerNode> players) {
  private static final Comparator<SupplyNode> SUPPLY_ORDER = Comparator.comparingInt(SupplyNode::entityId);
  private static final Comparator<UnitNode> UNIT_ORDER =
      Comparator.comparingInt(UnitNode::entityId).thenComparingInt(UnitNode::mountedBuildingId);
  private static final Comparator<BuildingNode> BUILDING_ORDER = Comparator.comparingInt(BuildingNode::entityId);

  public WorldMerkleTree {
    players = List.copyOf(players);
  }

  /// Zero-allocation 64-bit Murmur3 primitive word hasher.
  public static final class Murmur3Hasher {
    private static final long C1 = 0x87c37b91114253d5L;
    private static final long C2 = 0x4cf5ad432745937fL;
    private static final long H1_ADDEND = 0x52dce729L;
    private static final long FMIX_C1 = 0xff51afd7ed558ccdL;
    private static final long FMIX_C2 = 0xc4ceb9fe1a85ec53L;

    private long h1;
    private long length;

    public Murmur3Hasher() {
      this(0L);
    }

    public Murmur3Hasher(long seed) {
      this.h1 = seed;
    }

    public void reset(long seed) {
      this.h1 = seed;
      this.length = 0L;
    }

    public static long mixK1(long k1) {
      k1 *= C1;
      k1 = Long.rotateLeft(k1, 31);
      k1 *= C2;
      return k1;
    }

    public static long mixH1(long h1, long k1) {
      h1 ^= mixK1(k1);
      h1 = Long.rotateLeft(h1, 27);
      h1 = h1 * 5L + H1_ADDEND;
      return h1;
    }

    public static long fmix64(long k) {
      k ^= k >>> 33;
      k *= FMIX_C1;
      k ^= k >>> 33;
      k *= FMIX_C2;
      k ^= k >>> 33;
      return k;
    }

    public void putLong(long value) {
      h1 = mixH1(h1, value);
      length += Long.BYTES;
    }

    public void putInt(int value) {
      putLong(Integer.toUnsignedLong(value));
    }

    public void putFloat(float value) {
      putInt(Float.floatToRawIntBits(value));
    }

    public void putDouble(double value) {
      putLong(Double.doubleToRawLongBits(value));
    }

    public void putBoolean(boolean value) {
      putLong(value ? 1L : 0L);
    }

    public void putEnum(@Nullable Enum<?> value) {
      putInt(value != null ? value.ordinal() : -1);
    }

    public void putString(@Nullable String value) {
      if (value == null) {
        putInt(-1);
        return;
      }
      int len = value.length();
      putInt(len);
      int i = 0;
      while (i + 4 <= len) {
        long word =
            ((long) value.charAt(i))
                | (((long) value.charAt(i + 1)) << 16)
                | (((long) value.charAt(i + 2)) << 32)
                | (((long) value.charAt(i + 3)) << 48);
        putLong(word);
        i += 4;
      }
      if (i < len) {
        long tail = 0L;
        int shift = 0;
        while (i < len) {
          tail |= ((long) value.charAt(i)) << shift;
          shift += 16;
          i++;
        }
        putLong(tail);
      }
    }

    public long digest() {
      return fmix64(h1 ^ length);
    }
  }

  /// World-level non-player subsystems snapshot.
  public record SubsystemsNode(
      long hash, ClockNode clock, long landscapeHash, long unitGridHash, long suppliesHash, List<SupplyNode> supplies) {
    public SubsystemsNode {
      supplies = List.copyOf(supplies);
    }
  }

  /// Simulation clock, global checksum, and PRNG state snapshot.
  public record ClockNode(
      long hash, int tick, int gamespeed, float secondsPerTick, int globalChecksum, RandomState randomState) {}

  /// Harvestable supply entity snapshot.
  public record SupplyNode(
      int entityId, long hash, String supplyKind, int gridX, int gridY, float posX, float posY, boolean empty) {}

  /// Per-player snapshot grouping player header metrics, units, and buildings.
  public record PlayerNode(
      int playerIndex,
      long hash,
      PlayerHeaderNode header,
      long unitsHash,
      List<UnitNode> units,
      long buildingsHash,
      List<BuildingNode> buildings) {
    public PlayerNode {
      units = List.copyOf(units);
      buildings = List.copyOf(buildings);
    }
  }

  /// Player-level counters, team/race metadata, and chieftain status snapshot.
  public record PlayerHeaderNode(
      int playerIndex,
      long hash,
      int team,
      Race race,
      boolean alive,
      int unitCount,
      int buildingCount,
      int activeChieftainId,
      boolean trainingChieftain,
      int unitsLost,
      int buildingsLost,
      int unitsKilled,
      int buildingsDestroyed,
      int treeHarvested,
      int rockHarvested,
      int ironHarvested,
      int rubberHarvested,
      int unitsMoved,
      int weaponsThrown,
      int magics) {}

  /// Mobile or tower-mounted unit snapshot.
  public record UnitNode(
      int entityId,
      int mountedBuildingId,
      long hash,
      UnitType unitType,
      int gridX,
      int gridY,
      float posX,
      float posY,
      float posZ,
      float dirX,
      float dirY,
      int hitPoints,
      boolean dead,
      boolean mounted,
      EntityActivity activity,
      boolean shouldDecide,
      String lastBehaviourState,
      @Nullable String primaryController,
      @Nullable String currentController,
      @Nullable String currentBehaviour,
      int animation,
      float animTime,
      @Nullable SupplyType supplyType,
      int supplyCount,
      float magicProgress0,
      float magicProgress1,
      @Nullable MagicType lastMagicType) {}

  /// Building structure snapshot.
  public record BuildingNode(
      int entityId,
      long hash,
      BuildingType buildingType,
      int gridX,
      int gridY,
      float posX,
      float posY,
      float posZ,
      int hitPoints,
      int buildPoints,
      boolean dead,
      EntityActivity activity,
      int garrisonedUnits,
      boolean trainingChieftain,
      int woodCount,
      int rockCount,
      int ironCount,
      int rubberCount,
      int rockWeapons,
      int ironWeapons,
      int rubberWeapons) {}

  /// Single diverging node or leaf attribute between two world Merkle trees.
  public record Difference(String path, long expectedHash, long actualHash, String expectedValue, String actualValue) {}

  public static WorldMerkleTree capture(World world) {
    Murmur3Hasher hasher = new Murmur3Hasher();
    int tick = world.getTick();
    SubsystemsNode subsystems = captureSubsystems(world, hasher);
    List<Player> worldPlayers = world.getPlayers();
    List<PlayerNode> playerNodes = new ArrayList<>(worldPlayers.size());
    for (int i = 0; i < worldPlayers.size(); i++) {
      playerNodes.add(capturePlayer(worldPlayers.get(i), i, hasher));
    }

    hasher.reset(0L);
    hasher.putInt(tick);
    hasher.putLong(subsystems.hash());
    hasher.putInt(playerNodes.size());
    for (int i = 0; i < playerNodes.size(); i++) {
      hasher.putLong(playerNodes.get(i).hash());
    }
    long rootHash = hasher.digest();
    return new WorldMerkleTree(rootHash, tick, subsystems, playerNodes);
  }

  public static List<Difference> diff(WorldMerkleTree expected, WorldMerkleTree actual) {
    if (expected.rootHash() == actual.rootHash()) {
      return List.of();
    }
    List<Difference> diffs = new ArrayList<>();
    if (expected.subsystems().hash() != actual.subsystems().hash()) {
      diffSubsystems(diffs, expected.subsystems(), actual.subsystems());
    }
    diffPlayers(diffs, expected.players(), actual.players());
    return List.copyOf(diffs);
  }

  private static SubsystemsNode captureSubsystems(World world, Murmur3Hasher hasher) {
    ClockNode clock = captureClock(world, hasher);
    long landscapeHash = hashLandscape(world.getHeightMap(), hasher);
    long unitGridHash = hashUnitGrid(world.getUnitGrid(), hasher);
    List<SupplyNode> supplies = captureSupplies(world, hasher);

    hasher.reset(0L);
    hasher.putInt(supplies.size());
    for (int i = 0; i < supplies.size(); i++) {
      hasher.putLong(supplies.get(i).hash());
    }
    long suppliesHash = hasher.digest();

    hasher.reset(0L);
    hasher.putLong(clock.hash());
    hasher.putLong(landscapeHash);
    hasher.putLong(unitGridHash);
    hasher.putLong(suppliesHash);
    long subsystemsHash = hasher.digest();

    return new SubsystemsNode(subsystemsHash, clock, landscapeHash, unitGridHash, suppliesHash, supplies);
  }

  private static ClockNode captureClock(World world, Murmur3Hasher hasher) {
    int tick = world.getTick();
    int gamespeed = world.getGamespeed();
    float secondsPerTick = world.getSecondsPerTick();
    int globalChecksum = world.getChecksum();
    RandomState randomState = world.getRandomState();

    hasher.reset(0L);
    hasher.putInt(tick);
    hasher.putInt(gamespeed);
    hasher.putFloat(secondsPerTick);
    hasher.putInt(globalChecksum);
    hashRandomState(randomState, hasher);
    long hash = hasher.digest();

    return new ClockNode(hash, tick, gamespeed, secondsPerTick, globalChecksum, randomState);
  }

  private static void hashRandomState(RandomState randomState, Murmur3Hasher hasher) {
    switch (randomState) {
      case RandomState.LegacyLcg lcg -> {
        hasher.putLong(lcg.seed());
        hasher.putBoolean(lcg.haveNextNextGaussian());
        hasher.putDouble(lcg.nextNextGaussian());
      }
    }
  }

  private static long hashLandscape(HeightMap heightMap, Murmur3Hasher hasher) {
    return heightMap.getOrComputeHeightHash(() -> {
      hasher.reset(0L);
      hasher.putInt(heightMap.getGridUnitsPerWorld());
      float[] heights = heightMap.getHeightData();
      hasher.putInt(heights.length);
      for (float h : heights) {
        hasher.putFloat(h);
      }
      return hasher.digest();
    });
  }

  private static long hashUnitGrid(UnitGrid unitGrid, Murmur3Hasher hasher) {
    int gridSize = unitGrid.getGridSize();
    hasher.reset(0L);
    hasher.putInt(gridSize);
    for (int y = 0; y < gridSize; y++) {
      for (int x = 0; x < gridSize; x++) {
        Occupant occ = unitGrid.getOccupant(x, y);
        if (occ != null) {
          int occupantId = occ instanceof Distributable d ? d.getId() : occ.getClass().getSimpleName().hashCode();
          hasher.putInt(x);
          hasher.putInt(y);
          hasher.putInt(occupantId);
        }
      }
    }
    return hasher.digest();
  }

  private static List<SupplyNode> captureSupplies(World world, Murmur3Hasher hasher) {
    List<SupplyNode> supplies = new ArrayList<>();
    for (Distributable d : world.getDistributableTable().getDistributables()) {
      if (d instanceof Supply supply) {
        supplies.add(captureSupply(d.getId(), supply, hasher));
      }
    }
    supplies.sort(SUPPLY_ORDER);
    return supplies;
  }

  private static SupplyNode captureSupply(int entityId, Supply supply, Murmur3Hasher hasher) {
    String supplyKind = supply.getClass().getSimpleName();
    int gridX = supply.getGridX();
    int gridY = supply.getGridY();
    float posX = supply.getPositionX();
    float posY = supply.getPositionY();
    boolean empty = supply.isEmpty();

    hasher.reset(0L);
    hasher.putInt(entityId);
    hasher.putString(supplyKind);
    hasher.putInt(gridX);
    hasher.putInt(gridY);
    hasher.putFloat(posX);
    hasher.putFloat(posY);
    hasher.putBoolean(empty);
    long hash = hasher.digest();

    return new SupplyNode(entityId, hash, supplyKind, gridX, gridY, posX, posY, empty);
  }

  private static PlayerNode capturePlayer(Player player, int playerIndex, Murmur3Hasher hasher) {
    PlayerHeaderNode header = capturePlayerHeader(player, playerIndex, hasher);
    List<UnitNode> units = new ArrayList<>();
    List<BuildingNode> buildings = new ArrayList<>();

    for (Selectable<?> selectable : player.getUnits().getSet()) {
      switch (selectable) {
        case Unit unit -> units.add(captureUnit(unit, Distributable.NO_ID, hasher));
        case Building building -> {
          buildings.add(captureBuilding(building, hasher));
          if (!building.isDead()
              && building.getRawUnitContainer() instanceof MountUnitContainer muc
              && muc.getNumSupplies() > 0) {
            Unit mountedUnit = muc.getUnit();
            if (mountedUnit != null) {
              units.add(captureUnit(mountedUnit, building.getId(), hasher));
            }
          }
        }
      }
    }

    units.sort(UNIT_ORDER);
    buildings.sort(BUILDING_ORDER);

    hasher.reset(0L);
    hasher.putInt(units.size());
    for (int i = 0; i < units.size(); i++) {
      hasher.putLong(units.get(i).hash());
    }
    long unitsHash = hasher.digest();

    hasher.reset(0L);
    hasher.putInt(buildings.size());
    for (int i = 0; i < buildings.size(); i++) {
      hasher.putLong(buildings.get(i).hash());
    }
    long buildingsHash = hasher.digest();

    hasher.reset(0L);
    hasher.putLong(header.hash());
    hasher.putLong(unitsHash);
    hasher.putLong(buildingsHash);
    long playerHash = hasher.digest();

    return new PlayerNode(playerIndex, playerHash, header, unitsHash, units, buildingsHash, buildings);
  }

  private static PlayerHeaderNode capturePlayerHeader(Player player, int playerIndex, Murmur3Hasher hasher) {
    int team = player.getPlayerInfo().team();
    Race race = player.getPlayerInfo().race();
    boolean alive = player.isAlive();
    int unitCount = player.getUnitCountContainer().getNumSupplies();
    int buildingCount = player.getBuildingCountContainer().getNumSupplies();
    Unit chieftain = player.getActiveChieftain();
    int activeChieftainId = chieftain != null ? chieftain.getId() : Distributable.NO_ID;
    boolean trainingChieftain = player.isTrainingChieftain();
    int unitsLost = player.getUnitsLost();
    int buildingsLost = player.getBuildingsLost();
    int unitsKilled = player.getUnitsKilled();
    int buildingsDestroyed = player.getBuildingsDestroyed();
    int treeHarvested = player.getTreeHarvested();
    int rockHarvested = player.getRockHarvested();
    int ironHarvested = player.getIronHarvested();
    int rubberHarvested = player.getRubberHarvested();
    int unitsMoved = player.getUnitsMoved();
    int weaponsThrown = player.getWeaponsThrown();
    int magics = player.getMagics();

    hasher.reset(0L);
    hasher.putInt(playerIndex);
    hasher.putInt(team);
    hasher.putEnum(race);
    hasher.putBoolean(alive);
    hasher.putInt(unitCount);
    hasher.putInt(buildingCount);
    hasher.putInt(activeChieftainId);
    hasher.putBoolean(trainingChieftain);
    hasher.putInt(unitsLost);
    hasher.putInt(buildingsLost);
    hasher.putInt(unitsKilled);
    hasher.putInt(buildingsDestroyed);
    hasher.putInt(treeHarvested);
    hasher.putInt(rockHarvested);
    hasher.putInt(ironHarvested);
    hasher.putInt(rubberHarvested);
    hasher.putInt(unitsMoved);
    hasher.putInt(weaponsThrown);
    hasher.putInt(magics);
    long hash = hasher.digest();

    return new PlayerHeaderNode(
        playerIndex,
        hash,
        team,
        race,
        alive,
        unitCount,
        buildingCount,
        activeChieftainId,
        trainingChieftain,
        unitsLost,
        buildingsLost,
        unitsKilled,
        buildingsDestroyed,
        treeHarvested,
        rockHarvested,
        ironHarvested,
        rubberHarvested,
        unitsMoved,
        weaponsThrown,
        magics);
  }

  private static UnitNode captureUnit(Unit unit, int mountedBuildingId, Murmur3Hasher hasher) {
    int entityId = unit.getId();
    UnitType unitType = unit.getTemplate().getUnitType();
    int gridX = unit.getGridX();
    int gridY = unit.getGridY();
    float posX = unit.getPositionX();
    float posY = unit.getPositionY();
    float posZ = unit.getPositionZ();
    float dirX = unit.getDirectionX();
    float dirY = unit.getDirectionY();
    int hitPoints = unit.getHitPoints();
    boolean dead = unit.isDead();
    boolean mounted = unit.isMounted();
    EntityActivity activity = unit.getActivity();
    boolean shouldDecide = unit.isShouldDecide();
    String lastBehaviourState = unit.getLastBehaviourStateName();
    String primaryController = dead ? null : stateName(unit.getStateMachine().getPrimaryOrderState());
    String currentController = stateName(unit.getCurrentState());
    String currentBehaviour = unit.isMoving() ? "WalkingPhase" : stateName(unit.getCurrentState());
    int animation = unit.getAnimation();
    float animTime = unit.getAnimationTicks();

    UnitSupplyContainer supplyContainer = unit.getSupplyContainer();
    SupplyType supplyType = supplyContainer != null ? supplyContainer.getSupplyType().orElse(null) : null;
    int supplyCount = supplyContainer != null ? supplyContainer.getNumSupplies() : 0;

    MagicCapability magicCapability = unit.getMagicCapability().orElse(null);
    float magicProgress0 = 0f;
    float magicProgress1 = 0f;
    MagicType lastMagicType = magicCapability != null ? magicCapability.getLastMagicType() : null;
    if (magicCapability != null) {
      List<MagicType> magics = unit.getOwner().getRaceInfo().getMagics();
      if (!magics.isEmpty()) {
        magicProgress0 = magicCapability.getProgress(magics.getFirst());
      }
      if (magics.size() > 1) {
        magicProgress1 = magicCapability.getProgress(magics.get(1));
      }
    }

    hasher.reset(0L);
    hasher.putInt(entityId);
    hasher.putInt(mountedBuildingId);
    hasher.putEnum(unitType);
    hasher.putInt(gridX);
    hasher.putInt(gridY);
    hasher.putFloat(posX);
    hasher.putFloat(posY);
    hasher.putFloat(posZ);
    hasher.putFloat(dirX);
    hasher.putFloat(dirY);
    hasher.putInt(hitPoints);
    hasher.putBoolean(dead);
    hasher.putBoolean(mounted);
    hasher.putEnum(activity);
    hasher.putBoolean(shouldDecide);
    hasher.putString(lastBehaviourState);
    hasher.putInt(animation);
    hasher.putFloat(animTime);
    hasher.putEnum(supplyType);
    hasher.putInt(supplyCount);
    hasher.putFloat(magicProgress0);
    hasher.putFloat(magicProgress1);
    hasher.putEnum(lastMagicType);
    long hash = hasher.digest();

    return new UnitNode(
        entityId,
        mountedBuildingId,
        hash,
        unitType,
        gridX,
        gridY,
        posX,
        posY,
        posZ,
        dirX,
        dirY,
        hitPoints,
        dead,
        mounted,
        activity,
        shouldDecide,
        lastBehaviourState,
        primaryController,
        currentController,
        currentBehaviour,
        animation,
        animTime,
        supplyType,
        supplyCount,
        magicProgress0,
        magicProgress1,
        lastMagicType);
  }

  private static BuildingNode captureBuilding(Building building, Murmur3Hasher hasher) {
    int entityId = building.getId();
    BuildingType buildingType = building.getTemplate().getBuildingType();
    int gridX = building.getGridX();
    int gridY = building.getGridY();
    float posX = building.getPositionX();
    float posY = building.getPositionY();
    float posZ = building.getPositionZ();
    int hitPoints = building.getHitPoints();
    int buildPoints = building.getBuildPoints();
    boolean dead = building.isDead();
    EntityActivity activity = building.getActivity();
    UnitContainer unitContainer = dead ? null : building.getRawUnitContainer();
    int garrisonedUnits = unitContainer != null ? unitContainer.getNumSupplies() : 0;
    boolean trainingChieftain = building.isTrainingChieftain();
    int woodCount = supplyCount(building, SupplyType.WOOD);
    int rockCount = supplyCount(building, SupplyType.ROCK);
    int ironCount = supplyCount(building, SupplyType.IRON);
    int rubberCount = supplyCount(building, SupplyType.RUBBER);
    int rockWeapons = weaponCount(building, WeaponTier.ROCK);
    int ironWeapons = weaponCount(building, WeaponTier.IRON);
    int rubberWeapons = weaponCount(building, WeaponTier.RUBBER);

    hasher.reset(0L);
    hasher.putInt(entityId);
    hasher.putEnum(buildingType);
    hasher.putInt(gridX);
    hasher.putInt(gridY);
    hasher.putFloat(posX);
    hasher.putFloat(posY);
    hasher.putFloat(posZ);
    hasher.putInt(hitPoints);
    hasher.putInt(buildPoints);
    hasher.putBoolean(dead);
    hasher.putEnum(activity);
    hasher.putInt(garrisonedUnits);
    hasher.putBoolean(trainingChieftain);
    hasher.putInt(woodCount);
    hasher.putInt(rockCount);
    hasher.putInt(ironCount);
    hasher.putInt(rubberCount);
    hasher.putInt(rockWeapons);
    hasher.putInt(ironWeapons);
    hasher.putInt(rubberWeapons);
    long hash = hasher.digest();

    return new BuildingNode(
        entityId,
        hash,
        buildingType,
        gridX,
        gridY,
        posX,
        posY,
        posZ,
        hitPoints,
        buildPoints,
        dead,
        activity,
        garrisonedUnits,
        trainingChieftain,
        woodCount,
        rockCount,
        ironCount,
        rubberCount,
        rockWeapons,
        ironWeapons,
        rubberWeapons);
  }

  private static int supplyCount(Building building, SupplyType type) {
    return building.isDead()
        ? 0
        : building
            .getRole()
            .getSupplyStorage()
            .map(storage -> storage.getSupplyContainerDirect(type))
            .map(SupplyContainer::getNumSupplies)
            .orElse(0);
  }

  private static int weaponCount(Building building, WeaponTier tier) {
    return building.isDead()
        ? 0
        : building.getRole().getWeaponsWorkshop().map(w -> w.getWeaponSupplyCount(tier)).orElse(0);
  }

  private static @Nullable String stateName(@Nullable UnitState state) {
    return state != null ? state.getClass().getSimpleName() : null;
  }

  private static void diffSubsystems(List<Difference> diffs, SubsystemsNode exp, SubsystemsNode act) {
    if (exp.clock().hash() != act.clock().hash()) {
      diffClock(diffs, exp.clock(), act.clock());
    }
    if (exp.landscapeHash() != act.landscapeHash()) {
      diffs.add(
          new Difference(
              "subsystems.landscape",
              exp.landscapeHash(),
              act.landscapeHash(),
              Long.toHexString(exp.landscapeHash()),
              Long.toHexString(act.landscapeHash())));
    }
    if (exp.unitGridHash() != act.unitGridHash()) {
      diffs.add(
          new Difference(
              "subsystems.unitGrid",
              exp.unitGridHash(),
              act.unitGridHash(),
              Long.toHexString(exp.unitGridHash()),
              Long.toHexString(act.unitGridHash())));
    }
    if (exp.suppliesHash() != act.suppliesHash()) {
      diffSupplies(diffs, exp.supplies(), act.supplies());
    }
  }

  private static void diffClock(List<Difference> diffs, ClockNode exp, ClockNode act) {
    String prefix = "subsystems.clock";
    diffInt(diffs, prefix, "tick", exp.hash(), act.hash(), exp.tick(), act.tick());
    diffInt(diffs, prefix, "gamespeed", exp.hash(), act.hash(), exp.gamespeed(), act.gamespeed());
    diffFloat(diffs, prefix, "secondsPerTick", exp.hash(), act.hash(), exp.secondsPerTick(), act.secondsPerTick());
    diffInt(diffs, prefix, "globalChecksum", exp.hash(), act.hash(), exp.globalChecksum(), act.globalChecksum());
    diffRandomState(diffs, prefix, exp.hash(), act.hash(), exp.randomState(), act.randomState());
  }

  private static void diffRandomState(
      List<Difference> diffs, String prefix, long expHash, long actHash, RandomState exp, RandomState act) {
    if (exp instanceof RandomState.LegacyLcg expLcg && act instanceof RandomState.LegacyLcg actLcg) {
      diffLong(diffs, prefix, "randomSeed", expHash, actHash, expLcg.seed(), actLcg.seed());
      diffBoolean(
          diffs,
          prefix,
          "haveNextNextGaussian",
          expHash,
          actHash,
          expLcg.haveNextNextGaussian(),
          actLcg.haveNextNextGaussian());
      diffDouble(
          diffs, prefix, "nextNextGaussian", expHash, actHash, expLcg.nextNextGaussian(), actLcg.nextNextGaussian());
    } else {
      diffValue(diffs, prefix, "randomState", expHash, actHash, exp, act);
    }
  }

  private static void diffSupplies(List<Difference> diffs, List<SupplyNode> expList, List<SupplyNode> actList) {
    int i = 0;
    int j = 0;
    while (i < expList.size() || j < actList.size()) {
      if (j >= actList.size() || (i < expList.size() && expList.get(i).entityId() < actList.get(j).entityId())) {
        SupplyNode exp = expList.get(i++);
        diffs.add(
            new Difference(
                "subsystems.supplies[" + exp.entityId() + "]",
                exp.hash(),
                0L,
                exp.supplyKind() + "@(" + exp.gridX() + "," + exp.gridY() + ")",
                "<missing>"));
      } else if (i >= expList.size() || expList.get(i).entityId() > actList.get(j).entityId()) {
        SupplyNode act = actList.get(j++);
        diffs.add(
            new Difference(
                "subsystems.supplies[" + act.entityId() + "]",
                0L,
                act.hash(),
                "<missing>",
                act.supplyKind() + "@(" + act.gridX() + "," + act.gridY() + ")"));
      } else {
        SupplyNode exp = expList.get(i++);
        SupplyNode act = actList.get(j++);
        if (exp.hash() != act.hash()) {
          String prefix = "subsystems.supplies[" + exp.entityId() + "]";
          diffValue(diffs, prefix, "supplyKind", exp.hash(), act.hash(), exp.supplyKind(), act.supplyKind());
          diffInt(diffs, prefix, "gridX", exp.hash(), act.hash(), exp.gridX(), act.gridX());
          diffInt(diffs, prefix, "gridY", exp.hash(), act.hash(), exp.gridY(), act.gridY());
          diffFloat(diffs, prefix, "posX", exp.hash(), act.hash(), exp.posX(), act.posX());
          diffFloat(diffs, prefix, "posY", exp.hash(), act.hash(), exp.posY(), act.posY());
          diffBoolean(diffs, prefix, "empty", exp.hash(), act.hash(), exp.empty(), act.empty());
        }
      }
    }
  }

  private static void diffPlayers(List<Difference> diffs, List<PlayerNode> expList, List<PlayerNode> actList) {
    int i = 0;
    int j = 0;
    while (i < expList.size() || j < actList.size()) {
      if (j >= actList.size() || (i < expList.size() && expList.get(i).playerIndex() < actList.get(j).playerIndex())) {
        PlayerNode exp = expList.get(i++);
        diffs.add(new Difference("players[" + exp.playerIndex() + "]", exp.hash(), 0L, "<present>", "<missing>"));
      } else if (i >= expList.size() || expList.get(i).playerIndex() > actList.get(j).playerIndex()) {
        PlayerNode act = actList.get(j++);
        diffs.add(new Difference("players[" + act.playerIndex() + "]", 0L, act.hash(), "<missing>", "<present>"));
      } else {
        PlayerNode exp = expList.get(i++);
        PlayerNode act = actList.get(j++);
        if (exp.hash() != act.hash()) {
          diffPlayer(diffs, exp, act);
        }
      }
    }
  }

  private static void diffPlayer(List<Difference> diffs, PlayerNode exp, PlayerNode act) {
    String prefix = "players[" + exp.playerIndex() + "]";
    if (exp.header().hash() != act.header().hash()) {
      diffPlayerHeader(diffs, prefix + ".header", exp.header(), act.header());
    }
    if (exp.unitsHash() != act.unitsHash()) {
      diffUnits(diffs, prefix, exp.units(), act.units());
    }
    if (exp.buildingsHash() != act.buildingsHash()) {
      diffBuildings(diffs, prefix, exp.buildings(), act.buildings());
    }
  }

  private static void diffPlayerHeader(
      List<Difference> diffs, String prefix, PlayerHeaderNode exp, PlayerHeaderNode act) {
    long eh = exp.hash();
    long ah = act.hash();
    diffInt(diffs, prefix, "team", eh, ah, exp.team(), act.team());
    diffValue(diffs, prefix, "race", eh, ah, exp.race(), act.race());
    diffBoolean(diffs, prefix, "alive", eh, ah, exp.alive(), act.alive());
    diffInt(diffs, prefix, "unitCount", eh, ah, exp.unitCount(), act.unitCount());
    diffInt(diffs, prefix, "buildingCount", eh, ah, exp.buildingCount(), act.buildingCount());
    diffInt(diffs, prefix, "activeChieftainId", eh, ah, exp.activeChieftainId(), act.activeChieftainId());
    diffBoolean(diffs, prefix, "trainingChieftain", eh, ah, exp.trainingChieftain(), act.trainingChieftain());
    diffInt(diffs, prefix, "unitsLost", eh, ah, exp.unitsLost(), act.unitsLost());
    diffInt(diffs, prefix, "buildingsLost", eh, ah, exp.buildingsLost(), act.buildingsLost());
    diffInt(diffs, prefix, "unitsKilled", eh, ah, exp.unitsKilled(), act.unitsKilled());
    diffInt(diffs, prefix, "buildingsDestroyed", eh, ah, exp.buildingsDestroyed(), act.buildingsDestroyed());
    diffInt(diffs, prefix, "treeHarvested", eh, ah, exp.treeHarvested(), act.treeHarvested());
    diffInt(diffs, prefix, "rockHarvested", eh, ah, exp.rockHarvested(), act.rockHarvested());
    diffInt(diffs, prefix, "ironHarvested", eh, ah, exp.ironHarvested(), act.ironHarvested());
    diffInt(diffs, prefix, "rubberHarvested", eh, ah, exp.rubberHarvested(), act.rubberHarvested());
    diffInt(diffs, prefix, "unitsMoved", eh, ah, exp.unitsMoved(), act.unitsMoved());
    diffInt(diffs, prefix, "weaponsThrown", eh, ah, exp.weaponsThrown(), act.weaponsThrown());
    diffInt(diffs, prefix, "magics", eh, ah, exp.magics(), act.magics());
  }

  private static void diffUnits(
      List<Difference> diffs, String playerPrefix, List<UnitNode> expList, List<UnitNode> actList) {
    int i = 0;
    int j = 0;
    while (i < expList.size() || j < actList.size()) {
      int cmp =
          (i < expList.size() && j < actList.size())
              ? UNIT_ORDER.compare(expList.get(i), actList.get(j))
              : (i < expList.size() ? -1 : 1);
      if (cmp < 0) {
        UnitNode exp = expList.get(i++);
        diffs.add(
            new Difference(
                playerPrefix + "." + unitLabel(exp),
                exp.hash(),
                0L,
                exp.unitType().name() + "@(" + exp.gridX() + "," + exp.gridY() + ")",
                "<missing>"));
      } else if (cmp > 0) {
        UnitNode act = actList.get(j++);
        diffs.add(
            new Difference(
                playerPrefix + "." + unitLabel(act),
                0L,
                act.hash(),
                "<missing>",
                act.unitType().name() + "@(" + act.gridX() + "," + act.gridY() + ")"));
      } else {
        UnitNode exp = expList.get(i++);
        UnitNode act = actList.get(j++);
        if (exp.hash() != act.hash()) {
          diffUnitFields(diffs, playerPrefix + "." + unitLabel(exp), exp, act);
        }
      }
    }
  }

  private static String unitLabel(UnitNode unit) {
    if (unit.mountedBuildingId() != Distributable.NO_ID) {
      return "units[" + unit.entityId() + "@mounted:" + unit.mountedBuildingId() + "]";
    }
    return "units[" + unit.entityId() + "]";
  }

  private static void diffUnitFields(List<Difference> diffs, String prefix, UnitNode exp, UnitNode act) {
    long eh = exp.hash();
    long ah = act.hash();
    diffValue(diffs, prefix, "unitType", eh, ah, exp.unitType(), act.unitType());
    diffInt(diffs, prefix, "gridX", eh, ah, exp.gridX(), act.gridX());
    diffInt(diffs, prefix, "gridY", eh, ah, exp.gridY(), act.gridY());
    diffFloat(diffs, prefix, "posX", eh, ah, exp.posX(), act.posX());
    diffFloat(diffs, prefix, "posY", eh, ah, exp.posY(), act.posY());
    diffFloat(diffs, prefix, "posZ", eh, ah, exp.posZ(), act.posZ());
    diffFloat(diffs, prefix, "dirX", eh, ah, exp.dirX(), act.dirX());
    diffFloat(diffs, prefix, "dirY", eh, ah, exp.dirY(), act.dirY());
    diffInt(diffs, prefix, "hitPoints", eh, ah, exp.hitPoints(), act.hitPoints());
    diffBoolean(diffs, prefix, "dead", eh, ah, exp.dead(), act.dead());
    diffBoolean(diffs, prefix, "mounted", eh, ah, exp.mounted(), act.mounted());
    diffValue(diffs, prefix, "activity", eh, ah, exp.activity(), act.activity());
    diffBoolean(diffs, prefix, "shouldDecide", eh, ah, exp.shouldDecide(), act.shouldDecide());
    diffValue(diffs, prefix, "lastBehaviourState", eh, ah, exp.lastBehaviourState(), act.lastBehaviourState());
    diffValue(diffs, prefix, "primaryController", eh, ah, exp.primaryController(), act.primaryController());
    diffValue(diffs, prefix, "currentController", eh, ah, exp.currentController(), act.currentController());
    diffValue(diffs, prefix, "currentBehaviour", eh, ah, exp.currentBehaviour(), act.currentBehaviour());
    diffInt(diffs, prefix, "animation", eh, ah, exp.animation(), act.animation());
    diffFloat(diffs, prefix, "animTime", eh, ah, exp.animTime(), act.animTime());
    diffValue(diffs, prefix, "supplyType", eh, ah, exp.supplyType(), act.supplyType());
    diffInt(diffs, prefix, "supplyCount", eh, ah, exp.supplyCount(), act.supplyCount());
    diffFloat(diffs, prefix, "magicProgress0", eh, ah, exp.magicProgress0(), act.magicProgress0());
    diffFloat(diffs, prefix, "magicProgress1", eh, ah, exp.magicProgress1(), act.magicProgress1());
    diffValue(diffs, prefix, "lastMagicType", eh, ah, exp.lastMagicType(), act.lastMagicType());
  }

  private static void diffBuildings(
      List<Difference> diffs, String playerPrefix, List<BuildingNode> expList, List<BuildingNode> actList) {
    int i = 0;
    int j = 0;
    while (i < expList.size() || j < actList.size()) {
      if (j >= actList.size() || (i < expList.size() && expList.get(i).entityId() < actList.get(j).entityId())) {
        BuildingNode exp = expList.get(i++);
        diffs.add(
            new Difference(
                playerPrefix + ".buildings[" + exp.entityId() + "]",
                exp.hash(),
                0L,
                exp.buildingType().name() + "@(" + exp.gridX() + "," + exp.gridY() + ")",
                "<missing>"));
      } else if (i >= expList.size() || expList.get(i).entityId() > actList.get(j).entityId()) {
        BuildingNode act = actList.get(j++);
        diffs.add(
            new Difference(
                playerPrefix + ".buildings[" + act.entityId() + "]",
                0L,
                act.hash(),
                "<missing>",
                act.buildingType().name() + "@(" + act.gridX() + "," + act.gridY() + ")"));
      } else {
        BuildingNode exp = expList.get(i++);
        BuildingNode act = actList.get(j++);
        if (exp.hash() != act.hash()) {
          diffBuildingFields(diffs, playerPrefix + ".buildings[" + exp.entityId() + "]", exp, act);
        }
      }
    }
  }

  private static void diffBuildingFields(List<Difference> diffs, String prefix, BuildingNode exp, BuildingNode act) {
    long eh = exp.hash();
    long ah = act.hash();
    diffValue(diffs, prefix, "buildingType", eh, ah, exp.buildingType(), act.buildingType());
    diffInt(diffs, prefix, "gridX", eh, ah, exp.gridX(), act.gridX());
    diffInt(diffs, prefix, "gridY", eh, ah, exp.gridY(), act.gridY());
    diffFloat(diffs, prefix, "posX", eh, ah, exp.posX(), act.posX());
    diffFloat(diffs, prefix, "posY", eh, ah, exp.posY(), act.posY());
    diffFloat(diffs, prefix, "posZ", eh, ah, exp.posZ(), act.posZ());
    diffInt(diffs, prefix, "hitPoints", eh, ah, exp.hitPoints(), act.hitPoints());
    diffInt(diffs, prefix, "buildPoints", eh, ah, exp.buildPoints(), act.buildPoints());
    diffBoolean(diffs, prefix, "dead", eh, ah, exp.dead(), act.dead());
    diffValue(diffs, prefix, "activity", eh, ah, exp.activity(), act.activity());
    diffInt(diffs, prefix, "garrisonedUnits", eh, ah, exp.garrisonedUnits(), act.garrisonedUnits());
    diffBoolean(diffs, prefix, "trainingChieftain", eh, ah, exp.trainingChieftain(), act.trainingChieftain());
    diffInt(diffs, prefix, "woodCount", eh, ah, exp.woodCount(), act.woodCount());
    diffInt(diffs, prefix, "rockCount", eh, ah, exp.rockCount(), act.rockCount());
    diffInt(diffs, prefix, "ironCount", eh, ah, exp.ironCount(), act.ironCount());
    diffInt(diffs, prefix, "rubberCount", eh, ah, exp.rubberCount(), act.rubberCount());
    diffInt(diffs, prefix, "rockWeapons", eh, ah, exp.rockWeapons(), act.rockWeapons());
    diffInt(diffs, prefix, "ironWeapons", eh, ah, exp.ironWeapons(), act.ironWeapons());
    diffInt(diffs, prefix, "rubberWeapons", eh, ah, exp.rubberWeapons(), act.rubberWeapons());
  }

  private static void diffInt(
      List<Difference> diffs, String prefix, String field, long expHash, long actHash, int exp, int act) {
    if (exp != act) {
      diffs.add(new Difference(prefix + "." + field, expHash, actHash, Integer.toString(exp), Integer.toString(act)));
    }
  }

  private static void diffFloat(
      List<Difference> diffs, String prefix, String field, long expHash, long actHash, float exp, float act) {
    if (Float.floatToRawIntBits(exp) != Float.floatToRawIntBits(act)) {
      diffs.add(new Difference(prefix + "." + field, expHash, actHash, Float.toString(exp), Float.toString(act)));
    }
  }

  private static void diffDouble(
      List<Difference> diffs, String prefix, String field, long expHash, long actHash, double exp, double act) {
    if (Double.doubleToRawLongBits(exp) != Double.doubleToRawLongBits(act)) {
      diffs.add(new Difference(prefix + "." + field, expHash, actHash, Double.toString(exp), Double.toString(act)));
    }
  }

  private static void diffBoolean(
      List<Difference> diffs, String prefix, String field, long expHash, long actHash, boolean exp, boolean act) {
    if (exp != act) {
      diffs.add(new Difference(prefix + "." + field, expHash, actHash, Boolean.toString(exp), Boolean.toString(act)));
    }
  }

  private static void diffValue(
      List<Difference> diffs,
      String prefix,
      String field,
      long expHash,
      long actHash,
      @Nullable Object exp,
      @Nullable Object act) {
    if (!Objects.equals(exp, act)) {
      diffs.add(new Difference(prefix + "." + field, expHash, actHash, String.valueOf(exp), String.valueOf(act)));
    }
  }

  private static void diffLong(
      List<Difference> diffs, String prefix, String field, long expHash, long actHash, long exp, long act) {
    if (exp != act) {
      diffs.add(new Difference(prefix + "." + field, expHash, actHash, Long.toString(exp), Long.toString(act)));
    }
  }
}
