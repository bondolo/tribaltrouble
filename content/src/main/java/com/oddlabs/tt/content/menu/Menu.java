package com.oddlabs.tt.content.menu;

import com.oddlabs.matchmaking.Game;
import com.oddlabs.net.NetworkSelector;
import com.oddlabs.tt.audio.AudioSettings;
import com.oddlabs.tt.base.animation.AnimationManager;
import com.oddlabs.tt.base.util.ProgressListener;
import com.oddlabs.tt.base.util.Utils;
import com.oddlabs.tt.client.camera.Camera;
import com.oddlabs.tt.client.camera.MenuCamera;
import com.oddlabs.tt.client.delegate.CameraDelegate;
import com.oddlabs.tt.client.delegate.FormFactory;
import com.oddlabs.tt.client.render.FullSceneRenderer;
import com.oddlabs.tt.client.render.Picker;
import com.oddlabs.tt.engine.settings.GraphicsSettings;
import com.oddlabs.tt.client.viewer.Selection;
import com.oddlabs.tt.content.form.OptionsMenu;
import com.oddlabs.tt.client.match.ProgressForm;
import com.oddlabs.tt.content.form.QuitForm;
import com.oddlabs.tt.client.Peer;
import com.oddlabs.tt.client.screen.ScreenManager;
import com.oddlabs.tt.client.landscape.LandscapeBaker;
import com.oddlabs.tt.client.screen.DefaultScreen;
import com.oddlabs.tt.client.screen.Screen;
import com.oddlabs.tt.scenery.LandscapeRenderer;
import com.oddlabs.tt.client.resource.LandscapeAssetsLoader;
import com.oddlabs.tt.engine.render.RenderConfig;
import com.oddlabs.tt.engine.render.RenderQueues;
import com.oddlabs.tt.engine.render.Texture;
import com.oddlabs.tt.engine.settings.AccessibilitySettings;
import com.oddlabs.tt.client.resource.AudioRegistry;
import com.oddlabs.tt.procedural.landscape.IslandGenerator;
import com.oddlabs.tt.engine.resource.WorldInfo;
import com.oddlabs.tt.gui.FocusDirection;
import com.oddlabs.tt.gui.Form;
import com.oddlabs.tt.gui.GUIImage;
import com.oddlabs.tt.gui.GUIObject;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.MenuButton;
import com.oddlabs.tt.gui.MessageForm;
import com.oddlabs.tt.gui.WarningForm;
import com.oddlabs.tt.input.GameAction;
import com.oddlabs.tt.input.InputEvent;
import com.oddlabs.tt.input.InputPhase;
import com.oddlabs.tt.simulation.landscape.IslandConfig;
import com.oddlabs.tt.simulation.landscape.Landscape;
import com.oddlabs.tt.simulation.landscape.NotificationListener;
import com.oddlabs.tt.simulation.landscape.World;
import com.oddlabs.tt.simulation.landscape.WorldGenerator;
import com.oddlabs.tt.simulation.landscape.WorldParameters;
import com.oddlabs.tt.simulation.model.Race;
import com.oddlabs.tt.simulation.model.Terrain;
import com.oddlabs.tt.simulation.player.Player;
import com.oddlabs.tt.simulation.player.PlayerInfo;
import com.oddlabs.util.Color;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
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

    private static final int MENU_ISLAND_SIZE = 256;
    private static final float MENU_ISLAND_HILLS = 1.0f;
    private static final float MENU_ISLAND_VEGETATION = 2.0f;
    private static final float MENU_ISLAND_RESOURCES = 0.0f;
    private static final int MENU_ISLAND_SEED = 1;

    private static final ResourceBundle bundle = ResourceBundle.getBundle("com.oddlabs.tt.content.menu.MainMenu");

    public static String i18n(String key, Object... args) {
        return Utils.getBundleString(bundle, key, args);
    }

    protected final Peer peer;
    private final ScreenManager screenManager;

    private @Nullable Form current_menu;
    private boolean current_menu_centered;

    private @Nullable GUIImage overlay;
    private @Nullable GUIImage logo;

    protected Menu(GUIRoot gui_root, Camera camera, Peer peer, ScreenManager screenManager) {
        super(gui_root, camera);
        this.peer = peer;
        this.screenManager = screenManager;
        setCanFocus(true);
        setFocusCycle(true);
    }

    public final Peer getPeer() {
        return peer;
    }

    public ScreenManager getScreenManager() {
        return screenManager;
    }

    protected final NetworkSelector getNetwork() {
        return peer.getNetwork().getSelector();
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
        addOptionsButton(() -> new OptionsMenu(getGUIRoot(), peer));
    }

    final void addOptionsButton(FormFactory<?> factory) {
        MenuButton options = new MenuButton(getGUIRoot(), i18n("options"), COLOR_NORMAL, COLOR_ACTIVE);
        options.addMouseClickListener((_, _, _, _) -> setMenuCentered(factory.create()));
        addChild(options);
    }

    protected final void addExitButton() {
        MenuButton exit = new MenuButton(getGUIRoot(), i18n("quit"), COLOR_NORMAL, COLOR_ACTIVE);
        exit.addMouseClickListener((_, _, _, _) -> setMenuCentered(new QuitForm(getGUIRoot(), peer::shutdown)));
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
        disableButtons(true);
        form.addCloseListener(() -> {
            disableButtons(false);
            current_menu = null;
            setFocus();
        });
        current_menu = form;
        current_menu_centered = centered;
        getGUIRoot().addChild(form);
        positionMenu();
        form.setFocus();
    }

    private void disableButtons(boolean disabled) {
        GUIObject child = getLastChild();
        while (child != null) {
            if (child instanceof MenuButton button) {
                button.setDisabled(disabled);
            }
            child = child.getPrior();
        }
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
    public void setFocus(FocusDirection direction) {
        if (current_menu != null) {
            current_menu.setFocus(direction);
        } else {
            GUIObject child = getLastChild();
            while (child != null) {
                if (child instanceof MenuButton button && !button.isDisabled()) {
                    button.setFocus(direction);
                    break;
                }
                child = child.getPrior();
            }
            super.setFocus(direction);
            focusNext();
        }
    }

    @Override
    public void mouseScrolled(int amount) {
    }

    protected final void addResumeButton() {
        MenuButton resume = new MenuButton(getGUIRoot(), i18n("resume"), COLOR_NORMAL, COLOR_ACTIVE);
        addChild(resume);
        resume.addMouseClickListener((_, _, _, _) -> pop());
    }

    public static void startMenu(Peer peer, ScreenManager screenManager) {
        setupMainMenu(peer, false, screenManager);
    }

    public static @Nullable Runnable setupMainMenu(
            Peer peer, final boolean first_progress, ScreenManager screenManager) {
        IslandConfig islandConfig = new IslandConfig(
                Terrain.NATIVE, MENU_ISLAND_SIZE, MENU_ISLAND_HILLS,
                MENU_ISLAND_VEGETATION, MENU_ISLAND_RESOURCES,
                MENU_ISLAND_SEED);
        final WorldGenerator<Landscape> generator = new IslandGenerator(islandConfig);
        ProgressForm.Mode mode = first_progress ? ProgressForm.Mode.STARTUP : ProgressForm.Mode.MENU_RETURN;
        return ProgressForm.setProgressForm(peer.getNetwork().getSelector(), screenManager, peer
                .getAudioManager(), (
                        GUIRoot gui_root) -> finishMainMenu(peer, gui_root,
                                first_progress, generator, screenManager), mode);
    }

    private static Screen finishMainMenu(
            Peer peer, GUIRoot gui_root,
            boolean first_progress, WorldGenerator<Landscape> generator, ScreenManager screenManager) {
        peer.getFramePacer().freezeTime();
        WorldParameters world_params = new WorldParameters(Game.GAMESPEED_NORMAL, "", 2, Player.DEFAULT_MAX_UNIT_COUNT);
        var players = List.of(new PlayerInfo(0, Race.NATIVES, ""));
        Landscape landscape = ProgressListener.subTask(0.40f,
                () -> generator.generate(players.size(), world_params.initialUnitCount(), 0f));
        WorldInfo<Texture> world_info = ProgressListener.subTask(0.30f,
                () -> LandscapeBaker.bakeWorld(landscape));
        RenderQueues render_queues = new RenderQueues();
        ProgressListener.subTask(0.15f,
                () -> new LandscapeAssetsLoader(render_queues));
        World world = ProgressListener.subTask(0.15f,
                () -> World.newWorld(null, null,
                        new NotificationListener() {
                        }, world_params, world_info.landscape(), players,
                        AccessibilitySettings.from(peer.getSettings()).linear_player_colours,
                        RenderConfig.INSERT_PLANTS[GraphicsSettings.from(peer.getSettings()).graphic_detail]));
        AnimationManager menuAnimationManager = new AnimationManager();
        LandscapeRenderer landscape_renderer = new LandscapeRenderer(world, world_info, menuAnimationManager);
        Player local_player = world.getPlayers().getFirst();
        Selection selection = new Selection(local_player);
        var renderer = new FullSceneRenderer(null, local_player,
                render_queues, world_info,
                landscape_renderer, new Picker(menuAnimationManager, local_player, gui_root, render_queues,
                        landscape_renderer, selection, peer.getAudioManager()), selection,
                peer.getAudioManager(), peer.getSettings(),
                gui_root.getWidth(), gui_root.getHeight());
        peer.getAudioManager().setMusic(AudioRegistry.MUSIC_MENU, 0f);
        MainMenu main_menu = new MainMenu(gui_root,
                new MenuCamera(world, gui_root.getAnimationManagerHighPrecision(), menuAnimationManager), peer,
                screenManager);
        gui_root.pushDelegate(main_menu);
        AudioSettings audioSettings = AudioSettings.from(peer.getSettings());
        if (first_progress && audioSettings.warning_no_sound
                && !peer.getEventQueue().getDeterministic().log(peer.getAudioManager() != null)) {
            gui_root.addModalForm(new WarningForm(gui_root, i18n("sound_not_available_caption"), i18n(
                    "sound_not_available_message"),
                    doNotShowAgain -> audioSettings.warning_no_sound = !doNotShowAgain));
        }
        if (!initNetwork(peer)) {
            gui_root.addModalForm(new MessageForm(gui_root, i18n("network_not_available_caption"),
                    i18n("network_not_available_message"),
                    i18n("quit"), (_, _, _, _) -> peer.shutdown()));
        }
        return new DefaultScreen(gui_root, renderer);
    }

    public static boolean initNetwork(Peer peer) {
        boolean is_network_created;
        try {
            peer.getNetwork().getSelector().initSelector();
            tryGetLoopbackAddress();
            is_network_created = true;
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to initialize network", e);
            is_network_created = false;
        }
        return peer.getEventQueue().getDeterministic().log(is_network_created);
    }
}
