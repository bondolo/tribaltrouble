package com.oddlabs.tt.content.menu;

import com.oddlabs.matchmaking.ChatRoomEntry;
import com.oddlabs.matchmaking.Game;
import com.oddlabs.matchmaking.GameHost;
import com.oddlabs.matchmaking.GameSession;
import com.oddlabs.matchmaking.MatchmakingServerInterface;
import com.oddlabs.matchmaking.Profile;
import com.oddlabs.matchmaking.RankingEntry;
import com.oddlabs.net.NetworkSelector;
import com.oddlabs.tt.gui.Form;
import com.oddlabs.tt.content.form.GameInfoForm;
import com.oddlabs.tt.gui.MessageForm;
import com.oddlabs.tt.content.form.MultiplayerLobby;
import com.oddlabs.tt.content.skirmish.MultiplayerInGameInfo;
import com.oddlabs.tt.content.form.ChatPanel;
import com.oddlabs.tt.net.ChatRoomInfo;
import com.oddlabs.tt.gui.ColumnInfo;
import com.oddlabs.tt.gui.FocusDirection;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.Group;
import com.oddlabs.tt.gui.HorizButton;
import com.oddlabs.tt.gui.IntegerLabel;
import com.oddlabs.tt.gui.Label;
import com.oddlabs.tt.gui.MouseButton;
import com.oddlabs.tt.gui.MultiColumnComboBox;
import com.oddlabs.tt.gui.Panel;
import com.oddlabs.tt.gui.PanelGroup;
import com.oddlabs.tt.gui.PulldownItem;
import com.oddlabs.tt.gui.PulldownMenu;
import com.oddlabs.tt.gui.Row;
import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.gui.event.EnterListener;
import com.oddlabs.tt.gui.event.FocusListener;
import com.oddlabs.tt.gui.event.ItemChosenListener;
import com.oddlabs.tt.gui.event.MouseClickListener;
import com.oddlabs.tt.gui.event.RowListener;
import com.oddlabs.tt.content.skirmish.TerrainMenu;
import com.oddlabs.tt.content.skirmish.TerrainMenuListener;
import com.oddlabs.tt.client.viewer.ChatCommand;
import com.oddlabs.tt.net.GameNetwork;
import com.oddlabs.tt.net.MatchmakingListener;
import com.oddlabs.tt.net.ServerMessageBundler;
import com.oddlabs.tt.base.util.Utils;
import com.oddlabs.tt.engine.font.Font;
import com.oddlabs.tt.engine.render.Renderer;
import com.oddlabs.tt.simulation.landscape.WorldGenerator;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

import static com.oddlabs.tt.gui.Origin.AT_END;
import static com.oddlabs.tt.gui.Placement.BOTTOM_LEFT;
import static com.oddlabs.tt.gui.Placement.RIGHT_MID;

/** Lobby menu for selecting multiplayer games and chat rooms. */
public final class SelectGameMenu extends Form implements MatchmakingListener, TerrainMenuListener, MultiplayerLobby {
    private static final int BUTTON_WIDTH_SHORT = 60;
    private static final int BUTTON_WIDTH = 110;
    private static final int BUTTON_WIDTH_LONG = 150;
    private static final int BUTTON_WIDTH_EXTRA_LONG = 170;

    private static final int PANEL_INDEX_GAME = 0;
    private static final int PANEL_INDEX_CHAT = 1;
    private static final int PANEL_INDEX_HIGHSCORE = 2;

    private final Menu main_menu;
    private final ProfilesForm profiles_form;
    private final Panel[] panels = new Panel[3];

    // List of games
    private final Panel game_list_panel;
    private final MultiColumnComboBox<GameHost> game_list_box;
    private final List<GameHost> game_hosts = new ArrayList<>();

    // List of chat rooms
    private final Panel chat_room_list_panel;
    private final MultiColumnComboBox<ChatRoomEntry> chat_room_list_box;
    private final List<ChatRoomEntry> chat_rooms = new ArrayList<>();
    private final GUIRoot gui_root;
    private final NetworkSelector network;

