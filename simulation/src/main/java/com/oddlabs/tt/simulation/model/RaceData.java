package com.oddlabs.tt.simulation.model;

import com.oddlabs.tt.base.geom.SpriteGeometry;
import com.oddlabs.tt.base.util.Utils;
import com.oddlabs.tt.simulation.model.weapon.InstantHitFactory;
import com.oddlabs.tt.simulation.model.weapon.IronAxeWeapon;
import com.oddlabs.tt.simulation.model.weapon.IronSpearWeapon;
import com.oddlabs.tt.simulation.model.weapon.LightningCloudFactory;
import com.oddlabs.tt.simulation.model.weapon.MagicFactory;
import com.oddlabs.tt.simulation.model.weapon.PoisonFogFactory;
import com.oddlabs.tt.simulation.model.weapon.RockAxeWeapon;
import com.oddlabs.tt.simulation.model.weapon.RockSpearWeapon;
import com.oddlabs.tt.simulation.model.weapon.RubberAxeWeapon;
import com.oddlabs.tt.simulation.model.weapon.RubberSpearWeapon;
import com.oddlabs.tt.simulation.model.weapon.SonicBlastFactory;
import com.oddlabs.tt.simulation.model.weapon.StunFactory;
import com.oddlabs.tt.simulation.model.weapon.ThrowingFactory;
import com.oddlabs.tt.simulation.model.weapon.WeaponFactory;
import com.oddlabs.tt.simulation.player.NativeChieftainAI;
import com.oddlabs.tt.simulation.player.VikingChieftainAI;
import org.joml.Vector3f;

import java.util.EnumMap;
import java.util.ResourceBundle;
import java.util.stream.IntStream;

/**
 * Central model-side registry for game race statistics and template configs.
 */
public final class RaceData {
    private static final ResourceBundle bundle = ResourceBundle.getBundle(RaceData.class.getName());

    private static String i18n(String key, Object... args) {
        return Utils.getBundleString(bundle, key, args);
    }

    public static final int QUARTERS_SIZE = 5;
    public static final int ARMORY_SIZE = 5;
    public static final int TOWER_SIZE = 3;
    public static final int MAX_BUILDING_SIZE = IntStream.of(QUARTERS_SIZE, ARMORY_SIZE, TOWER_SIZE).max()
            .orElseThrow();
    public static final int QUARTERS_HIT_POINTS = 200;
    public static final int ARMORY_HIT_POINTS = 200;
    public static final int TOWER_HIT_POINTS = 100;
    public static final int VIKING_CHIEFTAIN_HIT_POINTS = 60;
    public static final int NATIVE_CHIEFTAIN_HIT_POINTS = 40;

    public static final float THROW_RANGE = 6f;
    public static final float BUILDING_RING_PHYSICAL_THICKNESS = 0.2f;
    private static final int MAX_UNIT_RESOURCES = 1;

    private final EnumMap<Race, RaceInfo> raceInfos;

    public static boolean isValidRace(int race) {
        return race >= 0 && race < Race.values().length;
    }

    public static String getRaceName(Race race) {
        return switch (race) {
            case NATIVES -> i18n("natives");
            case VIKINGS -> i18n("vikings");
        };
    }

    public RaceData() {
        this.raceInfos = createDefaultRaceInfos();
    }

    public RaceInfo getRaceInfo(Race race) {
        return raceInfos.get(race);
    }

    private static EnumMap<Race, RaceInfo> createDefaultRaceInfos() {
        EnumMap<Race, RaceInfo> raceInfos = new EnumMap<>(Race.class);
        raceInfos.put(Race.NATIVES, createNativesRaceInfo());
        raceInfos.put(Race.VIKINGS, createVikingsRaceInfo());
        return raceInfos;
    }

