package com.oddlabs.tt.client.viewer;

import com.oddlabs.net.NetworkSelector;
import com.oddlabs.router.SessionID;
import com.oddlabs.tt.simulation.model.Model;

import com.oddlabs.tt.audio.AudioManager;
import com.oddlabs.tt.audio.AudioParameters;
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
import com.oddlabs.tt.client.render.DefaultRenderer;
import com.oddlabs.tt.client.render.Picker;
import com.oddlabs.tt.client.render.RacesAssetsLoader;
import com.oddlabs.tt.client.Peer;
import com.oddlabs.tt.engine.settings.AccessibilitySettings;
import com.oddlabs.tt.engine.settings.GraphicsSettings;
import com.oddlabs.tt.engine.render.CameraState;
import com.oddlabs.tt.engine.render.LandscapeBaker;
import com.oddlabs.tt.engine.render.LandscapeRenderer;
import com.oddlabs.tt.engine.render.LandscapeAssetsLoader;
import com.oddlabs.tt.engine.render.MatrixStack;
import com.oddlabs.tt.engine.render.RenderConfig;
import com.oddlabs.tt.engine.render.RenderQueues;
import com.oddlabs.tt.engine.render.Texture;
import com.oddlabs.tt.procedural.GeneratedLandscapeData;
import com.oddlabs.tt.engine.resource.AudioAssets;
import com.oddlabs.tt.engine.resource.WorldInfo;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.Group;
import com.oddlabs.tt.gui.InfoPrinter;
import com.oddlabs.tt.input.InputManager;
import com.oddlabs.tt.net.ChatListener;
import com.oddlabs.tt.net.ChatMethod;
import com.oddlabs.tt.net.ChatSender;
import com.oddlabs.tt.net.InGameChatHistory;
import com.oddlabs.tt.net.PeerHub;
import com.oddlabs.tt.net.ServerMessageBundler;
import com.oddlabs.tt.simulation.landscape.AbstractTreeGroup;
import com.oddlabs.tt.simulation.landscape.NotificationListener;
import com.oddlabs.tt.simulation.landscape.World;
import com.oddlabs.tt.simulation.model.DistributableTable;
import com.oddlabs.tt.simulation.landscape.WorldGenerator;
import com.oddlabs.tt.simulation.landscape.WorldParameters;
import com.oddlabs.tt.simulation.model.Difficulty;
import com.oddlabs.tt.simulation.model.RaceData;
import com.oddlabs.tt.simulation.model.Selectable;
import com.oddlabs.tt.simulation.model.SupplyModel;
import com.oddlabs.tt.simulation.model.Target;
import com.oddlabs.tt.simulation.model.Unit;
import com.oddlabs.tt.simulation.model.UnitType;
import com.oddlabs.tt.simulation.player.AI;
import com.oddlabs.tt.simulation.player.AdvancedAI;
import com.oddlabs.tt.simulation.player.NativeChieftainAI;
import com.oddlabs.tt.simulation.player.PassiveAI;
import com.oddlabs.tt.simulation.player.Player;
import com.oddlabs.tt.simulation.player.PlayerInfo;
import com.oddlabs.tt.simulation.player.PlayerSlot;
import com.oddlabs.tt.simulation.player.UnitInfo;
import com.oddlabs.tt.simulation.player.VikingChieftainAI;

import com.oddlabs.tt.audio.AudioFile;
import com.oddlabs.tt.simulation.model.EmojiType;
import com.oddlabs.tt.simulation.model.Race;
import com.oddlabs.tt.simulation.model.SupplyType;
import com.oddlabs.tt.simulation.model.UnitVisualType;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Orchestrates the primary in-game experience, managing the world state, player interactions,
 * rendering, and the user interface for a single player.
 * Coordinates camera state and audio listener updates for the game world.
 */
public final class WorldViewer implements Animated, AutoCloseable {

    private static final String[] GAMESPEED_STRINGS = new String[]{"paused", "slow", "normal", "fast", "ludicrous"};

    private final Peer engine;
    private final GameCamera camera;
    private final ActionButtonPanel panel;
    private final SelectionDelegate delegate;
    private final PeerHub peerhub;
    private final GUIRoot gui_root;
    private final NotificationManager notification_manager;
    private final InGameInfo ingame_info;
    private final NetworkSelector network;
    private final Selection selection;
    private final World world;
    private final Picker picker;
    private final DefaultRenderer renderer;
    private final LandscapeRenderer landscape_renderer;
    private final Player local_player;
    private final WorldParameters world_params;
    private final AnimationManager animation_manager_local;
    private final Cheat cheat;
    private final AudioManager audioManager;
    private final ChatListener chat_listener;
    private final ChatSender chat_sender;
    private final InGameChatHistory in_game_chat_history;

