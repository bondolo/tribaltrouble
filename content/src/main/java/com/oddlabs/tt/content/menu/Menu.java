package com.oddlabs.tt.content.menu;

import com.oddlabs.matchmaking.Game;
import com.oddlabs.net.NetworkSelector;
import com.oddlabs.tt.audio.AudioManager;
import com.oddlabs.tt.base.animation.AnimationManager;
import com.oddlabs.tt.base.util.Utils;
import com.oddlabs.tt.client.camera.Camera;
import com.oddlabs.tt.client.camera.MenuCamera;
import com.oddlabs.tt.client.delegate.CameraDelegate;
import com.oddlabs.tt.client.delegate.FormFactory;
import com.oddlabs.tt.client.render.DefaultRenderer;
import com.oddlabs.tt.client.render.Picker;
import com.oddlabs.tt.client.trigger.GameOverTrigger;
import com.oddlabs.tt.client.viewer.InGameInfo;
import com.oddlabs.tt.client.viewer.Selection;
import com.oddlabs.tt.client.viewer.WorldInitAction;
import com.oddlabs.tt.client.viewer.WorldStarter;
import com.oddlabs.tt.client.viewer.WorldViewer;
import com.oddlabs.tt.content.form.ConnectingForm;
import com.oddlabs.tt.content.form.MultiplayerLobby;
import com.oddlabs.tt.content.form.OptionsMenu;
import com.oddlabs.tt.content.form.ProgressForm;
import com.oddlabs.tt.content.form.QuitForm;
import com.oddlabs.tt.engine.ClientEngine;
import com.oddlabs.tt.engine.render.LandscapeRenderer;
import com.oddlabs.tt.engine.render.LandscapeAssetsLoader;
import com.oddlabs.tt.engine.render.MatrixStack;
import com.oddlabs.tt.engine.render.RenderConfig;
import com.oddlabs.tt.engine.render.RenderQueues;
import com.oddlabs.tt.engine.render.Texture;
import com.oddlabs.tt.engine.resource.AssetRegistry;
import com.oddlabs.tt.engine.resource.AudioAssets;
import com.oddlabs.tt.engine.resource.IslandGenerator;
import com.oddlabs.tt.engine.resource.WorldInfo;
import com.oddlabs.tt.gui.FocusDirection;
import com.oddlabs.tt.gui.Form;
import com.oddlabs.tt.gui.GUI;
import com.oddlabs.tt.gui.GUIImage;
import com.oddlabs.tt.gui.GUIObject;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.MenuButton;
import com.oddlabs.tt.gui.MessageForm;
import com.oddlabs.tt.gui.WarningForm;
import com.oddlabs.tt.gui.render.UIRenderer;
import com.oddlabs.tt.input.GameAction;
import com.oddlabs.tt.input.InputEvent;
import com.oddlabs.tt.input.InputPhase;
import com.oddlabs.tt.net.Client;
import com.oddlabs.tt.net.GameNetwork;
import com.oddlabs.tt.net.LoadCallbackFactory;
import com.oddlabs.tt.net.Server;
import com.oddlabs.tt.procedural.LandscapeConfig;
import com.oddlabs.tt.simulation.landscape.IslandConfig;
import com.oddlabs.tt.simulation.landscape.NotificationListener;
import com.oddlabs.tt.simulation.landscape.World;
import com.oddlabs.tt.simulation.landscape.WorldGenerator;
import com.oddlabs.tt.simulation.landscape.WorldParameters;
import com.oddlabs.tt.simulation.model.Race;
import com.oddlabs.tt.simulation.model.Terrain;
import com.oddlabs.tt.simulation.player.DefaultPlayerSlotHandler;
import com.oddlabs.tt.simulation.player.Player;
import com.oddlabs.tt.simulation.player.PlayerInfo;
import com.oddlabs.util.Color;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.oddlabs.util.Utils.tryGetLoopbackAddress;

/**
 * Base class for full-screen menus in the game, such as the main menu or the in-game escape menu.
 * It provides common UI elements like the background logo and layout helpers for menu buttons.
 */
public abstract class Menu extends CameraDelegate<Camera> {
    private static final Logger logger = Logger.getLogger(Menu.class.getName());

    public static final Color COLOR_NORMAL = Color.Standard.WHITE;
    public static final Color COLOR_ACTIVE = new Color.Standard(0xFF_FF_CC_9F);
    private static final int MENU_X = 160;
    private static final int OVERLAY_TEXTURE_WIDTH = 1024;
    private static final int OVERLAY_TEXTURE_HEIGHT = 1024;
    private static final int OVERLAY_IMAGE_WIDTH = 800;
    private static final int OVERLAY_IMAGE_HEIGHT = 600;
    private static final String OVERLAY_TEXTURE_NAME = "/textures/gui/mainmenu";