    private static BuildingTemplate createBuildingTemplate(
            BuildingType buildingType,
            Race race,
            String builtSpriteLocation,
            float builtSelectionRadius,
            float builtSelectionHeight,
            String halfbuiltSpriteLocation,
            float halfbuiltSelectionRadius,
            float halfbuiltSelectionHeight,
            String startSpriteLocation,
            float startSelectionRadius,
            float startSelectionHeight,
            float shadowDiameter,
            int placingSize,
            float smokeRadius,
            float smokeHeight,
            int numFragments,
            int maxHitPoints,
            UnitContainerFactory unitContainerFactory,
            Abilities abilities,
            float[] hitOffsetZ,
            float mountOffset,
            float noDetailSize,
            float rallyX,
            float rallyY,
            float rallyZ,
            float chimneyX,
            float chimneyY,
            float chimneyZ,
            String name
    ) {
        SpriteGeometry builtGeom = SpriteGeometry.load(builtSpriteLocation);
        SpriteGeometry halfbuiltGeom = SpriteGeometry.load(halfbuiltSpriteLocation);
        SpriteGeometry startGeom = SpriteGeometry.load(startSpriteLocation);

        assert hitOffsetZ.length == 3;
        return new BuildingTemplate(
                buildingType,
                placingSize,
                smokeRadius,
                smokeHeight,
                numFragments,
                shadowDiameter,
                builtGeom.bounds(),
                builtSelectionRadius,
                builtSelectionHeight,
                halfbuiltGeom.bounds(),
                halfbuiltSelectionRadius,
                halfbuiltSelectionHeight,
                startGeom.bounds(),
                startSelectionRadius,
                startSelectionHeight,
                maxHitPoints,
                unitContainerFactory,
                abilities,
                hitOffsetZ,
                mountOffset,
                noDetailSize,
                0f,
                new Vector3f(rallyX, rallyY, rallyZ),
                new Vector3f(chimneyX, chimneyY, chimneyZ),
                name
        );
    }

