package com.oddlabs.tt.client.viewer;

import com.oddlabs.router.SessionID;

import com.oddlabs.tt.audio.AudioManager;
import com.oddlabs.tt.base.animation.Animated;
import com.oddlabs.tt.base.animation.AnimationManager;
import com.oddlabs.tt.base.event.LocalEventQueue;
import com.oddlabs.tt.base.util.ProgressListener;
import com.oddlabs.tt.base.util.Utils;
import com.oddlabs.tt.client.camera.GameCamera;
import com.oddlabs.tt.client.delegate.GameStatsDelegate;
import com.oddlabs.tt.client.delegate.SelectionDelegate;
import com.oddlabs.tt.client.gui.ActionButtonPanel;
import com.oddlabs.tt.client.gui.GUIIcons;
import com.oddlabs.tt.client.render.FullSceneRenderer;
import com.oddlabs.tt.client.render.Picker;
import com.oddlabs.tt.client.resource.RacesAssetsLoader;
import com.oddlabs.tt.client.Peer;
import com.oddlabs.tt.client.screen.ScreenManager;
import com.oddlabs.tt.client.resource.AssetRegistry;
import com.oddlabs.tt.client.resource.EmojiType;
import com.oddlabs.tt.engine.settings.AccessibilitySettings;
import com.oddlabs.tt.engine.settings.GraphicsSettings;
import com.oddlabs.tt.engine.render.CameraState;
import com.oddlabs.tt.client.landscape.LandscapeBaker;
import com.oddlabs.tt.scenery.LandscapeRenderer;
import com.oddlabs.tt.client.resource.LandscapeAssetsLoader;
import com.oddlabs.tt.engine.render.IconQuad;
import com.oddlabs.tt.engine.render.RenderConfig;
import com.oddlabs.tt.engine.render.RenderQueues;
import com.oddlabs.tt.engine.render.SpriteKey;
import com.oddlabs.tt.engine.render.Texture;
import com.oddlabs.tt.simulation.landscape.Landscape;
import com.oddlabs.tt.engine.resource.WorldInfo;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.Group;
import com.oddlabs.util.Color;
import com.oddlabs.tt.input.InputManager;
import com.oddlabs.tt.net.ChatListener;
import com.oddlabs.tt.net.ChatMethod;
import com.oddlabs.tt.net.ChatSender;
import com.oddlabs.tt.net.InGameChatHistory;
import com.oddlabs.tt.net.PeerHub;
import com.oddlabs.tt.simulation.landscape.LandscapeGeometry;
import com.oddlabs.tt.simulation.landscape.NotificationListener;
import com.oddlabs.tt.simulation.landscape.SpawnLocation;
import com.oddlabs.tt.simulation.landscape.World;
import com.oddlabs.tt.simulation.model.DistributableTable;
import com.oddlabs.tt.simulation.landscape.WorldGenerator;
import com.oddlabs.tt.simulation.landscape.WorldParameters;
import com.oddlabs.tt.simulation.model.Difficulty;
import com.oddlabs.tt.simulation.model.Model;
import com.oddlabs.tt.simulation.model.RaceData;
import com.oddlabs.tt.simulation.player.AI;
import com.oddlabs.tt.simulation.player.AdvancedAI;
import com.oddlabs.tt.simulation.player.NativeChieftainAI;
import com.oddlabs.tt.simulation.player.PassiveAI;
import com.oddlabs.tt.simulation.model.Unit;
import com.oddlabs.tt.simulation.model.UnitType;
import com.oddlabs.tt.simulation.player.Player;
import com.oddlabs.tt.simulation.player.PlayerInfo;
import com.oddlabs.tt.simulation.player.PlayerSlot;
import com.oddlabs.tt.simulation.player.UnitInfo;
import com.oddlabs.tt.simulation.player.VikingChieftainAI;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Orchestrates the primary in-game experience, managing the world state, player interactions,
 * rendering, and the user interface for a single player.
 * Coordinates camera state and audio listener updates for the game world.
 */
