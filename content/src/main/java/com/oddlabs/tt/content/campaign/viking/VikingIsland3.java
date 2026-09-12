package com.oddlabs.tt.content.campaign.viking;

import com.oddlabs.tt.content.campaign.Campaign;
import com.oddlabs.tt.content.campaign.CampaignDialogForm;
import com.oddlabs.tt.content.campaign.CampaignState;
import com.oddlabs.tt.content.campaign.InGameCampaignDialogForm;
import com.oddlabs.tt.content.campaign.Island;
import com.oddlabs.tt.simulation.model.Race;

import com.oddlabs.tt.simulation.model.Difficulty;

import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.Origin;
import com.oddlabs.tt.simulation.model.Terrain;
import com.oddlabs.tt.net.GameNetwork;
import com.oddlabs.tt.simulation.player.PlayerSlot;
import com.oddlabs.tt.simulation.player.Player;
import com.oddlabs.tt.simulation.player.UnitInfo;
import com.oddlabs.tt.simulation.trigger.GameStartedTrigger;
import com.oddlabs.tt.client.trigger.VictoryTrigger;
import com.oddlabs.tt.base.util.Utils;

import java.util.ResourceBundle;
import java.util.stream.IntStream;

/**
 * Campaign level logic for Viking Island 3, containing objectives and triggers.
 */
final class VikingIsland3 extends Island {
    private static final ResourceBundle bundle = ResourceBundle.getBundle(VikingIsland3.class.getName());

    private static String i18n(String key, Object... args) {
        return Utils.getBundleString(bundle, key, args);
    }

    VikingIsland3(Campaign campaign) {
        super(campaign);
    }

    @Override
    public void init(GUIRoot gui_root) {
        String[] ai_names = IntStream.range(0, 6)
                .mapToObj(i -> i18n("name" + i))
                .toArray(String[]::new);
        // gametype, owner, game, meters_per_world, hills, vegetation_amount, supplies_amount, seed, speed, map_code
        GameNetwork game_network = startNewGame(gui_root, 512, Terrain.NATIVE, .75f, 1f, .5f,
                96443, 3, VikingCampaign.MAX_UNITS, ai_names);
        game_network.getClient().getServerInterface().setPlayerSlot(0,
                PlayerSlot.HUMAN,
                Race.VIKINGS.getValue(),
                0,
                true,
                PlayerSlot.AI_NONE);
        game_network.getClient().setUnitInfo(0,
                new UnitInfo(false, false, 0, true,
                        getCampaign().getState().getNumPeons(),
                        getCampaign().getState().getNumRockWarriors(),
                        getCampaign().getState().getNumIronWarriors(),
                        getCampaign().getState().getNumRubberWarriors()));
        int ai_difficulty;
        int ai_peons = switch (getCampaign().getState().getDifficulty()) {
            case Difficulty.EASY -> {
                ai_difficulty = PlayerSlot.AI_NORMAL;
                yield 2;
            }
            case Difficulty.NORMAL -> {
                ai_difficulty = PlayerSlot.AI_HARD;
                yield 5;
            }
            case Difficulty.HARD -> {
                ai_difficulty = PlayerSlot.AI_HARD;
                yield 15;
            }
            default -> throw new IllegalArgumentException();
        };
        game_network.getClient().getServerInterface().setPlayerSlot(2,
                PlayerSlot.AI,
                Race.NATIVES.getValue(),
                1,
                true,
                ai_difficulty);
        game_network.getClient().setUnitInfo(2, new UnitInfo(true, true, 1, false, ai_peons, 0, 5, 0));
        game_network.getClient().getServerInterface().startServer();
    }

    @Override
    protected void start() {
        Runnable runnable;
        final Player enemy = getViewer().getWorld().getPlayers().get(1);

        // Introduction
        runnable = () -> {
            CampaignDialogForm dialog = new InGameCampaignDialogForm(getViewer(), i18n("header0"),
                    i18n("dialog0"),
                    getCampaign().getIcons().getFaces()[0],
                    Origin.AT_START);
            addModalForm(dialog);
        };
        new GameStartedTrigger(getViewer().getWorld(), runnable);

        // Winner prize
        runnable = () -> {
            getCampaign().getState().setIslandState(3, CampaignState.ISLAND_COMPLETED);
            getCampaign().getState().setIslandState(2, CampaignState.ISLAND_AVAILABLE);
            getCampaign().getState().setIslandState(4, CampaignState.ISLAND_AVAILABLE);
            getCampaign().getState().setIslandState(8, CampaignState.ISLAND_AVAILABLE);
            getCampaign().victory(getViewer());
        };
        // Winning condition
        new VictoryTrigger(getViewer(), runnable);

        // Put warrior in tower
        enemy.getAI().ifPresent(ai -> ai.manTowers(1)); // TODO: exchange with insertGuardTower()

        // Insert treasures
        float dir = (float) Math.sin(Math.PI / 4);
        placeStatues(i18n("statue"),
                StatuePlacement.atGrid(134, 29, dir, -dir, 4),
                StatuePlacement.atGrid(130, 28, 0, -1, 1),
                StatuePlacement.atGrid(130, 34, 0, 1, 3),
                StatuePlacement.atGrid(125, 37, 0, 1, 1),
                StatuePlacement.atGrid(121, 32, -1, 0, 5),
                StatuePlacement.atGrid(124, 28, -dir, -dir, 3),
                StatuePlacement.atGrid(136, 38, dir, dir, 4),
                StatuePlacement.atGrid(139, 33, 1, 0, 1));
    }

    @Override
    public CharSequence getHeader() {
        return i18n("header");
    }

    @Override
    public CharSequence getDescription() {
        return i18n("description");
    }

    @Override
    public CharSequence getCurrentObjective() {
        return i18n("objective");
    }
}