    private static RaceInfo createVikingsRaceInfo() {
        BuildingTemplate vikingQuarters = createBuildingTemplate(
                BuildingType.QUARTERS,
                Race.VIKINGS,
                "/geometry/vikings/quarters.binsprite",
                3.5f, 7f,
                "/geometry/vikings/quarters_halfbuilt.binsprite",
                3.5f, 6f,
                "/geometry/vikings/quarters_start.binsprite",
                5f, 1f,
                22f, QUARTERS_SIZE, 6f, 9f, 30, QUARTERS_HIT_POINTS,
                new ReproduceUnitContainerFactory(),
                new Abilities(Abilities.REPRODUCE | Abilities.RALLY_TO | Abilities.TARGET),
                new float[]{0f, 1f, 3f}, 0f, 6f,
                3.65f, .25f, 8f,
                0f, 0f, 0f,
                i18n("quarters")
        );

        BuildingTemplate vikingArmory = createBuildingTemplate(
                BuildingType.ARMORY,
                Race.VIKINGS,
                "/geometry/vikings/armory.binsprite",
                3.5f, 7f,
                "/geometry/vikings/armory_halfbuilt.binsprite",
                3.5f, 6f,
                "/geometry/vikings/armory_start.binsprite",
                5f, 1f,
                22f, ARMORY_SIZE, 6f, 9f, 30, ARMORY_HIT_POINTS,
                new WorkerUnitContainerFactory(),
                new Abilities(Abilities.SUPPLY_CONTAINER | Abilities.BUILD_ARMIES | Abilities.RALLY_TO
                        | Abilities.TARGET),
                new float[]{0f, 1f, 3f}, 0f, 6f,
                0f, 2.25f, 10f,
                .25f, -2.8f, 13.1f,
                i18n("armory")
        );

        BuildingTemplate vikingTower = createBuildingTemplate(
                BuildingType.TOWER,
                Race.VIKINGS,
                "/geometry/vikings/tower.binsprite",
                1.25f, 11f,
                "/geometry/vikings/tower_halfbuilt.binsprite",
                2f, 7f,
                "/geometry/vikings/tower_start.binsprite",
                2.5f, 1f,
                10f, TOWER_SIZE, 3f, 12f, 20, TOWER_HIT_POINTS,
                new MountUnitContainerFactory(),
                new Abilities(Abilities.ATTACK | Abilities.RALLY_TO | Abilities.TARGET),
                new float[]{0f, 2f, 7.5f}, 9.55f, 2.5f,
                .85f, .85f, 9.5f,
                0f, 0f, 0f,
                i18n("tower")
        );

        final float shadowDiameterWarrior = 1.9f;
        final float shadowDiameterPeon = 1.6f;
        final float shadowDiameterChieftain = 2.2f;

        SpriteGeometry warriorGeom = SpriteGeometry.load("/geometry/vikings/warrior.binsprite");
        SpriteGeometry peonGeom = SpriteGeometry.load("/geometry/vikings/peon.binsprite");
        SpriteGeometry chieftainGeom = SpriteGeometry.load("/geometry/vikings/chieftain.binsprite");

        WeaponFactory vikingWarriorRockWeapon = new ThrowingFactory<>(
                RockAxeWeapon.class, RockAxeWeapon::new, 0.5f, THROW_RANGE, 29f / 58f
        );
        WeaponFactory vikingWarriorIronWeapon = new ThrowingFactory<>(
                IronAxeWeapon.class, IronAxeWeapon::new, 0.75f, THROW_RANGE, 29f / 58f
        );
        WeaponFactory vikingWarriorRubberWeapon = new ThrowingFactory<>(
                RubberAxeWeapon.class, RubberAxeWeapon::new, 0.95f, THROW_RANGE, 29f / 58f
        );

        UnitTemplate vikingWarriorRock = new UnitTemplate(
                .4f, 1.2f,
                new Abilities(Abilities.ATTACK | Abilities.TARGET | Abilities.THROW),
                4f, vikingWarriorRockWeapon, UnitVisualType.WARRIOR_ROCK,
                warriorGeom.bounds(), warriorGeom.animTypes(),
                shadowDiameterWarrior, null, .25f, new float[]{1.2f}, 1f, .5f,
                i18n("rock_warrior"),
                1, 0f, 0f, 2f, 3
        );

        UnitTemplate vikingWarriorIron = new UnitTemplate(
                .4f, 1.2f,
                new Abilities(Abilities.ATTACK | Abilities.TARGET | Abilities.THROW),
                4f, vikingWarriorIronWeapon, UnitVisualType.WARRIOR_IRON,
                warriorGeom.bounds(), warriorGeom.animTypes(),
                shadowDiameterWarrior, null, .25f, new float[]{1.2f}, 1f, .7f,
                i18n("iron_warrior"),
                1, 0f, 0f, 2f, 5
        );

        UnitTemplate vikingWarriorRubber = new UnitTemplate(
                .4f, 1.2f,
                new Abilities(Abilities.ATTACK | Abilities.TARGET | Abilities.THROW),
                4f, vikingWarriorRubberWeapon, UnitVisualType.WARRIOR_RUBBER,
                warriorGeom.bounds(), warriorGeom.animTypes(),
                shadowDiameterWarrior, null, .25f, new float[]{1.2f}, 1f, .7f,
                i18n("chicken_warrior"),
                1, 0f, 0f, 2f, 10
        );

        UnitTemplate vikingPeon = new UnitTemplate(
                .4f, 1.1f,
                new Abilities(Abilities.BUILD | Abilities.HARVEST | Abilities.ATTACK | Abilities.TARGET),
                5f, new InstantHitFactory(1 / 5f, 0f, 11f / 38f),
                UnitVisualType.PEON, peonGeom.bounds(), peonGeom.animTypes(),
                shadowDiameterPeon, new UnitSupplyContainerFactory(MAX_UNIT_RESOURCES),
                .25f, new float[]{.7f}, 1f, 0f,
                i18n("peon"),
                1, .1f, 0f, 1.75f, 1
        );

        UnitTemplate vikingChieftain = new UnitTemplate(
                .4f, 1.4f,
                new Abilities(Abilities.ATTACK | Abilities.TARGET | Abilities.MAGIC),
                4f, new InstantHitFactory(3 / 4f, 0f, 75f / 119f),
                UnitVisualType.CHIEFTAIN, chieftainGeom.bounds(), chieftainGeom.animTypes(),
                shadowDiameterChieftain, null,
                .15f, new float[]{1.7f}, 1f, 0.5f,
                i18n("chieftain"),
                VIKING_CHIEFTAIN_HIT_POINTS,
                -.07f, .312f, 2.7f, 40
        );

        EnumMap<MagicType, MagicFactory> vikingMagic = new EnumMap<>(MagicType.class);
        vikingMagic.put(MagicType.STUN, new StunFactory(2.57f, 0f, 3.8f, 36f, 30f, 10f, 6f, 57f / 159f, 100f / 159f));
        vikingMagic.put(MagicType.SONIC_BLAST, new SonicBlastFactory(2.57f, 0f, 3.8f, 36f, 17f, 2f, 150, 30, .8f, 6f,
                57f / 159f, 100f / 159f));

        return new RaceInfo(
                Race.VIKINGS,
                vikingQuarters,
                vikingArmory,
                vikingTower,
                vikingWarriorRock,
                vikingWarriorIron,
                vikingWarriorRubber,
                vikingPeon,
                vikingChieftain,
                vikingMagic,
                new VikingChieftainAI()
        );
    }