public final class WorldViewer implements Animated, AutoCloseable {

    private static final Color.Linear PRIVATE_MESSAGE_COLOR = new Color.Standard(0xFF_33_66_FF).linear();
    private static final Color.Linear TEAM_MESSAGE_COLOR = new Color.Standard(0xFF_4C_7F_FF).linear();

    private final Peer peer;
    private final ScreenManager screenManager;
    private final GameCamera camera;
    private final GUIIcons icons;
    private final ActionButtonPanel panel;
    private final SelectionDelegate delegate;
    private final PeerHub peerhub;
    private final GUIRoot gui_root;
    private final NotificationManager notification_manager;
    private final InGameInfo ingame_info;
    private final Selection selection;
    private final World world;
    private final Picker picker;
    private final FullSceneRenderer renderer;
    private final LandscapeRenderer landscape_renderer;
    private final Player local_player;
    private final WorldParameters world_params;
    private final AnimationManager animation_manager_local;
    private final Cheat cheat;
    private final ChatListener chat_listener;
    private final ChatSender chat_sender;
    private final InGameChatHistory in_game_chat_history;

    public WorldViewer(final GUIRoot gui_root,
            Peer peer,
            WorldParameters world_params, InGameInfo ingame_info, WorldGenerator<Landscape> generator,
            PlayerSlot[] player_slots, UnitInfo[] unit_infos, short player_slot,
            SessionID session_id, ScreenManager screenManager) {
        this.peer = peer;
        this.screenManager = screenManager;
        this.world_params = world_params;
        this.ingame_info = ingame_info;
        var network = peer.getNetwork().getSelector();
        this.cheat = new Cheat(!ingame_info.isMultiplayer());
        var audioManager = peer.getAudioManager();
        this.animation_manager_local = new AnimationManager();
        this.icons = new GUIIcons();
        final CameraState camera_state = new CameraState();
        this.notification_manager = new NotificationManager(gui_root, audioManager, icons.getNotifyArrow(),
                icons.getNotifyArrowHeadX(), icons.getNotifyArrowHeadY());
        RenderQueues render_queues = new RenderQueues();
        LandscapeGeometry landscape_geometry = new LandscapeGeometry();
        ProgressListener.subTask(0.15f,
                () -> new LandscapeAssetsLoader(render_queues));
        RaceData races_resources = ProgressListener.subTask(0.35f,
                () -> RacesAssetsLoader.load(render_queues));
        registerHarvestEmojis(render_queues, icons);
        var player_infos = Arrays.stream(player_slots).map(slot -> (PlayerInfo) slot.getInfo()).toList();
        Landscape landscape = ProgressListener.subTask(0.25f,
                () -> generator.generate(
                        player_infos.size(), world_params.initialUnitCount(), ingame_info.getRandomStartPosition()));
        WorldInfo<Texture> world_info = ProgressListener.subTask(0.15f,
                () -> LandscapeBaker.bakeWorld(landscape));
        camera_state.setFog(world_info.fog_info());
        this.world = ProgressListener.subTask(0.10f,
                () -> World.newWorld(races_resources, landscape_geometry, NotificationListener.EMPTY, world_params,
                        world_info.landscape(), player_infos, AccessibilitySettings.from(peer
                                .getSettings()).linear_player_colours,
                        RenderConfig.INSERT_PLANTS[GraphicsSettings.from(peer.getSettings()).graphic_detail]));
        this.local_player = world.getPlayers().get(player_slot);
        this.selection = new Selection(local_player);
        peer.setPathfindCountSupplier(world.getUnitGrid()::getAndResetPathfindCount);
        landscape_renderer = new LandscapeRenderer(world, world_info, animation_manager_local);
        this.picker = new Picker(animation_manager_local, local_player, gui_root, render_queues, landscape_renderer,
                selection, audioManager, icons);
        this.renderer = new FullSceneRenderer(cheat, local_player, render_queues,
                world_info, landscape_renderer, picker,
                selection, audioManager, peer.getSettings(),
                gui_root.getWidth(), gui_root.getHeight());
        WorldNotificationHandler notificationHandler = new WorldNotificationHandler(
                this, peer, gui_root, notification_manager, animation_manager_local,
                audioManager, ingame_info.isMultiplayer(), renderer, landscape_renderer);
        world.setNotificationListener(notificationHandler);
        world.getDistributableTable().getDistributables().stream()
                .filter(Model.class::isInstance)
                .map(Model.class::cast)
                .forEach(landscape_renderer::onStaticModelAdded);
        this.gui_root = gui_root;
        this.gui_root.setCheatIndicator(icons.getCheatIcon(), cheat::isEnabled);
        this.chat_listener = message -> {
            var infoPrinter = gui_root.getInfoPrinter();
            switch (message.type()) {
                case NORMAL -> infoPrinter.print(message.formatShort());
                case TEAM -> infoPrinter.print(message.formatShort(), TEAM_MESSAGE_COLOR);
                case PRIVATE -> infoPrinter.print(message.formatShort(), PRIVATE_MESSAGE_COLOR);
                default -> {
                }
            }
        };
        peer.getNetwork().getChatHub().addListener(chat_listener);
        this.peerhub = new PeerHub(animation_manager_local, ingame_info.isMultiplayer(), ingame_info.isRated(),
                local_player, player_slots, network, notification_manager,
                peer.getNetwork().getMatchmakingClient(), peer.getNetwork().getChatHub(),
                world.getDistributableTable(), session_id,
                new ViewerStallHandler(this));
        this.peerhub.setIgnoreFilter(ChatCommand::isIgnoring);
        this.in_game_chat_history = new InGameChatHistory();
        peer.getNetwork().getChatHub().addListener(in_game_chat_history);
        var matchmakingClient = peer.getNetwork().getMatchmakingClient();
        Map<String, ChatMethod> commands = Map.of("iamacheater", (_, _, _) -> cheat.enable());
        this.chat_sender = new InGameChatSender(gui_root.getInfoPrinter(), matchmakingClient, commands, peerhub);
        this.camera = new GameCamera(this, camera_state);
        this.panel = new ActionButtonPanel(this, camera);
        this.delegate = new SelectionDelegate(this, camera);
        camera.reset(getLocalPlayer().getStartX(), getLocalPlayer().getStartY());
        initPlayers(world_info.landscape().startingLocations(), player_slots, world.getPlayers(), unit_infos,
                world_params.initialGameSpeed());
        gui_root.getAnimationManager().registerAnimation(this);
    }

