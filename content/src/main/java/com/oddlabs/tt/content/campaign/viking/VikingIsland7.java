package com.oddlabs.tt.content.campaign.viking;

import com.oddlabs.tt.content.campaign.Campaign;
import com.oddlabs.tt.content.campaign.CampaignDialogForm;
import com.oddlabs.tt.content.campaign.CampaignState;
import com.oddlabs.tt.content.campaign.InGameCampaignDialogForm;
import com.oddlabs.tt.content.campaign.Island;
import com.oddlabs.tt.simulation.model.Race;

import com.oddlabs.tt.simulation.model.Difficulty;

import com.oddlabs.tt.simulation.model.Terrain;
import com.oddlabs.tt.simulation.model.UnitType;

import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.Origin;
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
 * Campaign level logic for Viking Island 7, containing objectives and triggers.
 */
final class VikingIsland7 extends Island {
    private static final ResourceBundle bundle = ResourceBundle.getBundle(VikingIsland7.class.getName());

    VikingIsland7(Campaign campaign) {
        super(campaign);
    }

    private static String i18n(String key, Object... args) {
        return Utils.getBundleString(bundle, key, args);
    }

    @Override
    public void init(GUIRoot gui_root) {
        String[] ai_names = IntStream.range(0, 6)
                .mapToObj(i -> i18n("name" + i))
                .toArray(String[]::new);
        GameNetwork game_network = startNewGame(gui_root, 512, Terrain.NATIVE, .75f, 1f, .5f,
                725925, 7, VikingCampaign.MAX_UNITS, ai_names);
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
                ai_difficulty = PlayerSlot.AI_EASY;
                yield 5;
            }
            case Difficulty.NORMAL -> {
                ai_difficulty = PlayerSlot.AI_EASY;
                yield 15;
            }
            case Difficulty.HARD -> {
                ai_difficulty = PlayerSlot.AI_HARD;
                yield 20;
            }
            default -> throw new IllegalArgumentException();
        };
        game_network.getClient().getServerInterface().setPlayerSlot(2,
                PlayerSlot.AI,
                Race.NATIVES.getValue(),
                1,
                true,
                ai_difficulty);
        game_network.getClient().setUnitInfo(2, new UnitInfo(true, true, 0, false, ai_peons, 0, 0, 0));
        game_network.getClient().getServerInterface().setPlayerSlot(3,
                PlayerSlot.AI,
                Race.NATIVES.getValue(),
                1,
                true,
                ai_difficulty);
        game_network.getClient().setUnitInfo(3, new UnitInfo(true, true, 0, false, ai_peons, 0, 0, 0));
        game_network.getClient().getServerInterface().startServer();
    }

    @Override
    protected void start() {
        final Player enemy0 = getViewer().getWorld().getPlayers().get(1);
        final Player enemy1 = getViewer().getWorld().getPlayers().get(2);

        // Introduction
        new GameStartedTrigger(getViewer().getWorld(), () -> {
            CampaignDialogForm dialog = new InGameCampaignDialogForm(getViewer(), i18n("header0"),
                    i18n("dialog0"),
                    getCampaign().getIcons().getFaces()[0],
                    Origin.AT_START);
            addModalForm(dialog);
        });

        // Winning condition
        new VictoryTrigger(getViewer(), () -> {
            // Winner prize
            getCampaign().getState().setIslandState(7, CampaignState.ISLAND_COMPLETED);
            getCampaign().getState().setIslandState(6, CampaignState.ISLAND_AVAILABLE);
            getCampaign().getState().setIslandState(8, CampaignState.ISLAND_AVAILABLE);
            getCampaign().getState().setIslandState(9, CampaignState.ISLAND_SEMI_AVAILABLE);
            getCampaign().getState().setIslandState(11, CampaignState.ISLAND_SEMI_AVAILABLE);
            getCampaign().victory(getViewer());
        });

        // Put warrior in tower
        insertGuardTower(enemy0, UnitType.WARRIOR_IRON, 83, 70);
        insertGuardTower(enemy1, UnitType.WARRIOR_IRON, 189, 74);

        // Insert treasures
        float dir = (float) Math.sin(Math.PI / 4);
        placeStatues(i18n("statue"),
                StatuePlacement.atGrid(67, 64, -1, 0, 3),
                StatuePlacement.atGrid(70, 52, -1, 0, 4),
                StatuePlacement.atGrid(77, 63, 0, 1, 1),
                StatuePlacement.atGrid(82, 52, dir, -dir, 3),
                StatuePlacement.atGrid(76, 75, dir, dir, 4),
                StatuePlacement.atGrid(205, 81, dir, dir, 5),
                StatuePlacement.atGrid(199, 42, dir, -dir, 1),
                StatuePlacement.atGrid(197, 69, dir, -dir, 1),
                StatuePlacement.atGrid(194, 77, 0, 1, 3),
                StatuePlacement.atGrid(187, 70, -1, 0, 3),
                StatuePlacement.atGrid(188, 77, -dir, dir, 4),
                StatuePlacement.atGrid(190, 65, 0, -1, 5));
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