    private static final ResourceBundle bundle = ResourceBundle.getBundle("com.oddlabs.tt.content.menu.MainMenu");

    public static String i18n(String key, Object... args) {
        return Utils.getBundleString(bundle, key, args);
    }

    protected final ClientEngine engine;

    private @Nullable Form current_menu;
    private boolean current_menu_centered;

    private @Nullable GUIImage overlay;
    private @Nullable GUIImage logo;

    protected Menu(GUIRoot gui_root, Camera camera) {
        super(gui_root, camera);
        this.engine = gui_root.getGUI().getEngine();
        setCanFocus(true);
        setFocusCycle(true);
    }

    public final ClientEngine getEngine() {
        return engine;
    }

    protected final NetworkSelector getNetwork() {
        return engine.getNetwork().getSelector();
    }

    public final AudioManager getAudioManager() {
        return engine.getAudioManager();
    }

    private void init() {
        clearChildren();
        int screen_width = getGUIRoot().getWidth();
        int screen_height = getGUIRoot().getHeight();
        overlay = new GUIImage(screen_width, screen_height, 0f, 0f, (float) OVERLAY_IMAGE_WIDTH / OVERLAY_TEXTURE_WIDTH,
                (float) OVERLAY_IMAGE_HEIGHT / OVERLAY_TEXTURE_HEIGHT, OVERLAY_TEXTURE_NAME);
        overlay.setPos(0, 0);
        addChild(overlay);

        String logo_file = i18n("logo_file");

        float heightScale = screen_height / 600f;
        int logoHeight = (int) (206f * heightScale);
        int logoWidth = (int) (347f * heightScale);

        logo = new GUIImage(logoWidth, logoHeight, 0f, 0f, 347f / 512f, 206f / 256f, logo_file);
        logo.setPos(0, screen_height - logoHeight);
        addChild(logo);
    }

    protected final void addDefaultOptionsButton() {
        addOptionsButton(() -> new OptionsMenu(getGUIRoot()));
    }

    final void addOptionsButton(FormFactory<?> factory) {
        MenuButton options = new MenuButton(i18n("options"), COLOR_NORMAL, COLOR_ACTIVE);
        options.addMouseClickListener((_, _, _, _) -> setMenuCentered(factory.create()));
        addChild(options);
    }

    protected final void addExitButton() {
        MenuButton exit = new MenuButton(i18n("quit"), COLOR_NORMAL, COLOR_ACTIVE);
        exit.addMouseClickListener((_, _, _, _) -> setMenuCentered(new QuitForm(engine::shutdown)));
        addChild(exit);
    }

    protected abstract void addButtons();

    protected final void reload() {
        init();
        addButtons();

        displayChangedNotify(getGUIRoot().getWidth(), getGUIRoot().getHeight());
    }

    @Override
    public void handleInput(InputEvent event) {
        if ((event.getPhase() == InputPhase.PRESSED || event.getPhase() == InputPhase.REPEAT) && event.hasActions()) {
            if (event.consumeAction(GameAction.UI_CANCEL)) {
                event.consume(); // Menu usually swallows escape
                return;
            }
            if (event.consumeAction(GameAction.UI_FOCUS_NEXT)) {
                switchFocus(FocusDirection.FORWARD);
                event.consume();
                return;
            }
            if (event.consumeAction(GameAction.UI_FOCUS_PREV)) {
                switchFocus(FocusDirection.BACKWARD);
                event.consume();
                return;
            }
            if (event.consumeAction(GameAction.UI_NAV_UP)) {
                focusPrior();
                event.consume();
                return;
            }
            if (event.consumeAction(GameAction.UI_NAV_DOWN)) {
                focusNext();
                event.consume();
                return;
            }
        }

        super.handleInput(event);
    }

    public final void setMenu(Form form) {
        setMenu(form, false);
    }

    public final void setMenuCentered(Form form) {
        setMenu(form, true);
    }

    private void setMenu(Form form, boolean centered) {
        if (current_menu != null) {
            current_menu.remove();
        }
        current_menu = form;
        current_menu_centered = centered;
        getGUIRoot().addChild(form);
        positionMenu();
        form.setFocus();
    }

    private void positionMenu() {
        if (current_menu != null) {
            if (current_menu_centered) {
                current_menu.centerPos();
            } else {
                current_menu.setPos(MENU_X, (getGUIRoot().getHeight() - current_menu.getHeight()) / 2);
            }
        }
    }