    public ChatSender getChatSender() {
        return chat_sender;
    }

    public InGameChatHistory getInGameChatHistory() {
        return in_game_chat_history;
    }

    public Peer getPeer() {
        return peer;
    }

    public ScreenManager getScreenManager() {
        return screenManager;
    }

    public GUIIcons getIcons() {
        return icons;
    }

    public AudioManager getAudioManager() {
        return peer.getAudioManager();
    }

    public AnimationManager getAnimationManagerLocal() {
        return animation_manager_local;
    }

    @Override
    public void animate(float dt) {
        ScopedValue.where(PeerHub.CURRENT, peerhub).run(() -> animation_manager_local.runAnimations(dt));
    }

    @Override
    public void close() {
        world.setNotificationListener(NotificationListener.EMPTY);
        peer.setPathfindCountSupplier(null);
        peer.getNetwork().getChatHub().removeListener(in_game_chat_history);
        peer.getNetwork().getChatHub().removeListener(chat_listener);
        gui_root.getAnimationManager().removeAnimation(this);
        peerhub.close();
        ingame_info.close(this);
        renderer.close();
    }

    public WorldParameters getParameters() {
        return world_params;
    }

    public Cheat getCheat() {
        return cheat;
    }

    private boolean paused;

    public void setPaused(boolean paused) {
        if (this.paused != paused) {
            this.paused = paused;
            peerhub.setPaused(paused);
        }
    }

