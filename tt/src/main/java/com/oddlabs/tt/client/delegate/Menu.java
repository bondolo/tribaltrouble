package com.oddlabs.tt.client.delegate;

import com.oddlabs.matchmaking.Game;
import com.oddlabs.net.NetworkSelector;
import com.oddlabs.tt.client.camera.Camera;
import com.oddlabs.tt.client.form.ConnectingForm;
import com.oddlabs.tt.client.form.MultiplayerLobby;
import com.oddlabs.tt.client.form.OptionsMenu;
import com.oddlabs.tt.client.form.QuitForm;
import com.oddlabs.tt.client.gui.FocusDirection;
import com.oddlabs.tt.client.gui.Form;
import com.oddlabs.tt.client.gui.GUI;
import com.oddlabs.tt.client.gui.GUIImage;
import com.oddlabs.tt.client.gui.GUIObject;
import com.oddlabs.tt.client.gui.GUIRoot;
import com.oddlabs.tt.client.gui.MenuButton;
import com.oddlabs.tt.client.input.GameAction;
import com.oddlabs.tt.client.input.InputEvent;
import com.oddlabs.tt.client.input.InputPhase;
import com.oddlabs.tt.simulation.landscape.WorldParameters;
import com.oddlabs.tt.simulation.model.Terrain;
import com.oddlabs.tt.core.net.Client;
import com.oddlabs.tt.core.net.GameNetwork;
import com.oddlabs.tt.core.net.Server;
import com.oddlabs.tt.core.net.WorldInitAction;
import com.oddlabs.tt.simulation.player.Player;
import com.oddlabs.tt.engine.render.Renderer;
import com.oddlabs.tt.engine.resource.IslandGenerator;
import com.oddlabs.tt.engine.resource.WorldGenerator;
import com.oddlabs.tt.client.trigger.GameOverTrigger;
import com.oddlabs.tt.core.util.Utils;
import com.oddlabs.tt.client.viewer.InGameInfo;
import com.oddlabs.tt.client.viewer.WorldViewer;
import com.oddlabs.util.Color;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.net.InetAddress;
import java.util.ResourceBundle;

/**
 * Base class for full-screen menus in the game, such as the main menu or the in-game escape menu.
 * It provides common UI elements like the background logo and layout helpers for menu buttons.
 */
public abstract class Menu extends CameraDelegate<Camera> {
    public static final Color COLOR_NORMAL = Color.Standard.WHITE;
    public static final Color COLOR_ACTIVE = new Color.Standard(0xFF_FF_CC_9F);
    private static final int MENU_X = 160;
    private static final int OVERLAY_TEXTURE_WIDTH = 1024;
    private static final int OVERLAY_TEXTURE_HEIGHT = 1024;
    private static final int OVERLAY_IMAGE_WIDTH = 800;
    private static final int OVERLAY_IMAGE_HEIGHT = 600;
    private static final String OVERLAY_TEXTURE_NAME = "/textures/gui/mainmenu";

    private static final ResourceBundle bundle = ResourceBundle.getBundle("com.oddlabs.tt.content.menu.MainMenu");

    public static @NonNull String i18n(@NonNull String key, @NonNull Object @NonNull... args) {
        return Utils.getBundleString(bundle, key, args);
    }

    private final @NonNull NetworkSelector network;

    private @Nullable Form current_menu;
    private boolean current_menu_centered;

    private @Nullable GUIImage overlay;
    private @Nullable GUIImage logo;

    protected Menu(@NonNull NetworkSelector network, @NonNull GUIRoot gui_root, @NonNull Camera camera) {
        super(gui_root, camera);
        this.network = network;
        setCanFocus(true);
        setFocusCycle(true);
    }