    public WorldViewer(final GUIRoot gui_root,
            Peer engine,
            WorldParameters world_params, InGameInfo ingame_info, WorldGenerator<GeneratedLandscapeData> generator,
            PlayerSlot[] player_slots, UnitInfo[] unit_infos, short player_slot,
            SessionID session_id) {
        this.engine = engine;
        this.world_params = world_params;
        this.ingame_info = ingame_info;
        this.network = engine.getNetwork().getSelector();
        this.cheat = new Cheat(!ingame_info.isMultiplayer());
        this.audioManager = engine.getAudioManager();
        this.animation_manager_local = new AnimationManager();
        final CameraState camera_state = new CameraState();
        this.notification_manager = new NotificationManager(gui_root, audioManager);
        MatrixStack modelViewStack = new MatrixStack();
        MatrixStack projectionStack = new MatrixStack();
        RenderQueues render_queues = new RenderQueues();
        LandscapeAssetsLoader landscape_resources = ProgressListener.subTask(0.15f,
                () -> new LandscapeAssetsLoader(render_queues));
        RaceData races_resources = ProgressListener.subTask(0.35f,
                () -> RacesAssetsLoader.load(render_queues));
        boolean[] initialized = new boolean[]{false};
        NotificationListener listener = new NotificationListener() {
            @Override
            public void gamespeedChanged(int speed) {
                gui_root.getInfoPrinter().print(Utils.getBundleString(PeerHub.bundle, "changed_to_"
                        + GAMESPEED_STRINGS[speed]));
            }

            @Override
            public void playerGamespeedChanged() {
                String result = world.getPlayers().stream()
                        .filter(p -> World.isValidGamespeed(p.getPreferredGamespeed()))
                        .map(p -> p.getPlayerInfo().getName() + ": " + ServerMessageBundler.getGamespeedString(p
                                .getPreferredGamespeed()))
                        .collect(Collectors.joining(", "));
                if (!result.isEmpty() && isMultiplayer())
                    gui_root.getInfoPrinter().print(result);
            }

            @Override
            public void newAttackNotification(Selectable<?> target) {
                Player owner = target.getOwner();
                if (owner == getLocalPlayer())
                    notification_manager.newAttackNotification(animation_manager_local, target, getLocalPlayer());
            }

            @Override
            public void newSelectableNotification(Selectable<?> target) {
                Player owner = target.getOwner();
                if (owner == getLocalPlayer())
                    notification_manager.newSelectableNotification(target, animation_manager_local, getLocalPlayer());
            }

            @Override
            public void treeFelled(AbstractTreeGroup.TreeType treeType, float x, float y, float z) {
                audioManager.newAudio(x, y, z, AudioAssets.TREE_FALL[treeType.ordinal() % 2]);
            }

            @Override
            public void onHarvest(Model model, SupplyType supplyType) {
                audioManager.newAudio(model.getPositionX(), model.getPositionY(), model.getPositionZ(),
                        AudioAssets.getHarvestSound(supplyType));
                addVisualSound(model, EmojiType.fromSupply(supplyType), AudioAssets.AUDIO_DISTANCE_DEATH);
            }

            @Override
            public void onRepair(Model model) {
                audioManager.newAudio(model.getPositionX(), model.getPositionY(), model.getPositionZ(),
                        AudioAssets.getHarvestSound(SupplyType.WOOD));
                var emoji = ThreadLocalRandom.current().nextBoolean() ? EmojiType.REPAIR_SAW : EmojiType.REPAIR_HAMMER;
                addVisualSound(model, emoji, AudioAssets.AUDIO_DISTANCE_DEATH);
            }

            @Override
            public void onBuildingHit(float x, float y, float z) {
                audioManager.newAudio(x, y, z,
                        AudioAssets.BUILDING_HITS[ThreadLocalRandom.current().nextInt(
                                AudioAssets.BUILDING_HITS.length)]);
            }

            @Override
            public void onUnitDeath(Unit unit, UnitVisualType unitType, Race race) {
                AudioFile deathSound = switch (unitType) {
                    case PEON -> AudioAssets.SFX_DEATH_PEON;
                    case WARRIOR_ROCK -> (race == Race.VIKINGS)
                            ? AudioAssets.SFX_DEATH_VIKING_WARRIORS[0]
                            : AudioAssets.SFX_DEATH_NATIVE_WARRIORS[0];
                    case WARRIOR_IRON, WARRIOR_RUBBER, CHIEFTAIN -> (race == Race.VIKINGS)
                            ? AudioAssets.SFX_DEATH_VIKING_WARRIORS[1]
                            : AudioAssets.SFX_DEATH_NATIVE_WARRIORS[1];
                };
                var params = new AudioParameters(deathSound, AudioAssets.AUDIO_RANK_DEATH,
                        AudioAssets.AUDIO_DISTANCE_DEATH, AudioAssets.AUDIO_GAIN_DEATH, AudioAssets.AUDIO_RADIUS_DEATH);
                audioManager.newAudio(unit.getPositionX(), unit.getPositionY(), unit.getPositionZ(), params);
                addVisualSound(unit, EmojiType.GRAVESTONE, AudioAssets.AUDIO_DISTANCE_DEATH);
            }

            @Override
            public void onUnitAttack(UnitVisualType unitType, Race race, float x, float y, float z) {
                AudioFile sound;
                if (unitType == UnitVisualType.CHIEFTAIN) {
                    AudioFile[] hits = (race == Race.VIKINGS)
                            ? AudioAssets.SFX_VIKING_CHIEFTAIN_HITS
                            : AudioAssets.SFX_NATIVE_CHIEFTAIN_HITS;
                    sound = hits[ThreadLocalRandom.current().nextInt(hits.length)];
                } else {
                    sound = AudioAssets.SFX_IMPACT_MEATS[ThreadLocalRandom.current().nextInt(
                            AudioAssets.SFX_IMPACT_MEATS.length)];
                }
                var params = new AudioParameters(sound, AudioAssets.AUDIO_RANK_WEAPON_HIT,
                        AudioAssets.AUDIO_DISTANCE_WEAPON_HIT, AudioAssets.AUDIO_GAIN_WEAPON_HIT,
                        AudioAssets.AUDIO_RADIUS_WEAPON_HIT);
                audioManager.newAudio(x, y, z, params);
            }

            @Override
            public void onChickenCluck(Model model) {
                audioManager.newAudio(model.getPositionX(), model.getPositionY(), model.getPositionZ(),
                        AudioAssets.CHICKEN_IDLES[ThreadLocalRandom.current().nextInt(
                                AudioAssets.CHICKEN_IDLES.length)]);
                addVisualSound(model, EmojiType.CHICKEN_CLUCK, AudioAssets.AUDIO_DISTANCE_DEATH);
            }

            @Override
            public void onChickenPeck(float x, float y, float z) {
                audioManager.newAudio(x, y, z, AudioAssets.CHICKEN_PECK);
            }

            @Override
            public void onChickenDeath(Model model) {
                audioManager.newAudio(model.getPositionX(), model.getPositionY(), model.getPositionZ(),
                        AudioAssets.CHICKEN_DEATH);
                addVisualSound(model, EmojiType.HARVEST_RUBBER, AudioAssets.AUDIO_DISTANCE_DEATH);
            }

            @Override
            public void registerTarget(Target target) {
                if (initialized[0] && target instanceof SupplyModel supplyModel) {
                    WorldViewer.this.renderer.getRenderState().onSupplySpawn(supplyModel);
                }
            }

            @Override
            public void unregisterTarget(Target target) {
                if (target instanceof Selectable<?> selectable)
                    getSelection().removeFromArmies(selectable);
            }

            @Override
            public void onLightningStrike(Model model, float x, float y, float z) {
                WorldViewer.this.renderer.getRenderState().onLightningStrike(model, x, y, z);
            }

            @Override
            public void onSonicBlast(Model model, float targetX, float targetY, float targetZ, float radius,
                    float duration) {
                WorldViewer.this.renderer.getRenderState().onSonicBlast(model, targetX, targetY, targetZ, radius,
                        duration);
            }

            @Override
            public void onWeaponThrow(float x, float y, float z) {
                var params = new AudioParameters(AudioAssets.SFX_WEAPON_SPEAR, AudioAssets.AUDIO_RANK_WEAPON_ATTACK,
                        AudioAssets.AUDIO_DISTANCE_WEAPON_ATTACK, AudioAssets.AUDIO_GAIN_WEAPON_ATTACK,
                        AudioAssets.AUDIO_RADIUS_WEAPON_ATTACK);
                audioManager.newAudio(x, y, z, params);
            }

            @Override
            public void onModelRemoved(Model model) {
                WorldViewer.this.renderer.getRenderState().onModelRemoved(model);
            }
        };
        var player_infos = Arrays.stream(player_slots).map(slot -> (PlayerInfo) slot.getInfo()).toList();
        GeneratedLandscapeData landscapeData = ProgressListener.subTask(0.25f,
                () -> generator.generate(
                        player_infos.size(), world_params.initialUnitCount(), ingame_info.getRandomStartPosition()));
        WorldInfo<Texture> world_info = ProgressListener.subTask(0.15f,
                () -> LandscapeBaker.bakeWorld(landscapeData));
        camera_state.setFog(world_info.fog_info());
        this.world = ProgressListener.subTask(0.10f,
                () -> World.newWorld(landscape_resources, races_resources, listener, world_params,
                        world_info.landscapeData(), player_infos, AccessibilitySettings.from(engine
                                .getSettings()).linear_team_colours,
                        RenderConfig.INSERT_PLANTS[GraphicsSettings.from(engine.getSettings()).graphic_detail]));
        initialized[0] = true;
        this.local_player = world.getPlayers().get(player_slot);
        this.selection = new Selection(local_player);
        landscape_renderer = new LandscapeRenderer(world, world_info, animation_manager_local);
        this.picker = new Picker(animation_manager_local, local_player, gui_root, render_queues, landscape_renderer,
                selection, audioManager);
        this.renderer = new DefaultRenderer(cheat, local_player, render_queues,
                world_info, landscape_renderer, picker,
                selection, modelViewStack, projectionStack, audioManager, engine.getSettings(),
                gui_root.getWidth(), gui_root.getHeight());
        this.gui_root = gui_root;
        this.gui_root.setCheatIcon(GUIIcons.getIcons().getCheatIcon());
        this.chat_listener = message -> {
            var infoPrinter = gui_root.getInfoPrinter();
            switch (message.type()) {
                case NORMAL -> infoPrinter.print(message.formatShort());
                case TEAM -> infoPrinter.print(message.formatShort(), InfoPrinter.TEAM_COLOR);
                case PRIVATE -> infoPrinter.print(message.formatShort(), InfoPrinter.PRIVATE_COLOR);
                default -> {
                }
            }
        };
        engine.getNetwork().getChatHub().addListener(chat_listener);
        this.peerhub = new PeerHub(animation_manager_local, ingame_info.isMultiplayer(), ingame_info.isRated(),
                local_player, player_slots, network, notification_manager,
                engine.getNetwork().getMatchmakingClient(), engine.getNetwork().getChatHub(),
                world.getDistributableTable(), session_id,
                new ViewerStallHandler(this));
        this.peerhub.setIgnoreFilter(ChatCommand::isIgnoring);
        this.in_game_chat_history = new InGameChatHistory();
        engine.getNetwork().getChatHub().addListener(in_game_chat_history);
        var matchmakingClient = engine.getNetwork().getMatchmakingClient();
        Map<String, ChatMethod> commands = Map.of("iamacheater", (_, _, _) -> cheat.enable());
        this.chat_sender = new InGameChatSender(gui_root.getInfoPrinter(), matchmakingClient, commands, peerhub);
        this.camera = new GameCamera(this, camera_state);
        this.panel = new ActionButtonPanel(this, camera);
        this.delegate = new SelectionDelegate(this, camera);
        camera.reset(getLocalPlayer().getStartX(), getLocalPlayer().getStartY());
        initPlayers(world_info.landscapeData().startingLocations(), player_slots, world.getPlayers(), unit_infos,
                world_params.initialGameSpeed());
        gui_root.getAnimationManager().registerAnimation(this);
    }