    public boolean isPaused() {
        return peerhub.isPaused();
    }

    public Player getLocalPlayer() {
        return local_player;
    }

    private void initPlayer(ResourceBundle bundle, List<SpawnLocation> startingLocations, PlayerSlot slot,
            Player player, UnitInfo unit_info, int initial_gamespeed) {
        if (slot.getType() == PlayerSlot.AI) {
            AI ai = switch (slot.getAIDifficulty()) {
                case PlayerSlot.AI_NORMAL -> new AdvancedAI(player, player, unit_info, Difficulty.NORMAL);
                case PlayerSlot.AI_HARD -> new AdvancedAI(player, player, unit_info, Difficulty.HARD);
                case PlayerSlot.AI_EASY -> new AdvancedAI(player, player, unit_info, Difficulty.EASY);
                case PlayerSlot.AI_BATTLE_TUTORIAL -> new PassiveAI(player, player, unit_info, true);
                case PlayerSlot.AI_TOWER_TUTORIAL -> null;
                case PlayerSlot.AI_CHIEFTAIN_TUTORIAL -> {
                    new Unit(player, 100, 100, null, player.getRaceInfo().getUnitTemplate(UnitType.PEON));
                    new Unit(player, 200, 100, null, player.getRaceInfo().getUnitTemplate(UnitType.PEON));
                    new Unit(player, 40, 200, null, player.getRaceInfo().getUnitTemplate(UnitType.PEON));
                    yield null;
                }
                case PlayerSlot.AI_PASSIVE_CAMPAIGN -> new PassiveAI(player, player, unit_info, true);
                case PlayerSlot.AI_NEUTRAL_CAMPAIGN -> new PassiveAI(player, player, unit_info, false);
                default -> throw new IllegalArgumentException("unexpected difficulty: " + slot.getAIDifficulty());
            };
            if (ai != null) {
                player.setAI(ai);
                world.getAnimationManagerRealTime().registerAnimation(ai);
            }
        } else {
            player.setPreferredGamespeed(initial_gamespeed);
            int i = 0;
            for (int j = 0; j < unit_info.numPeons(); j++, i++) {
                SpawnLocation loc = startingLocations.get(i);
                new Unit(player, loc.x(), loc.y(), null, player.getRaceInfo()
                        .getUnitTemplate(UnitType.PEON));
            }
            for (int j = 0; j < unit_info.numRockWarriors(); j++, i++) {
                SpawnLocation loc = startingLocations.get(i);
                new Unit(player, loc.x(), loc.y(), null, player.getRaceInfo()
                        .getUnitTemplate(UnitType.WARRIOR_ROCK));
            }
            for (int j = 0; j < unit_info.numIronWarriors(); j++, i++) {
                SpawnLocation loc = startingLocations.get(i);
                new Unit(player, loc.x(), loc.y(), null, player.getRaceInfo()
                        .getUnitTemplate(UnitType.WARRIOR_IRON));
            }
            for (int j = 0; j < unit_info.numRubberWarriors(); j++, i++) {
                SpawnLocation loc = startingLocations.get(i);
                new Unit(player, loc.x(), loc.y(), null, player.getRaceInfo()
                        .getUnitTemplate(UnitType.WARRIOR_RUBBER));
            }
            if (unit_info.hasChieftain()) {
                SpawnLocation loc = startingLocations.get(i);
                Unit chieftain;
                if (player.getRaceInfo().getChieftainAI() instanceof VikingChieftainAI)
                    chieftain = new Unit(player, loc.x(), loc.y(), null, player
                            .getRaceInfo().getUnitTemplate(UnitType.CHIEFTAIN), Utils.getBundleString(bundle,
                                    "chieftain_name"), false);
                else if (player.getRaceInfo().getChieftainAI() instanceof NativeChieftainAI)
                    chieftain = new Unit(player, loc.x(), loc.y(), null, player
                            .getRaceInfo().getUnitTemplate(UnitType.CHIEFTAIN), Utils.getBundleString(bundle,
                                    "native_chieftain_name"), false);
                else
                    throw new IllegalStateException("Unknown chieftain AI: " + player.getRaceInfo().getChieftainAI());
                chieftain.getOwner().getRaceInfo().getMagics().forEach(chieftain::maxMagicEnergy);
                player.setActiveChieftain(chieftain);
                i++;
            }
        }
    }