    private final MultiColumnComboBox<RankingEntry> ranking_list_box;

    private final int game_name_size;
    private final int user_name_size;
    private final int room_name_size;

    private static final ResourceBundle bundle = ResourceBundle.getBundle(SelectGameMenu.class.getName());

    private String i18n(String key, Object... args) {
        return Utils.getBundleString(bundle, key, args);
    }

    private GameMenu game_panel;
    private @Nullable ChatPanel chat_panel;
    private PanelGroup panel_group;

    public SelectGameMenu(NetworkSelector network, GUIRoot gui_root, Menu main_menu) {
        this(network, gui_root, main_menu, 0);
    }

    public SelectGameMenu(NetworkSelector network, GUIRoot gui_root, Menu main_menu,
            int panel_index) {
        this.main_menu = main_menu;
        this.gui_root = gui_root;
        this.network = network;

        // Game panel
        game_list_panel = new Panel(i18n("games_caption"));
        Label label_headline = new Label(i18n("multiplayer_caption"), Skin.getSkin().getHeadlineFont());
        game_list_panel.addChild(label_headline);
        game_list_panel.addFocusListener(new GameListPanelListener());
        game_name_size = 340;
        ColumnInfo[] infos = new ColumnInfo[]{
                new ColumnInfo(i18n("game_name"), game_name_size),
                new ColumnInfo(i18n("rated"), 120),
                new ColumnInfo(i18n("speed"), 120),
                new ColumnInfo(i18n("map_size"), 120)};
        game_list_box = new MultiColumnComboBox<>(gui_root, infos, 350);
        game_list_box.addRowListener(new GameDoubleClickedListener());
        game_list_panel.addChild(game_list_box);

        PulldownMenu<GameHost> game_list_pulldown_menu = new PulldownMenu<>();
        game_list_pulldown_menu.addItem(new PulldownItem<>(i18n("join")));
        game_list_pulldown_menu.addItem(new PulldownItem<>(i18n("game_info")));
        game_list_pulldown_menu.addItemChosenListener(new PulldownListener());
        game_list_box.setPulldownMenu(game_list_pulldown_menu);

        HorizButton update_list_button = new HorizButton(i18n("update_list"), BUTTON_WIDTH_EXTRA_LONG);
        game_list_panel.addChild(update_list_button);
        update_list_button.addMouseClickListener(new UpdateGameListListener());

        HorizButton create_button = new HorizButton(i18n("create_game"), BUTTON_WIDTH_LONG);
        game_list_panel.addChild(create_button);
        create_button.addMouseClickListener(new CreateGameListener());

        HorizButton join_button = new HorizButton(i18n("join_game"), BUTTON_WIDTH);
        game_list_panel.addChild(join_button);
        join_button.addMouseClickListener((_, _, _, _) -> {
            GameHost selected_game = game_list_box.getSelected();
            joinGame(selected_game);
        });

        // Place game panel objects
        label_headline.place();
        game_list_box.place(label_headline, BOTTOM_LEFT);

        update_list_button.place(game_list_box, BOTTOM_LEFT);
        create_button.place(update_list_button, RIGHT_MID);
        join_button.place(create_button, RIGHT_MID);

        game_list_panel.compileCanvas();
        panels[PANEL_INDEX_GAME] = game_list_panel;

        // League panel
        Panel highscore_list_panel = new Panel(i18n("league_caption"));
        label_headline = new Label(i18n("league_description"), Skin.getSkin().getHeadlineFont());
        highscore_list_panel.addChild(label_headline);
        user_name_size = 250;
        ColumnInfo[] score_infos = new ColumnInfo[]{
                new ColumnInfo(i18n("rank"), 50),
                new ColumnInfo(i18n("name"), user_name_size),
                new ColumnInfo(i18n("rating"), 100),
                new ColumnInfo(i18n("wins"), 100),
                new ColumnInfo(i18n("losses"), 100),
                new ColumnInfo(i18n("invalid"), 100)};
        ranking_list_box = new MultiColumnComboBox<>(gui_root, score_infos, 350);
        highscore_list_panel.addChild(ranking_list_box);

        HorizButton update_scores_button = new HorizButton(i18n("update_scores"), BUTTON_WIDTH_EXTRA_LONG);
        highscore_list_panel.addChild(update_scores_button);
        update_scores_button.addMouseClickListener(new UpdateScoresListener());

        // Place score panel objects
        label_headline.place();
        ranking_list_box.place(label_headline, BOTTOM_LEFT);

        update_scores_button.place(ranking_list_box, BOTTOM_LEFT);

        highscore_list_panel.compileCanvas();
        panels[PANEL_INDEX_HIGHSCORE] = highscore_list_panel;

        // Chat room list panel
        chat_room_list_panel = new Panel(i18n("chat_caption"));
        label_headline = new Label(i18n("chat_rooms_caption"), Skin.getSkin().getHeadlineFont());
        chat_room_list_panel.addChild(label_headline);

        room_name_size = 600;
        infos = new ColumnInfo[]{
                new ColumnInfo(i18n("room"), room_name_size),
                new ColumnInfo(i18n("users"), 100)};
        chat_room_list_box = new MultiColumnComboBox<>(gui_root, infos, 350);
        chat_room_list_box.addRowListener(new RoomDoubleClickedListener());
        chat_room_list_panel.addChild(chat_room_list_box);

        update_list_button = new HorizButton(i18n("update_rooms"), BUTTON_WIDTH_EXTRA_LONG);
        chat_room_list_panel.addChild(update_list_button);
        update_list_button.addMouseClickListener(new UpdateRoomListListener());

        create_button = new HorizButton(i18n("create_room"), BUTTON_WIDTH_LONG);
        chat_room_list_panel.addChild(create_button);
        create_button.addMouseClickListener(new CreateRoomListener());

        join_button = new HorizButton(i18n("join_room"), BUTTON_WIDTH);
        chat_room_list_panel.addChild(join_button);
        join_button.addMouseClickListener(new JoinRoomListener());

        // Place chat room list panel
        label_headline.place();
        chat_room_list_box.place(label_headline, BOTTOM_LEFT);
        update_list_button.place(chat_room_list_box, BOTTOM_LEFT);
        create_button.place(update_list_button, RIGHT_MID);
        join_button.place(create_button, RIGHT_MID);
        chat_room_list_panel.compileCanvas();

        // Common
        ChatRoomInfo info = Renderer.getRenderer().getNetwork().getMatchmakingClient().getChatRoomInfo();
        if (info != null) {
            chat_panel = createChatRoomPanel(info);
            panels[PANEL_INDEX_CHAT] = chat_panel;
        } else {
            panels[PANEL_INDEX_CHAT] = chat_room_list_panel;
        }
        panel_group = new PanelGroup(panel_index, panels);
        addChild(panel_group);

        HorizButton logout_button = new HorizButton(i18n("logout"), BUTTON_WIDTH);
        addChild(logout_button);
        logout_button.addMouseClickListener((_, _, _, _) -> this.cancel());

        panel_group.place();
        logout_button.place(AT_END);
        compileCanvas();

        Renderer.getRenderer().getNetwork().setMatchmakingListener(this);
        updateList(MatchmakingServerInterface.TYPE_GAME);
        updateList(MatchmakingServerInterface.TYPE_CHAT_ROOM_LIST);
        updateList(MatchmakingServerInterface.TYPE_RANKING_LIST);

        profiles_form = new ProfilesForm(gui_root, main_menu, this);
        if (Renderer.getRenderer().getNetwork().getMatchmakingClient().getProfile() == null) {
            main_menu.setMenuCentered(profiles_form);
            Renderer.getRenderer().getNetwork().getMatchmakingClient().requestProfiles();
        } else {
            main_menu.setMenuCentered(this);
        }
    }

