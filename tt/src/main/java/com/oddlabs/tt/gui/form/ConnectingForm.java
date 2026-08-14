package com.oddlabs.tt.gui.form;

import com.oddlabs.matchmaking.Game;
import com.oddlabs.tt.gui.CancelButton;
import com.oddlabs.tt.gui.Form;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.HorizButton;
import com.oddlabs.tt.gui.Label;
import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.simulation.model.Race;
import com.oddlabs.tt.net.Client;
import com.oddlabs.tt.net.ConfigurationListener;
import com.oddlabs.tt.net.GameNetwork;
import com.oddlabs.tt.simulation.player.PlayerSlot;
import com.oddlabs.tt.simulation.landscape.WorldGenerator;
import com.oddlabs.tt.base.util.Utils;
import org.jspecify.annotations.NonNull;

import java.util.ResourceBundle;
import java.util.concurrent.ThreadLocalRandom;

import static com.oddlabs.tt.gui.Placement.BOTTOM_MID;

/**
 * UI form shown while establishing a multiplayer connection or initializing a local game.
 */
public final class ConnectingForm extends Form implements ConfigurationListener {
    private static final ResourceBundle bundle = ResourceBundle.getBundle(ConnectingForm.class.getName());

    private @NonNull String i18n(@NonNull String key, @NonNull Object @NonNull... args) {
        return Utils.getBundleString(bundle, key, args);
    }

    private final MultiplayerLobby owner;
    private final boolean multiplayer;
    private final GUIRoot gui_root;
    private final GameNetwork game_network;

    public ConnectingForm(GameNetwork game_network, GUIRoot gui_root, MultiplayerLobby owner, boolean multiplayer) {
        this.game_network = game_network;
        this.gui_root = gui_root;
        this.owner = owner;
        this.multiplayer = multiplayer;

        Label info_label = new Label(i18n(multiplayer ? "connecting" : "starting"), Skin.getSkin().getHeadlineFont());
        addChild(info_label);
        HorizButton cancel_button = new CancelButton(120);
        addChild(cancel_button);
        cancel_button.addMouseClickListener((_, _, _, _) -> this.cancel());

        // Place objects
        info_label.place();
        cancel_button.place(info_label, BOTTOM_MID);

        // headline
        compileCanvas();
        centerPos();
    }

    @Override
    public void connected(@NonNull Client client, @NonNull Game game, @NonNull WorldGenerator generator,
            int player_slot) {
        if (multiplayer) {
            Race race = Race.values()[ThreadLocalRandom.current().nextInt(Race.values().length)];
            int team = player_slot;
            if (game.isRated())
                team = player_slot % 2;
            client.getServerInterface().setPlayerSlot(player_slot, PlayerSlot.HUMAN, race.getValue(), team, false,
                    PlayerSlot.AI_NONE);
            remove();
            owner.createGameMenu(game_network, game, generator, player_slot);
//			GameMenu panel = new GameMenu(owner, game, generator, player_slot);
//			owner.setGameMenu(panel);
//			Network.setConfigurationListener(panel);
        } else {
            assert player_slot == 0 : "player_slot must be 0";
        }
    }

    public void chat(int player_slot, String chat) {
    }

    @Override
    public void setPlayers(PlayerSlot[] players) {
        assert !multiplayer;
    }

    @Override
    public void connectionLost() {
        remove();
        gui_root.addModalForm(new MessageForm(i18n("connection_lost")));
    }

    @Override
    public void gameStarted(com.oddlabs.tt.base.util.@NonNull LoadCallback loadCallback) {
        remove();
        ProgressForm.setProgressForm(game_network.getClient().getNetwork(), gui_root.getGUI(),
                (LoadCallback) loadCallback);
        assert !multiplayer;
    }

    @Override
    protected void doCancel() {
        game_network.close();
    }
}
