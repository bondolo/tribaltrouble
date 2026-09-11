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
import com.oddlabs.tt.simulation.model.Unit;
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
 * Campaign level logic for Viking Island 14, containing objectives and triggers.
 */
public final class VikingIsland14 extends Island {
    private static final ResourceBundle bundle = ResourceBundle.getBundle(VikingIsland14.class.getName());

    private static String i18n(String key, Object... args) {
        return Utils.getBundleString(bundle, key, args);
    }

    public VikingIsland14(Campaign campaign) {
        super(campaign);
    }

    @Override
    public void init(GUIRoot gui_root) {
        String[] ai_names = IntStream.range(0, 6)
                .mapToObj(i -> i18n("name" + i))
                .toArray(String[]::new);
        // gametype, owner, game, meters_per_world, hills, vegetation_amount, supplies_amount, seed, speed, map_code
        GameNetwork game_network = startNewGame(gui_root, 1024, Terrain.NATIVE, .75f, .65f, .85f,
                25, 14, VikingCampaign.MAX_UNITS, ai_names);
        game_network.getClient().getServerInterface().setPlayerSlot(0,
                PlayerSlot.HUMAN,
                Race.VIKINGS.getValue(),
                0,
                true,
                PlayerSlot.AI_NONE);
        game_network.getClient().setUnitInfo(0, new UnitInfo(false, false, 0, false, 0, 0, 0, 0));
        int ai_difficulty;
        int ai_peons = switch (getCampaign().getState().getDifficulty()) {
            case Difficulty.EASY -> {
                ai_difficulty = PlayerSlot.AI_NORMAL;
                yield 1;
            }
            case Difficulty.NORMAL -> {
                ai_difficulty = PlayerSlot.AI_HARD;
                yield 5;
            }
            case Difficulty.HARD -> {
                ai_difficulty = PlayerSlot.AI_HARD;
                yield 12;
            }
            default -> throw new IllegalArgumentException();
        };
        game_network.getClient().getServerInterface().setPlayerSlot(2,
                PlayerSlot.AI,
                Race.NATIVES.getValue(),
                1,
                true,
                PlayerSlot.AI_HARD);
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
        Runnable runnable;
        final Player local_player = getViewer().getLocalPlayer();
        // Introduction
        runnable = () -> {
            CampaignDialogForm dialog = new InGameCampaignDialogForm(getViewer(), i18n("header0"),
                    i18n("dialog0"),
                    getCampaign().getIcons().getFaces()[0],
                    Origin.AT_START);
            addModalForm(dialog);
        };
        new GameStartedTrigger(getViewer().getWorld(), runnable);

        // Insert viking men
        int start_x = 236 * 2;
        int start_y = 362 * 2;
        ResourceBundle player_bundle = ResourceBundle.getBundle("com.oddlabs.tt.content.Player");
        local_player.setActiveChieftain(new Unit(local_player, start_x, start_y, null, local_player.getRaceInfo()
                .getUnitTemplate(UnitType.CHIEFTAIN), Utils.getBundleString(player_bundle, "chieftain_name"), false));
        local_player.getChieftain().ifPresent(chieftain -> chieftain.getOwner().getRaceInfo().getMagics().forEach(
                chieftain::maxMagicEnergy));

        for (int i = 0; i < getCampaign().getState().getNumPeons(); i++) {
            new Unit(local_player, start_x, start_y, null, local_player.getRaceInfo().getUnitTemplate(UnitType.PEON));
        }
        for (int i = 0; i < getCampaign().getState().getNumRockWarriors(); i++) {
            new Unit(local_player, start_x, start_y, null, local_player.getRaceInfo().getUnitTemplate(
                    UnitType.WARRIOR_ROCK));
        }
        for (int i = 0; i < getCampaign().getState().getNumIronWarriors(); i++) {
            new Unit(local_player, start_x, start_y, null, local_player.getRaceInfo().getUnitTemplate(
                    UnitType.WARRIOR_IRON));
        }
        for (int i = 0; i < getCampaign().getState().getNumRubberWarriors(); i++) {
            new Unit(local_player, start_x, start_y, null, local_player.getRaceInfo().getUnitTemplate(
                    UnitType.WARRIOR_RUBBER));
        }

        // Move start position (for the camera)
        getViewer().getCamera().reset(start_x, start_y);

        // Winner prize
        runnable = () -> {
            getCampaign().getState().setIslandState(14, CampaignState.ISLAND_COMPLETED);
            getCampaign().victory(getViewer());
        };

        // Winning condition
        new VictoryTrigger(getViewer(), runnable);

        // Insert treasures
        float dir = (float) Math.sin(Math.PI / 4);
        placeStatues(i18n("statue"),
                StatuePlacement.atGrid(163, 126, 0, 1, 0, StatuePlacement.LARGE_SHADOW_DIAMETER),
                StatuePlacement.atGrid(130, 124, -dir, -dir, 3),
                StatuePlacement.atGrid(152, 138, dir, dir, 1),
                StatuePlacement.atGrid(152, 144, 0, 1, 3),
                StatuePlacement.atGrid(140, 140, 0, 1, 4),
                StatuePlacement.atGrid(143, 116, 0, -1, 1),
                StatuePlacement.atGrid(142, 131, dir, -dir, 5),
                StatuePlacement.atGrid(423, 174, 0, 1, 1),
                StatuePlacement.atGrid(408, 161, -1, 0, 3),
                StatuePlacement.atGrid(426, 156, dir, -dir, 5),
                StatuePlacement.atGrid(418, 165, 0, 1, 1),
                StatuePlacement.atGrid(430, 165, 1, 0, 3),
                StatuePlacement.atGrid(419, 170, -dir, dir, 4),
                StatuePlacement.atGrid(416, 156, 0, -1, 5));
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