    private void setPanel(int index, Panel panel) {
        panels[index] = panel;
        PanelGroup temp_group = new PanelGroup(index, panels);
        temp_group.setPos(panel_group.getX(), panel_group.getY());
        panel_group.remove();
        panel_group = temp_group;
        addChild(panel_group);
        panel.setFocus();
    }

    private ChatPanel createChatRoomPanel(ChatRoomInfo info) {
        ChatPanel panel = new ChatPanel(gui_root, info, chat_room_list_panel.getWidth(), chat_room_list_panel
                .getHeight(), BUTTON_WIDTH_SHORT, new SendChatListener(), (_, _, _, _) -> leaveChatRoom());
        var chatHub = Renderer.getRenderer().getNetwork().getChatHub();
        chatHub.setIgnoreFilter(ChatCommand::isIgnoring);
        chatHub.addListener(panel);
        return panel;
    }

    @Override
    public void createGameMenu(GameNetwork game_network, Game game, WorldGenerator generator,
            int player_slot) {
        game_panel = new GameMenu(game_network, gui_root, this, game, generator, player_slot, game_list_panel
                .getWidth(), game_list_panel.getHeight(), BUTTON_WIDTH);
        setGameMenu(game_panel);
        game_network.getClient().setConfigurationListener(game_panel);
    }