    protected final @NonNull NetworkSelector getNetwork() {
        return network;
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

    final void addOptionsButton(@NonNull FormFactory<?> factory) {
        MenuButton options = new MenuButton(i18n("options"), COLOR_NORMAL, COLOR_ACTIVE);
        options.addMouseClickListener((_, _, _, _) -> setMenuCentered(factory.create()));
        addChild(options);
    }

    protected final void addExitButton() {
        MenuButton exit = new MenuButton(i18n("quit"), COLOR_NORMAL, COLOR_ACTIVE);
        exit.addMouseClickListener((_, _, _, _) -> setMenuCentered(new QuitForm(getGUIRoot())));
        addChild(exit);
    }

    protected abstract void addButtons();

    protected final void reload() {
        init();
        addButtons();

        displayChangedNotify(getGUIRoot().getWidth(), getGUIRoot().getHeight());
    }

    @Override
    public void handleInput(@NonNull InputEvent event) {
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

    @Override
    public void displayChangedNotify(int width, int height) {
        setDim(width, height);

        int y = height - (int) (190f * height / OVERLAY_IMAGE_HEIGHT);
        int x = 15;

        overlay.setDim(width, height);

        // Maintain aspect ratio based on height
        float heightScale = height / 600f;
        int logoHeight = (int) (206f * heightScale);
        int logoWidth = (int) (347f * heightScale);

        logo.setDim(logoWidth, logoHeight);
        logo.setPos(0, height - logoHeight);
        GUIObject child = getLastChild();
        while (child != null) {
            if (child instanceof MenuButton) {
                child.setPos(x, y - child.getHeight());
                y -= (int) (child.getHeight() * .875);
            }
            child = child.getPrior();
        }
        if (current_menu != null) {
            if (current_menu_centered) {
                current_menu.centerPos();
            } else {
                positionMenu();
            }
        }
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

    public final void setMenuCentered(@NonNull Form menu) {
        setMenu(menu);
        menu.centerPos();
        current_menu_centered = true;
    }

    public final void setMenu(@NonNull Form menu) {
        if (current_menu != null)
            current_menu.remove();
        disableButtons(true);
        menu.addCloseListener(() -> {
            disableButtons(false);
            current_menu = null;
        });
        current_menu = menu;
        addChild(current_menu);
        current_menu.setFocus();
        positionMenu();
        current_menu_centered = false;
    }

    private void positionMenu() {
        current_menu.setPos(MENU_X, (getGUIRoot().getHeight() - current_menu.getHeight()) * 2 / 3);
    }

    protected final void addResumeButton() {
        MenuButton resume = new MenuButton(i18n("resume"), COLOR_NORMAL, COLOR_ACTIVE);
        addChild(resume);
        resume.addMouseClickListener((_, _, _, _) -> pop());
    }

    public static void completeGameSetupHack(@NonNull WorldViewer world_viewer) {
        world_viewer.getGUIRoot().pushDelegate(world_viewer.getDelegate());
        Renderer.getRenderer().setMusic(world_viewer.getLocalPlayer().getRaceInfo().getMusic(), 10f);
    }

    public static final class DefaultWorldInitAction implements WorldInitAction {
        @Override
        public void run(@NonNull WorldViewer viewer) {
            new GameOverTrigger(viewer);
            completeGameSetupHack(viewer);
        }
    }

    public final @NonNull GameNetwork joinGame(@NonNull NetworkSelector network, GUI gui, int host_id,
            int gamespeed, @NonNull String map_code, MultiplayerLobby owner, @NonNull InGameInfo ingame_info,
            int max_unit_count) {
        GUIRoot gui_root = getGUIRoot();
        Client client = new Client(null, network, gui, host_id, new WorldParameters(gamespeed, map_code,
                Player.INITIAL_UNIT_COUNT,
                max_unit_count),
                ingame_info,
                new DefaultWorldInitAction());
        GameNetwork game_network = new GameNetwork(null, client);
        ConnectingForm connecting_form = new ConnectingForm(game_network, getGUIRoot(), owner, true);
        client.setConfigurationListener(connecting_form);
        gui_root.addModalForm(connecting_form);
        return game_network;
    }

    public static @NonNull GameNetwork startNewGame(@NonNull NetworkSelector network, @NonNull GUIRoot gui_root,
            MultiplayerLobby owner, WorldParameters world_params, @NonNull InGameInfo ingame_info,
            WorldInitAction init_action, Game game, int meters_per_world, @NonNull Terrain terrain,
            float hills, float vegetation_amount, float supplies_amount, int seed, String[] ai_names) {
        boolean multiplayer = ingame_info.isMultiplayer();
        WorldGenerator generator = new IslandGenerator(terrain, meters_per_world, hills, vegetation_amount,
                supplies_amount, seed);
        InetAddress address = multiplayer ? null : com.oddlabs.util.Utils.getLoopbackAddress();
        final Server server = new Server(network, game, address, generator, multiplayer, ai_names);
        Client client = new Client(server::close, network, gui_root.getGUI(), -1, world_params, ingame_info,
                init_action);
        GameNetwork game_network = new GameNetwork(server, client);
        ConnectingForm connecting_form = new ConnectingForm(game_network, gui_root, owner, multiplayer);
        client.setConfigurationListener(connecting_form);
        gui_root.addModalForm(connecting_form);
        return game_network;
    }
}
