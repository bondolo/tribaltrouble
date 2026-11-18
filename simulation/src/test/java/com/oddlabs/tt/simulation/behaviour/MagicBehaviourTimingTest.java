package com.oddlabs.tt.simulation.behaviour;

import com.oddlabs.tt.simulation.landscape.FlatLandscape;
import com.oddlabs.tt.simulation.landscape.Landscape;
import com.oddlabs.tt.simulation.landscape.NotificationListener;
import com.oddlabs.tt.simulation.landscape.World;
import com.oddlabs.tt.simulation.landscape.WorldParameters;
import com.oddlabs.tt.simulation.model.MagicType;
import com.oddlabs.tt.simulation.model.Model;
import com.oddlabs.tt.simulation.model.Race;
import com.oddlabs.tt.simulation.model.Target;
import com.oddlabs.tt.simulation.model.Unit;
import com.oddlabs.tt.simulation.model.UnitType;
import com.oddlabs.tt.simulation.player.Player;
import com.oddlabs.tt.simulation.player.PlayerConfig;
import com.oddlabs.tt.simulation.player.PlayerInfo;
import com.oddlabs.util.Color;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@NullMarked
final class MagicBehaviourTimingTest {

    private World world;
    private Player vikingPlayer;
    private Player nativePlayer;

    @BeforeEach
    void setUp() {
        int gridSize = 32;
        Landscape landscape = FlatLandscape.create(gridSize);
        WorldParameters worldParams = new WorldParameters("1", 5, 20);

        List<PlayerInfo> playerInfos = List.of(
                new PlayerInfo(0, Race.VIKINGS, "VikingPlayer"),
                new PlayerInfo(1, Race.NATIVES, "NativePlayer")
        );

        Color.Linear[] playerColors = new Color.Linear[]{
                new Color.Linear(1f, 0f, 0f, 1f),
                new Color.Linear(0f, 0f, 1f, 1f)
        };

        world = new World(worldParams, landscape, PlayerConfig.from(playerInfos, playerColors));
        vikingPlayer = world.getPlayers().getFirst();
        nativePlayer = world.getPlayers().get(1);
    }

    @Test
    void testStunTimingAndState() {
        Unit vikingChieftain = new Unit(vikingPlayer, 16f, 16f, null,
                vikingPlayer.getRaceInfo().getUnitTemplate(UnitType.CHIEFTAIN));
        vikingChieftain.register();
        Unit nativeWarrior = new Unit(nativePlayer, 17f, 16f, null,
                nativePlayer.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_ROCK));
        nativeWarrior.register();

        vikingChieftain.maxMagicEnergy(MagicType.STUN);
        vikingChieftain.doMagic(MagicType.STUN, true);

        // Advance ticks during PREPARING (0 to ~2.15s) and CASTING (~2.15s to ~3.77s).
        // At 50 ticks/sec (dt = 0.02s), release occurs around tick 189.
        for (int i = 0; i < 150; i++) {
            world.tick();
            assertFalse(nativeWarrior.isStunned(),
                    "Native warrior must not be stunned during PREPARING or CASTING (tick " + i + ")");
        }

        // Advance through release time (ENDING)
        for (int i = 150; i < 220; i++) {
            world.tick();
            if (nativeWarrior.isStunned()) {
                break;
            }
        }