    @Override
    public void setFocus(FocusDirection direction) {
        if (direction == FocusDirection.BACKWARD) {
            super.setFocus(direction);
        } else {
            game_list_panel.setFocus(direction);
        }
    }

    private static void updateList(int type) {
        Renderer.getRenderer().getNetwork().getMatchmakingClient().requestList(type);
    }

    @Override
    public void connectionLost() {
        leaveChatRoom();
        remove();
        profiles_form.connectionLost();
        gui_root.addModalForm(new MessageForm(i18n("connection_lost")));
    }

    @Override
    public void loggedIn() {
        assert false;
    }

    @Override
    public void loginError(int error_code) {
        assert false;
    }

    @Override
    public void terrainMenuCancel() {
        setPanel(PANEL_INDEX_GAME, game_list_panel);
    }

    @Override
    public void terrainMenuOK() {

    }

    private void setGameMenu(Panel panel) {
        updateList(MatchmakingServerInterface.TYPE_GAME);
        setPanel(PANEL_INDEX_GAME, panel);
    }

    public void removeGameMenu() {
        setPanel(PANEL_INDEX_GAME, game_list_panel);
    }

    @Override
    public void joinedChat(ChatRoomInfo info) {
        if (chat_panel != null) {
            chat_panel.connectionLost();
            Renderer.getRenderer().getNetwork().getChatHub().removeListener(chat_panel);
        }
        chat_panel = createChatRoomPanel(info);
        setPanel(PANEL_INDEX_CHAT, chat_panel);
    }

    @Override
    protected void doRemove() {
        super.doRemove();
        Renderer.getRenderer().getNetwork().getChatHub().removeListener(chat_panel);
    }

    @Override
    public void receivedProfiles(Profile[] profiles, String last_nick) {
        profiles_form.receivedProfiles(profiles, last_nick);
    }

    @Override
    public void updateChatRoom(ChatRoomInfo info) {
        chat_panel.update(info);
    }

    @Override
    public void receivedList(int type, Object[] names) {
        switch (type) {
            case MatchmakingServerInterface.TYPE_GAME -> {
                game_hosts.addAll(Arrays.asList((GameHost[]) names));
                updateGameListGUI();
            }
            case MatchmakingServerInterface.TYPE_CHAT_ROOM_LIST -> {
                chat_rooms.addAll(Arrays.asList((ChatRoomEntry[]) names));
                updateChatRoomListGUI();
            }
            case MatchmakingServerInterface.TYPE_RANKING_LIST -> {
                for (Object name : names) {
                    updateRankingList((RankingEntry) name);
                }
            }
            default -> throw new IllegalArgumentException("Unexpected list type " + type);
        }
    }

