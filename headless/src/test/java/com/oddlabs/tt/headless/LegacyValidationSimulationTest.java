package com.oddlabs.tt.headless;

import com.oddlabs.tt.simulation.landscape.IslandConfig;
import com.oddlabs.tt.simulation.landscape.WorldParameters;
import com.oddlabs.tt.simulation.model.Race;
import com.oddlabs.tt.simulation.model.Terrain;
import com.oddlabs.tt.simulation.player.Player;
import com.oddlabs.tt.simulation.player.PlayerSlot;
import com.oddlabs.tt.simulation.player.UnitInfo;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests verifying bit-exact deterministic parity with legacy game execution.
 */
@Tag("integration")
class LegacyValidationSimulationTest {

    @Test
    void testLegacySeed42RunToVictory() {
        int seed = 42;
        IslandConfig islandConfig = new IslandConfig(
                Terrain.NATIVE,
                256,
                0.5f,
                0.5f,
                0.5f,
                seed
        );

        WorldParameters worldParameters = new WorldParameters(
                2,
                "42",
                Player.INITIAL_UNIT_COUNT,
                100
        );

        UnitInfo battleUnits = new UnitInfo(false, false, 0, false, Player.INITIAL_UNIT_COUNT, 0, 0, 0);

        List<HeadlessMatchConfig.PlayerConfig> players = List.of(
                new HeadlessMatchConfig.PlayerConfig(0, Race.NATIVES, PlayerSlot.AI_NORMAL, "Player", battleUnits),
                new HeadlessMatchConfig.PlayerConfig(1, Race.VIKINGS, PlayerSlot.AI_NORMAL, "Bot-Natives", battleUnits)
        );

        HeadlessMatchConfig config = new HeadlessMatchConfig(
                islandConfig,
                worldParameters,
                players,
                50_000,
                50
        );

        LegacyValidationMatchRunner runner = new LegacyValidationMatchRunner();
        HeadlessMatchResult result = runner.run(config);

        System.out.println("Result: victory=" + result.victory()
                + ", winningTeam=" + result.winningTeam()
                + ", finishTick=" + result.finalTick()
                + ", checksum=" + result.finalChecksum());

        assertTrue(result.victory(), "Match should conclude in victory");
        assertEquals(1, result.winningTeam(), "Native bot (Team 1) must win");
        assertEquals(30_865, result.finalTick(), "Winning tick must match legacy baseline");
        assertEquals(228_323_619, result.finalChecksum(), "World checksum must match legacy baseline");
    }
}