    private static RaceInfo createNativesRaceInfo() {
        BuildingTemplate nativeQuarters = createBuildingTemplate(
                BuildingType.QUARTERS,
                Race.NATIVES,
                "/geometry/natives/quarters.binsprite",
                4f, 8f,
                "/geometry/natives/quarters_halfbuilt.binsprite",
                4f, 6f,
                "/geometry/natives/quarters_start.binsprite",
                5f, 1f,
                16f, QUARTERS_SIZE, 6f, 9f, 30, QUARTERS_HIT_POINTS,
                new ReproduceUnitContainerFactory(),
                new Abilities(Abilities.REPRODUCE | Abilities.RALLY_TO | Abilities.TARGET),
                new float[]{0f, 1f, 3f}, 0f, 6f,
                -1.15f, -.77f, 11f,
                0f, 0f, 0f,
                i18n("quarters")
        );

        BuildingTemplate nativeArmory = createBuildingTemplate(
                BuildingType.ARMORY,
                Race.NATIVES,
                "/geometry/natives/armory.binsprite",
                4f, 8f,
                "/geometry/natives/armory_halfbuilt.binsprite",
                4f, 6f,
                "/geometry/natives/armory_start.binsprite",
                5f, 1f,
                16f, ARMORY_SIZE, 6f, 9f, 30, ARMORY_HIT_POINTS,
                new WorkerUnitContainerFactory(),
                new Abilities(Abilities.SUPPLY_CONTAINER | Abilities.BUILD_ARMIES | Abilities.RALLY_TO
                        | Abilities.TARGET),
                new float[]{0f, 1f, 3f}, 0f, 6f,
                0f, -.4f, 12f,
                0f, -1f, 11.5f,
                i18n("armory")
        );

        BuildingTemplate nativeTower = createBuildingTemplate(
                BuildingType.TOWER,
                Race.NATIVES,
                "/geometry/natives/tower.binsprite",
                1f, 14f,
                "/geometry/natives/tower_halfbuilt.binsprite",
                1f, 14f,
                "/geometry/natives/tower_start.binsprite",
                1.5f, 2f,
                5f, TOWER_SIZE, 3f, 12f, 20, TOWER_HIT_POINTS,
                new MountUnitContainerFactory(),
                new Abilities(Abilities.ATTACK | Abilities.RALLY_TO | Abilities.TARGET),
                new float[]{0f, 11.5f, 11.5f}, 13f, 2.5f,
                .95f, 0f, 13f,
                0f, 0f, 0f,
                i18n("tower")
        );

        final float shadowDiameterWarrior = 1.9f;
        final float shadowDiameterPeon = 1.6f;
        final float shadowDiameterChieftain = 2.2f;

        SpriteGeometry warriorGeom = SpriteGeometry.load("/geometry/natives/warrior.binsprite");
        SpriteGeometry peonGeom = SpriteGeometry.load("/geometry/natives/peon.binsprite");
        SpriteGeometry chieftainGeom = SpriteGeometry.load("/geometry/natives/chieftain.binsprite");

        WeaponFactory nativeWarriorRockWeapon = new ThrowingFactory<>(
                RockSpearWeapon.class, RockSpearWeapon::new, 0.5f, THROW_RANGE, 46f / 100f
        );
        WeaponFactory nativeWarriorIronWeapon = new ThrowingFactory<>(
                IronSpearWeapon.class, IronSpearWeapon::new, 0.75f, THROW_RANGE, 46f / 100f
        );
        WeaponFactory nativeWarriorRubberWeapon = new ThrowingFactory<>(
                RubberSpearWeapon.class, RubberSpearWeapon::new, 0.95f, THROW_RANGE, 46f / 100f
        );

        UnitTemplate nativeWarriorRock = new UnitTemplate(
                .4f, 1.2f,
                new Abilities(Abilities.ATTACK | Abilities.TARGET | Abilities.THROW),
                4f, nativeWarriorRockWeapon, UnitVisualType.WARRIOR_ROCK,
                warriorGeom.bounds(), warriorGeom.animTypes(),
                shadowDiameterWarrior, null, .25f, new float[]{1.2f}, 1f, .5f,
                i18n("rock_warrior"),
                1, 0f, 0f, 2f, 3
        );

        UnitTemplate nativeWarriorIron = new UnitTemplate(
                .4f, 1.2f,
                new Abilities(Abilities.ATTACK | Abilities.TARGET | Abilities.THROW),
                4f, nativeWarriorIronWeapon, UnitVisualType.WARRIOR_IRON,
                warriorGeom.bounds(), warriorGeom.animTypes(),
                shadowDiameterWarrior, null, .25f, new float[]{1.2f}, 1f, .7f,
                i18n("iron_warrior"),
                1, 0f, 0f, 2f, 5
        );

        UnitTemplate nativeWarriorRubber = new UnitTemplate(
                .4f, 1.2f,
                new Abilities(Abilities.ATTACK | Abilities.TARGET | Abilities.THROW),
                4f, nativeWarriorRubberWeapon, UnitVisualType.WARRIOR_RUBBER,
                warriorGeom.bounds(), warriorGeom.animTypes(),
                shadowDiameterWarrior, null, .25f, new float[]{1.2f}, 1f, .7f,
                i18n("chicken_warrior"),
                1, 0f, 0f, 2f, 10
        );

        UnitTemplate nativePeon = new UnitTemplate(
                .4f, 1.1f,
                new Abilities(Abilities.BUILD | Abilities.HARVEST | Abilities.ATTACK | Abilities.TARGET),
                5f, new InstantHitFactory(1 / 5f, 0f, 51f / 83f),
                UnitVisualType.PEON, peonGeom.bounds(), peonGeom.animTypes(),
                shadowDiameterPeon, new UnitSupplyContainerFactory(MAX_UNIT_RESOURCES),
                .25f, new float[]{.7f}, 1f, 0f,
                i18n("peon"),
                1, 0f, 0f, 1.75f, 1
        );

        UnitTemplate nativeChieftain = new UnitTemplate(
                .4f, 1.4f,
                new Abilities(Abilities.ATTACK | Abilities.TARGET | Abilities.MAGIC),
                4f, new InstantHitFactory(3 / 4f, 0f, 75f / 129f),
                UnitVisualType.CHIEFTAIN, chieftainGeom.bounds(), chieftainGeom.animTypes(),
                shadowDiameterChieftain, null,
                .15f, new float[]{1.7f}, 1f, 0.5f,
                i18n("chieftain"),
                NATIVE_CHIEFTAIN_HIT_POINTS,
                .878f, .151f, 2.8f, 40
        );

        EnumMap<MagicType, MagicFactory> nativeMagic = new EnumMap<>(MagicType.class);
        nativeMagic.put(MagicType.POISON_FOG, new PoisonFogFactory(0.9f, 0f, 0.55f, 26f, .5f, 2f, 20f, 10, 5f, 80f
                / 224f, 163f / 224f));
        nativeMagic.put(MagicType.LIGHTNING_CLOUD, new LightningCloudFactory(0.9f, 0f, 0.55f, 22f, 1f, 8f, 1f, 30, 18f,
                5f, 80f / 224f, 163f / 224f));

        return new RaceInfo(
                Race.NATIVES,
                nativeQuarters,
                nativeArmory,
                nativeTower,
                nativeWarriorRock,
                nativeWarriorIron,
                nativeWarriorRubber,
                nativePeon,
                nativeChieftain,
                nativeMagic,
                new NativeChieftainAI()
        );
    }
}