    @Override
    public void clearList(int type) {
        switch (type) {
            case MatchmakingServerInterface.TYPE_GAME -> {
                game_hosts.clear();
                game_list_box.clear();
            }
            case MatchmakingServerInterface.TYPE_CHAT_ROOM_LIST -> {
                chat_rooms.clear();
                chat_room_list_box.clear();
            }
            case MatchmakingServerInterface.TYPE_RANKING_LIST -> ranking_list_box.clear();
            default -> throw new IllegalArgumentException("Unexpected list type " + type);
        }
    }

    private void updateRankingList(RankingEntry ranking) {
        Row<RankingEntry, Label> row = new Row<>(List.of(
                new IntegerLabel(ranking.getRanking(), Skin.getSkin().getMultiColumnComboBoxData().font()),
                new Label(ranking.getName(), Skin.getSkin().getMultiColumnComboBoxData().font(), user_name_size),
                new IntegerLabel(ranking.getRating(), Skin.getSkin().getMultiColumnComboBoxData().font()),
                new IntegerLabel(ranking.getWins(), Skin.getSkin().getMultiColumnComboBoxData().font()),
                new IntegerLabel(ranking.getLosses(), Skin.getSkin().getMultiColumnComboBoxData().font()),
                new IntegerLabel(ranking.getInvalid(), Skin.getSkin().getMultiColumnComboBoxData().font())), ranking);
        ranking_list_box.addRow(row);
    }

    private void updateGameListGUI() {
        Font combofont = Skin.getSkin().getMultiColumnComboBoxData().font();
        for (GameHost game_host : game_hosts) {
            String rated = ServerMessageBundler.getRatedString(game_host.getGame().isRated());
            String size = ServerMessageBundler.getSizeString(game_host.getGame().getSize());
            Row<GameHost, Label> row = new Row<>(List.of(
                    new Label(game_host.getGame().getName(), combofont, game_name_size),
                    new Label(rated, combofont),
                    new Label(ServerMessageBundler.getGamespeedString(game_host.getGame().getGamespeed()), combofont),
                    new Label(size, combofont)),
                    game_host);
            game_list_box.addRow(row);
        }
    }

    private void updateChatRoomListGUI() {
        Font combofont = Skin.getSkin().getMultiColumnComboBoxData().font();
        for (ChatRoomEntry chat_room_info : chat_rooms) {
            String users_and_max = i18n("users_and_max", chat_room_info.getNumJoined(),
                    MatchmakingServerInterface.MAX_ROOM_USERS);
            Row<ChatRoomEntry, Label> row = new Row<>(List.of(
                    new Label(chat_room_info.getName(), combofont, room_name_size),
                    new Label(users_and_max, combofont)),
                    chat_room_info);
            chat_room_list_box.addRow(row);
        }
    }

    @Override
    protected void doCancel() {
        leaveChatRoom();
        if (game_panel != null)
            game_panel.cancel();
        Renderer.getRenderer().getNetwork().getMatchmakingClient().close();
    }

    private void leaveChatRoom() {
        Renderer.getRenderer().getNetwork().getMatchmakingClient().leaveChatRoom();
        if (chat_panel != null)
            chat_panel.connectionLost();
        chat_panel = null;
        setPanel(PANEL_INDEX_CHAT, chat_room_list_panel);
    }

    private void joinGame(@Nullable GameHost selected_game) {
        if (Renderer.getRenderer().getNetwork().getMatchmakingClient().getProfile() != null) {
            if (selected_game != null) {
                boolean rated = selected_game.getGame().isRated();
                if (rated && Renderer.getRenderer().getNetwork().getMatchmakingClient().getProfile().getWins()
                        < GameSession.MIN_WINS_FOR_RANKING) {
                    String min_wins = i18n("min_wins", GameSession.MIN_WINS_FOR_RANKING);
                    gui_root.addModalForm(new MessageForm(min_wins));
                } else {
                    Game game = selected_game.getGame();
                    main_menu.joinGame(network, gui_root.getGUI(), selected_game.getHostID(), game
                            .getGamespeed(), game.getMapcode(), this, new MultiplayerInGameInfo(game
                                    .getRandomStartPos(), game.isRated()), game.getMaxUnitCount());
                }
            }
        }
    }