        assertTrue(nativeWarrior.isStunned(),
                "Native warrior must become stunned upon spell release at ENDING");
    }

    @Test
    void testSonicBlastTimingAndState() {
        AtomicBoolean sonicBlastNotified = new AtomicBoolean(false);
        world.setNotificationListener(new NotificationListener() {
            @Override
            public void onSonicBlast(Model model) {
                sonicBlastNotified.set(true);
            }
        });

        Unit vikingChieftain = new Unit(vikingPlayer, 16f, 16f, null,
                vikingPlayer.getRaceInfo().getUnitTemplate(UnitType.CHIEFTAIN));
        vikingChieftain.register();
        Unit nativeWarrior = new Unit(nativePlayer, 18f, 16f, null,
                nativePlayer.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_ROCK));
        nativeWarrior.register();
        int initialHealth = nativeWarrior.getHitPoints();

        vikingChieftain.maxMagicEnergy(MagicType.SONIC_BLAST);
        vikingChieftain.doMagic(MagicType.SONIC_BLAST, true);

        // Advance ticks during PREPARING and CASTING
        for (int i = 0; i < 150; i++) {
            world.tick();
            assertFalse(sonicBlastNotified.get(),
                    "onSonicBlast must not fire during PREPARING or CASTING (tick " + i + ")");
            assertEquals(initialHealth, nativeWarrior.getHitPoints(),
                    "Native warrior must not take damage before spell release");
        }

        // Advance through release time
        for (int i = 150; i < 220; i++) {
            world.tick();
            if (sonicBlastNotified.get()) {
                break;
            }
        }

        assertTrue(sonicBlastNotified.get(),
                "onSonicBlast must fire when spell releases at ENDING");
    }

    @Test
    void testPoisonFogTimingAndState() {
        Unit nativeChieftain = new Unit(nativePlayer, 16f, 16f, null,
                nativePlayer.getRaceInfo().getUnitTemplate(UnitType.CHIEFTAIN));
        nativeChieftain.register();
        Unit vikingWarrior = new Unit(vikingPlayer, 17f, 16f, null,
                vikingPlayer.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_ROCK));
        vikingWarrior.register();
        int initialHealth = vikingWarrior.getHitPoints();

        nativeChieftain.maxMagicEnergy(MagicType.POISON_FOG);
        nativeChieftain.doMagic(MagicType.POISON_FOG, true);

        // Advance ticks during PREPARING and CASTING (Natives: release at ~3.64s / tick 182)
        for (int i = 0; i < 150; i++) {
            world.tick();
            assertEquals(initialHealth, vikingWarrior.getHitPoints(),
                    "Viking warrior must not take poison damage during PREPARING or CASTING (tick " + i + ")");
        }

        // Advance through release time and into fog damage intervals
        for (int i = 150; i < 650; i++) {
            world.tick();
            if (vikingWarrior.getHitPoints() < initialHealth || vikingWarrior.isDead()) {
                break;
            }
        }

        assertTrue(vikingWarrior.getHitPoints() < initialHealth || vikingWarrior.isDead(),
                "Viking warrior must take damage from PoisonFog after release");
    }

    @Test
    void testLightningCloudTimingAndState() {
        AtomicBoolean lightningStruck = new AtomicBoolean(false);
        world.setNotificationListener(new NotificationListener() {
            @Override
            public void onLightningStrike(Model model, Target target) {
                lightningStruck.set(true);
            }
        });

        Unit nativeChieftain = new Unit(nativePlayer, 16f, 16f, null,
                nativePlayer.getRaceInfo().getUnitTemplate(UnitType.CHIEFTAIN));
        nativeChieftain.register();
        Unit vikingWarrior = new Unit(vikingPlayer, 17f, 16f, null,
                vikingPlayer.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_ROCK));
        vikingWarrior.register();

        nativeChieftain.maxMagicEnergy(MagicType.LIGHTNING_CLOUD);
        nativeChieftain.doMagic(MagicType.LIGHTNING_CLOUD, true);

        // Advance ticks during PREPARING and CASTING
        for (int i = 0; i < 150; i++) {
            world.tick();
            assertFalse(lightningStruck.get(),
                    "Lightning must not strike during PREPARING or CASTING (tick " + i + ")");
        }

        // Advance through release and strike interval
        for (int i = 150; i < 300; i++) {
            world.tick();
            if (lightningStruck.get()) {
                break;
            }
        }

        assertTrue(lightningStruck.get(),
                "Lightning must strike target after cloud releases at ENDING");
    }
}