    @Override
    public void displayChangedNotify(int width, int height) {
        super.displayChangedNotify(width, height);
        setDim(width, height);

        int y = height - (int) (190f * height / OVERLAY_IMAGE_HEIGHT);
        int x = 15;

        if (overlay != null) {
            overlay.setDim(width, height);
        }
        if (logo != null) {
            float heightScale = height / 600f;
            int logoHeight = (int) (206f * heightScale);
            int logoWidth = (int) (347f * heightScale);
            logo.setDim(logoWidth, logoHeight);
            logo.setPos(0, height - logoHeight);
        }
        GUIObject child = getLastChild();
        while (child != null) {
            if (child instanceof MenuButton) {
                child.setPos(x, y - child.getHeight());
                y -= (int) (child.getHeight() * .875);
            }
            child = child.getPrior();
        }
        positionMenu();
    }

    public final void removeMenu() {
        if (current_menu != null) {
            current_menu.remove();
            current_menu = null;
        }
    }

    @Override
    public final void setFocus() {
        if (current_menu != null) {
            current_menu.setFocus();
        } else {
            GUIObject child = getLastChild();
            while (child != null) {
                if (child instanceof MenuButton button) {
                    button.setFocus();
                    break;
                }
                child = child.getPrior();
            }
            super.setFocus();
            focusNext();
        }
    }

    @Override
    public void mouseScrolled(int amount) {
    }

    protected final void addResumeButton() {
        MenuButton resume = new MenuButton(i18n("resume"), COLOR_NORMAL, COLOR_ACTIVE);
        addChild(resume);
        resume.addMouseClickListener((_, _, _, _) -> pop());
    }

    public static void completeGameSetupHack(WorldViewer world_viewer) {
        world_viewer.getGUIRoot().pushDelegate(world_viewer.getDelegate());
        world_viewer.getAudioManager().setMusic(AssetRegistry.getInstance().getMusic(world_viewer.getLocalPlayer()
                .getPlayerInfo().getRace()), 10f);
    }

    public static final class DefaultWorldInitAction implements WorldInitAction {
        @Override
        public void run(WorldViewer viewer) {
            new GameOverTrigger(viewer);
            completeGameSetupHack(viewer);
        }
    }

    public final GameNetwork<GUIRoot, UIRenderer> joinGame(GUI gui, int host_id,
            int gamespeed, String map_code, MultiplayerLobby owner, InGameInfo ingame_info,
            int max_unit_count) {
        GUIRoot gui_root = getGUIRoot();
        WorldParameters world_params = new WorldParameters(gamespeed, map_code, Player.INITIAL_UNIT_COUNT,
                max_unit_count);
        var matchmakingClient = engine.getNetwork().getMatchmakingClient();
        var chatHub = engine.getNetwork().getChatHub();
        var networkSelector = engine.getNetwork().getSelector();
        @SuppressWarnings("unchecked") LoadCallbackFactory<GUIRoot, UIRenderer> starterFactory = (session_id, generator,
                player_slots,
                unit_infos, player_slot) -> new WorldStarter(session_id,
                        (WorldGenerator<WorldInfo<Texture>>) generator, world_params, player_slots, unit_infos,
                        player_slot, ingame_info,
                        new DefaultWorldInitAction());
        Client<GUIRoot, UIRenderer> client = new Client<>(null, networkSelector, matchmakingClient, chatHub, host_id,
                starterFactory,
                new DefaultPlayerSlotHandler());
        GameNetwork<GUIRoot, UIRenderer> game_network = new GameNetwork<>(null, client);
        ConnectingForm connecting_form = new ConnectingForm(game_network, getGUIRoot(), owner, true);
        client.setConfigurationListener(connecting_form);
        gui_root.addModalForm(connecting_form);
        return game_network;
    }

    public static GameNetwork<GUIRoot, UIRenderer> startNewGame(GUIRoot gui_root,
            MultiplayerLobby owner, WorldParameters world_params, InGameInfo ingame_info,
            WorldInitAction init_action, Game game, IslandConfig islandConfig, String[] ai_names) {
        var engine = gui_root.getGUI().getEngine();
        boolean multiplayer = ingame_info.isMultiplayer();
        WorldGenerator<WorldInfo<Texture>> generator = new IslandGenerator(islandConfig,
                engine.getSettings().getTexelsPerGridUnit());
        InetAddress address = multiplayer ? null : com.oddlabs.util.Utils.getLoopbackAddress();
        var matchmakingClient = engine.getNetwork().getMatchmakingClient();
        var chatHub = engine.getNetwork().getChatHub();
        var networkSelector = engine.getNetwork().getSelector();
        final Server server = new Server(networkSelector, matchmakingClient, game,
                address, generator, multiplayer, ai_names,
                (team, race_val, name) -> new PlayerInfo(team, Race.fromValue(race_val), name),
                new DefaultPlayerSlotHandler());
        @SuppressWarnings("unchecked") LoadCallbackFactory<GUIRoot, UIRenderer> starterFactory = (session_id, gen,
                player_slots, unit_infos,
                player_slot) -> new WorldStarter(
                        session_id, (WorldGenerator<WorldInfo<Texture>>) gen,
                        world_params, player_slots, unit_infos, player_slot, ingame_info, init_action);
        Client<GUIRoot, UIRenderer> client = new Client<>(server::close, networkSelector, matchmakingClient,
                chatHub, -1, starterFactory, new DefaultPlayerSlotHandler());
        GameNetwork<GUIRoot, UIRenderer> game_network = new GameNetwork<>(server, client);
        ConnectingForm connecting_form = new ConnectingForm(game_network, gui_root, owner, multiplayer);
        client.setConfigurationListener(connecting_form);
        gui_root.addModalForm(connecting_form);
        return game_network;
    }