    private void joinRoom(@Nullable ChatRoomEntry chat_room_info) {
        if (Renderer.getRenderer().getNetwork().getMatchmakingClient().getProfile() != null) {
            if (chat_room_info != null)
                Renderer.getRenderer().getNetwork().getMatchmakingClient().joinRoom(chat_room_info.getName());
        }
    }

    private final class RoomDoubleClickedListener implements RowListener<ChatRoomEntry> {
        @Override
        public void rowDoubleClicked(ChatRoomEntry chat_room_info) {
            joinRoom(chat_room_info);
        }
    }

    private static final class GameListPanelListener implements FocusListener {
        @Override
        public void activated(boolean activated) {
            if (activated)
                updateList(MatchmakingServerInterface.TYPE_GAME);
        }
    }

    private final class GameDoubleClickedListener implements RowListener<GameHost> {
        @Override
        public void rowDoubleClicked(GameHost selected_game) {
            joinGame(selected_game);
        }
    }

    private static final class UpdateScoresListener implements MouseClickListener {
        @Override
        public void mouseClicked(MouseButton button, int x, int y, int clicks) {
            updateList(MatchmakingServerInterface.TYPE_RANKING_LIST);
        }
    }

    private static final class UpdateGameListListener implements MouseClickListener {
        @Override
        public void mouseClicked(MouseButton button, int x, int y, int clicks) {
            updateList(MatchmakingServerInterface.TYPE_GAME);
        }
    }

    private final class CreateGameListener implements MouseClickListener {
        @Override
        public void mouseClicked(MouseButton button, int x, int y, int clicks) {
            if (Renderer.getRenderer().getNetwork().getMatchmakingClient().getProfile() != null) {
                Panel panel = new Panel(i18n("game"));
                Group g = new TerrainMenu(network, gui_root, main_menu.getAudioManager(), main_menu, true,
                        SelectGameMenu.this);
                panel.addChild(g);
                g.place();
                panel.compileCanvas();
                setPanel(PANEL_INDEX_GAME, panel);
            }
        }
    }

    private final class JoinRoomListener implements MouseClickListener {
        @Override
        public void mouseClicked(MouseButton button, int x, int y, int clicks) {
            ChatRoomEntry chat_room_info = chat_room_list_box.getSelected();
            joinRoom(chat_room_info);
        }
    }

    private static final class UpdateRoomListListener implements MouseClickListener {
        @Override
        public void mouseClicked(MouseButton button, int x, int y, int clicks) {
            updateList(MatchmakingServerInterface.TYPE_CHAT_ROOM_LIST);
        }
    }

    private final class CreateRoomListener implements MouseClickListener {
        @Override
        public void mouseClicked(MouseButton button, int x, int y, int clicks) {
            if (Renderer.getRenderer().getNetwork().getMatchmakingClient().getProfile() != null) {
                main_menu.setMenuCentered(new CreateChatRoomForm(gui_root, main_menu, SelectGameMenu.this));
            }
        }
    }

    private final class SendChatListener implements EnterListener {
        @Override
        public void enterPressed(CharSequence text) {
            String chat = text.toString();
            if (!chat.isEmpty()) {
                var matchmaker = Renderer.getRenderer().getNetwork().getMatchmakingClient();
                if (!ChatCommand.filterCommand(gui_root.getInfoPrinter(), matchmaker, chat)) {
                    matchmaker.getInterface().sendMessageToRoom(chat);
                }
            }
        }
    }

    private final class PulldownListener implements ItemChosenListener<GameHost> {
        @Override
        public void itemChosen(PulldownMenu<GameHost> menu, int item_index) {
            GameHost host = SelectGameMenu.this.game_list_box.getRightClickedRowData();
            switch (item_index) {
                case 0 -> //Join
                    joinGame(host);
                case 1 -> //Info
                    gui_root.addModalForm(new GameInfoForm(host.getGame()));
                default -> throw new IllegalArgumentException("Unexpected action " + item_index);
            }
            SelectGameMenu.this.game_list_box.setFocus();
        }
    }
}