    public ChatSender getChatSender() {
        return chat_sender;
    }

    public InGameChatHistory getInGameChatHistory() {
        return in_game_chat_history;
    }

    public Peer getEngine() {
        return engine;
    }

    public AudioManager getAudioManager() {
        return audioManager;
    }

    public AnimationManager getAnimationManagerLocal() {
        return animation_manager_local;
    }

    @Override
    public void animate(float t) {
        animation_manager_local.runAnimations(t);
    }

    @Override
    public void close() {
        engine.getNetwork().getChatHub().removeListener(in_game_chat_history);
        engine.getNetwork().getChatHub().removeListener(chat_listener);
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

    private void initPlayer(ResourceBundle bundle, float[] starting_location, PlayerSlot slot,
            Player player, UnitInfo unit_info, int initial_gamespeed) {
        if (slot.getType() == PlayerSlot.AI) {
            AI ai = switch (slot.getAIDifficulty()) {
                case PlayerSlot.AI_NORMAL -> new AdvancedAI(player, unit_info, Difficulty.NORMAL);
                case PlayerSlot.AI_HARD -> new AdvancedAI(player, unit_info, Difficulty.HARD);
                case PlayerSlot.AI_EASY -> new AdvancedAI(player, unit_info, Difficulty.EASY);
                case PlayerSlot.AI_BATTLE_TUTORIAL -> new PassiveAI(player, unit_info, true);
                case PlayerSlot.AI_TOWER_TUTORIAL -> null;
                case PlayerSlot.AI_CHIEFTAIN_TUTORIAL -> {
                    new Unit(player, 100, 100, null, player.getRaceInfo().getUnitTemplate(UnitType.PEON));
                    new Unit(player, 200, 100, null, player.getRaceInfo().getUnitTemplate(UnitType.PEON));
                    new Unit(player, 40, 200, null, player.getRaceInfo().getUnitTemplate(UnitType.PEON));
                    yield null;
                }
                case PlayerSlot.AI_PASSIVE_CAMPAIGN -> new PassiveAI(player, unit_info, true);
                case PlayerSlot.AI_NEUTRAL_CAMPAIGN -> new PassiveAI(player, unit_info, false);
                default -> throw new IllegalArgumentException("unexpected difficulty: " + slot.getAIDifficulty());
            };
            player.setAI(ai);
        } else {
            player.setPreferredGamespeed(initial_gamespeed);
            int i = 0;
            for (int j = 0; j < unit_info.numPeons(); j++, i++) {
                new Unit(player, starting_location[2 * i], starting_location[2 * i + 1], null, player.getRaceInfo()
                        .getUnitTemplate(UnitType.PEON));
            }
            for (int j = 0; j < unit_info.numRockWarriors(); j++, i++) {
                new Unit(player, starting_location[2 * i], starting_location[2 * i + 1], null, player.getRaceInfo()
                        .getUnitTemplate(UnitType.WARRIOR_ROCK));
            }
            for (int j = 0; j < unit_info.numIronWarriors(); j++, i++) {
                new Unit(player, starting_location[2 * i], starting_location[2 * i + 1], null, player.getRaceInfo()
                        .getUnitTemplate(UnitType.WARRIOR_IRON));
            }
            for (int j = 0; j < unit_info.numRubberWarriors(); j++, i++) {
                new Unit(player, starting_location[2 * i], starting_location[2 * i + 1], null, player.getRaceInfo()
                        .getUnitTemplate(UnitType.WARRIOR_RUBBER));
            }
            if (unit_info.hasChieftain()) {
                Unit chieftain;
                if (player.getRaceInfo().getChieftainAI() instanceof VikingChieftainAI)
                    chieftain = new Unit(player, starting_location[2 * i], starting_location[2 * i + 1], null, player
                            .getRaceInfo().getUnitTemplate(UnitType.CHIEFTAIN), Utils.getBundleString(bundle,
                                    "chieftain_name"), false);
                else if (player.getRaceInfo().getChieftainAI() instanceof NativeChieftainAI)
                    chieftain = new Unit(player, starting_location[2 * i], starting_location[2 * i + 1], null, player
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

    private void initPlayers(float[][] starting_locations, PlayerSlot[] slots, List<Player> players,
            UnitInfo[] unit_infos, int initial_gamespeed) {
        ResourceBundle bundle = ResourceBundle.getBundle("com.oddlabs.tt.content.Player");
        for (int i = 0; i < slots.length; i++) {
            initPlayer(bundle, starting_locations[i], slots[i], players.get(i), unit_infos[i], initial_gamespeed);
        }
    }

    private LandscapeRenderer getLandscapeRenderer() {
        return landscape_renderer;
    }

    public Picker getPicker() {
        return picker;
    }

    public DefaultRenderer getRenderer() {
        return renderer;
    }

    public World getWorld() {
        return world;
    }

    public NetworkSelector getNetwork() {
        return network;
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

    private void addVisualSound(Model model, EmojiType emoji, float audioDistance) {
        if (AccessibilitySettings.from(engine.getSettings()).sound_emojis) {
            renderer.getRenderState().addVisualSound(model, emoji, audioDistance);
        }
    }
}