    public static void startMenu(GUI gui) {
        setupMainMenu(gui, false);
    }

    public static @Nullable Runnable setupMainMenu(GUI gui,
            final boolean first_progress) {
        var engine = gui.getEngine();
        IslandConfig islandConfig = new IslandConfig(
                Terrain.NATIVE, 256, LandscapeConfig.LANDSCAPE_HILLS,
                LandscapeConfig.LANDSCAPE_VEGETATION, LandscapeConfig.LANDSCAPE_RESOURCES,
                LandscapeConfig.LANDSCAPE_SEED);
        final WorldGenerator<WorldInfo<Texture>> generator = new IslandGenerator(
                islandConfig, engine.getSettings().getTexelsPerGridUnit());
        return ProgressForm.setProgressForm(engine.getNetwork().getSelector(), gui, (
                GUIRoot gui_root) -> finishMainMenu(gui_root,
                        first_progress, generator), first_progress);
    }

    private static UIRenderer finishMainMenu(GUIRoot gui_root,
            boolean first_progress, WorldGenerator<WorldInfo<Texture>> generator) {
        var engine = gui_root.getGUI().getEngine();
        engine.getFramePacer().freezeTime();
        MatrixStack modelViewStack = new MatrixStack();
        MatrixStack projectionStack = new MatrixStack();
        WorldParameters world_params = new WorldParameters(Game.GAMESPEED_NORMAL, "", 2, Player.DEFAULT_MAX_UNIT_COUNT);
        var players = List.of(new PlayerInfo(0, Race.NATIVES, ""));
        WorldInfo<Texture> world_info = generator.generate(players.size(), world_params.initialUnitCount(), 0f);
        RenderQueues render_queues = new RenderQueues();
        LandscapeAssetsLoader landscape_resources = new LandscapeAssetsLoader(render_queues);
        ProgressForm.progress();
        World world = World.newWorld(landscape_resources, null,
                new NotificationListener() {
                }, world_params, world_info.landscapeData(), players,
                engine.getSettings().accessibility.linear_team_colours,
                RenderConfig.INSERT_PLANTS[engine.getSettings().graphic_detail],
                ProgressForm::progress);
        AnimationManager menuAnimationManager = new AnimationManager();
        LandscapeRenderer landscape_renderer = new LandscapeRenderer(world, world_info, menuAnimationManager);
        Player local_player = world.getPlayers().getFirst();
        Selection selection = new Selection(local_player);
        UIRenderer renderer = new DefaultRenderer(null, local_player, render_queues, world_info,
                landscape_renderer, new Picker(menuAnimationManager, local_player, gui_root, render_queues,
                        landscape_renderer, selection, engine.getAudioManager()), selection, modelViewStack,
                projectionStack,
                engine.getAudioManager(), engine.getSettings().graphic_detail,
                gui_root.getWidth(), gui_root.getHeight());
        engine.getAudioManager().setMusic(AudioAssets.MUSIC_MENU, 0f);
        MainMenu main_menu = new MainMenu(gui_root,
                new MenuCamera(world, gui_root.getAnimationManagerHighPrecision(), menuAnimationManager));
        gui_root.pushDelegate(main_menu);
        if (first_progress && engine.getSettings().audio.warning_no_sound
                && !engine.getEventQueue().getDeterministic().log(engine.getAudioManager() != null)) {
            gui_root.addModalForm(new WarningForm(i18n("sound_not_available_caption"), i18n(
                    "sound_not_available_message"),
                    doNotShowAgain -> engine.getSettings().audio.warning_no_sound = !doNotShowAgain));
        }
        if (!initNetwork(engine)) {
            gui_root.addModalForm(new MessageForm(i18n("network_not_available_caption"),
                    i18n("network_not_available_message"),
                    i18n("quit"), (_, _, _, _) -> engine.shutdown()));
        }
        return renderer;
    }

    public static boolean initNetwork(ClientEngine engine) {
        boolean is_network_created;
        try {
            engine.getNetwork().getSelector().initSelector();
            tryGetLoopbackAddress();
            is_network_created = true;
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to initialize network", e);
            is_network_created = false;
        }
        return engine.getEventQueue().getDeterministic().log(is_network_created);
    }
}