    private void initPlayers(List<List<SpawnLocation>> startingLocations, PlayerSlot[] slots, List<Player> players,
            UnitInfo[] unit_infos, int initial_gamespeed) {
        ResourceBundle bundle = ResourceBundle.getBundle("com.oddlabs.tt.content.Player");
        for (int i = 0; i < slots.length; i++) {
            initPlayer(bundle, startingLocations.get(i), slots[i], players.get(i), unit_infos[i], initial_gamespeed);
        }
    }

    private LandscapeRenderer getLandscapeRenderer() {
        return landscape_renderer;
    }

    public Picker getPicker() {
        return picker;
    }

    public FullSceneRenderer getRenderer() {
        return renderer;
    }

    public World getWorld() {
        return world;
    }

    public Selection getSelection() {
        return selection;
    }

    public NotificationManager getNotificationManager() {
        return notification_manager;
    }

    public DistributableTable getDistributableTable() {
        return world.getDistributableTable();
    }

    public GUIRoot getGUIRoot() {
        return gui_root;
    }

    public boolean isMultiplayer() {
        return ingame_info.isMultiplayer();
    }

    public InGameInfo getInGameInfo() {
        return ingame_info;
    }

    public void abort() {
        ingame_info.abort(this);
    }

    public void addGameOverGUI(GameStatsDelegate delegate, int header_y, Group buttons) {
        ingame_info.addGameOverGUI(this, delegate, header_y, buttons);
    }

    public GameCamera getCamera() {
        return camera;
    }

    public PeerHub getPeerHub() {
        return peerhub;
    }

    public ActionButtonPanel getPanel() {
        return panel;
    }

    public SelectionDelegate getDelegate() {
        return delegate;
    }

    public InputManager getInputManager() {
        return gui_root.getInputManager();
    }

    public LocalEventQueue getEventQueue() {
        return gui_root.getEventQueue();
    }

    public AnimationManager getAnimationManager() {
        return gui_root.getAnimationManager();
    }

    public AnimationManager getAnimationManagerHighPrecision() {
        return gui_root.getEventQueue().getHighPrecisionManager();
    }

    public float getTime() {
        return gui_root.getTime();
    }

    private static void registerHarvestEmojis(RenderQueues queues, GUIIcons icons) {
        AssetRegistry ar = AssetRegistry.getInstance();
        ar.registerEmoji(EmojiType.HARVEST_WOOD, registerIconSprite(queues, icons.getTreeStatusIcon()));
        ar.registerEmoji(EmojiType.HARVEST_ROCK, registerIconSprite(queues, icons.getRockStatusIcon()));
        ar.registerEmoji(EmojiType.HARVEST_IRON, registerIconSprite(queues, icons.getIronStatusIcon()));
        ar.registerEmoji(EmojiType.HARVEST_RUBBER, registerIconSprite(queues, icons.getRubberStatusIcon()));
    }

    private static SpriteKey registerIconSprite(RenderQueues queues, IconQuad icon) {
        return queues.registerQuadSprite(icon.getU1(), icon.getV1(), icon.getU2(), icon.getV2(), icon.getTexture());
    }
}
