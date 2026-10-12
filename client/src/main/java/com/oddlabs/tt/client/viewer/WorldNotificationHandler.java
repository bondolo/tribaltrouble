package com.oddlabs.tt.client.viewer;

import com.oddlabs.tt.audio.AudioManager;
import com.oddlabs.tt.audio.AudioParameters;
import com.oddlabs.tt.base.animation.AnimationManager;
import com.oddlabs.tt.base.util.Utils;
import com.oddlabs.tt.client.Peer;
import com.oddlabs.tt.client.render.FullSceneRenderer;
import com.oddlabs.tt.client.resource.AssetRegistry;
import com.oddlabs.tt.client.render.VisualSoundAccessory;
import com.oddlabs.tt.client.resource.EmojiType;
import com.oddlabs.tt.engine.settings.AccessibilitySettings;
import com.oddlabs.tt.net.PeerHub;
import com.oddlabs.tt.net.ServerMessageBundler;
import com.oddlabs.tt.scenery.LandscapeRenderer;
import com.oddlabs.tt.simulation.landscape.NotificationListener;
import com.oddlabs.tt.simulation.landscape.TreeSupply;
import com.oddlabs.tt.simulation.landscape.World;
import com.oddlabs.tt.simulation.model.Building;
import com.oddlabs.tt.simulation.model.Model;
import com.oddlabs.tt.simulation.model.Race;
import com.oddlabs.tt.simulation.model.Selectable;
import com.oddlabs.tt.simulation.model.SupplyModel;
import com.oddlabs.tt.simulation.model.SupplyType;
import com.oddlabs.tt.simulation.model.Target;
import com.oddlabs.tt.simulation.model.Unit;
import com.oddlabs.tt.simulation.model.UnitType;
import com.oddlabs.tt.simulation.player.Player;

import com.oddlabs.tt.audio.AudioFile;
import com.oddlabs.tt.client.resource.AudioRegistry;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Handles simulation notification events for the world viewer, dispatching audio, visual,
 * and UI updates in response to gameplay events.
 */
final class WorldNotificationHandler implements NotificationListener {
    private static final List<String> GAMESPEED_NAMES = List.of("paused", "slow", "normal", "fast", "ludicrous");

    private final WorldViewer viewer;
    private final Peer peer;
    private final com.oddlabs.tt.gui.GUIRoot guiRoot;
    private final NotificationManager notificationManager;
    private final AnimationManager animationManager;
    private final AudioManager audioManager;
    private final boolean isMultiplayer;
    private final FullSceneRenderer renderer;
    private final LandscapeRenderer landscapeRenderer;

    WorldNotificationHandler(WorldViewer viewer,
            Peer peer,
            com.oddlabs.tt.gui.GUIRoot guiRoot,
            NotificationManager notificationManager,
            AnimationManager animationManager,
            AudioManager audioManager,
            boolean isMultiplayer,
            FullSceneRenderer renderer,
            LandscapeRenderer landscapeRenderer) {
        this.viewer = viewer;
        this.peer = peer;
        this.guiRoot = guiRoot;
        this.notificationManager = notificationManager;
        this.animationManager = animationManager;
        this.audioManager = audioManager;
        this.isMultiplayer = isMultiplayer;
        this.renderer = renderer;
        this.landscapeRenderer = landscapeRenderer;
    }

    @Override
    public void gamespeedChanged(int speed) {
        guiRoot.getInfoPrinter().print(Utils.getBundleString(PeerHub.bundle, "changed_to_"
                + GAMESPEED_NAMES.get(speed)));
    }

    @Override
    public void playerGamespeedChanged() {
        String result = viewer.getWorld().getPlayers().stream()
                .filter(p -> World.isValidGamespeed(p.getPreferredGamespeed()))
                .map(p -> p.getPlayerInfo().name() + ": " + ServerMessageBundler.getGamespeedString(
                        p.getPreferredGamespeed()))
                .collect(Collectors.joining(", "));
        if (!result.isEmpty() && isMultiplayer) {
            guiRoot.getInfoPrinter().print(result);
        }
    }

    @Override
    public void newAttackNotification(Selectable<?> target) {
        Player localPlayer = viewer.getLocalPlayer();
        if (target.getOwner() == localPlayer) {
            notificationManager.newAttackNotification(animationManager, target, localPlayer);
        }
    }

    @Override
    public void newSelectableNotification(Selectable<?> target) {
        Player localPlayer = viewer.getLocalPlayer();
        if (target.getOwner() == localPlayer) {
            notificationManager.newSelectableNotification(target, animationManager, localPlayer);
        }
        if (target instanceof Building building) {
            landscapeRenderer.onStaticModelAdded(building);
        }
    }

    @Override
    public void treeFelled(TreeSupply tree) {
        audioManager.newAudio(tree.getCX(), tree.getCY(), tree.getCZ(),
                AudioRegistry.TREE_FALL[tree.getTreeType().ordinal() % 2]);
        renderer.onTreeFelled(tree);
    }

    @Override
    public void onHarvest(Model model, SupplyType supplyType) {
        playAudioWithVisual(model, AudioRegistry.getHarvestSound(supplyType),
                EmojiType.fromSupply(supplyType));
    }

    @Override
    public void onRepair(Model model) {
        var emoji = ThreadLocalRandom.current().nextBoolean() ? EmojiType.REPAIR_SAW : EmojiType.REPAIR_HAMMER;
        playAudioWithVisual(model, AudioRegistry.getHarvestSound(SupplyType.WOOD), emoji);
    }

    @Override
    public void onBuildingHit(float x, float y, float z) {
        audioManager.newAudio(x, y, z,
                AudioRegistry.BUILDING_HITS[ThreadLocalRandom.current().nextInt(
                        AudioRegistry.BUILDING_HITS.length)]);
    }

    @Override
    public void onUnitDeath(Unit unit, UnitType unitType, Race race) {
        AudioFile deathSound = switch (unitType) {
            case PEON -> AudioRegistry.SFX_DEATH_PEON;
            case WARRIOR_ROCK -> (race == Race.VIKINGS)
                    ? AudioRegistry.SFX_DEATH_VIKING_WARRIORS[0]
                    : AudioRegistry.SFX_DEATH_NATIVE_WARRIORS[0];
            case WARRIOR_IRON, WARRIOR_RUBBER, CHIEFTAIN -> (race == Race.VIKINGS)
                    ? AudioRegistry.SFX_DEATH_VIKING_WARRIORS[1]
                    : AudioRegistry.SFX_DEATH_NATIVE_WARRIORS[1];
        };
        var params = new AudioParameters(deathSound, AudioRegistry.AUDIO_RANK_DEATH,
                AudioRegistry.AUDIO_DISTANCE_DEATH, AudioRegistry.AUDIO_GAIN_DEATH,
                AudioRegistry.AUDIO_RADIUS_DEATH);
        playAudioWithVisual(unit, params, EmojiType.GRAVESTONE);
    }

    @Override
    public void onUnitAttack(UnitType unitType, Race race, float x, float y, float z) {
        AudioFile sound;
        if (unitType == UnitType.CHIEFTAIN) {
            AudioFile[] hits = (race == Race.VIKINGS)
                    ? AudioRegistry.SFX_VIKING_CHIEFTAIN_HITS
                    : AudioRegistry.SFX_NATIVE_CHIEFTAIN_HITS;
            sound = hits[ThreadLocalRandom.current().nextInt(hits.length)];
        } else {
            sound = AudioRegistry.SFX_IMPACT_MEATS[ThreadLocalRandom.current().nextInt(
                    AudioRegistry.SFX_IMPACT_MEATS.length)];
        }
        var params = new AudioParameters(sound, AudioRegistry.AUDIO_RANK_WEAPON_HIT,
                AudioRegistry.AUDIO_DISTANCE_WEAPON_HIT, AudioRegistry.AUDIO_GAIN_WEAPON_HIT,
                AudioRegistry.AUDIO_RADIUS_WEAPON_HIT);
        audioManager.newAudio(x, y, z, params);
    }

    @Override
    public void onChickenCluck(Model model) {
        AudioParameters sound = AudioRegistry.CHICKEN_IDLES[ThreadLocalRandom.current().nextInt(
                AudioRegistry.CHICKEN_IDLES.length)];
        playAudioWithVisual(model, sound, EmojiType.CHICKEN_CLUCK);
    }

    @Override
    public void onChickenPeck(float x, float y, float z) {
        audioManager.newAudio(x, y, z, AudioRegistry.CHICKEN_PECK);
    }

    @Override
    public void onChickenDeath(Model model) {
        playAudioWithVisual(model, AudioRegistry.CHICKEN_DEATH, EmojiType.HARVEST_RUBBER);
    }

    @Override
    public void registerTarget(Target target) {
        if (target instanceof SupplyModel supplyModel) {
            renderer.getRenderState().onSupplySpawn(supplyModel);
        } else if (target instanceof TreeSupply tree) {
            renderer.onTreeSpawned(tree);
        }
        if (target instanceof Model model) {
            landscapeRenderer.onStaticModelAdded(model);
        }
    }

    @Override
    public void unregisterTarget(Target target) {
        if (target instanceof Selectable<?> selectable) {
            viewer.getSelection().removeFromArmies(selectable);
        }
    }

    @Override
    public void onLightningStrike(Model model, float x, float y, float z) {
        renderer.getRenderState().onLightningStrike(model, x, y, z);
    }

    @Override
    public void onSonicBlast(Model model, float targetX, float targetY, float targetZ, float radius,
            float duration) {
        renderer.getRenderState().onSonicBlast(model, targetX, targetY, targetZ, radius, duration);
    }

    @Override
    public void onWeaponThrow(float x, float y, float z) {
        var params = new AudioParameters(AudioRegistry.SFX_WEAPON_SPEAR, AudioRegistry.AUDIO_RANK_WEAPON_ATTACK,
                AudioRegistry.AUDIO_DISTANCE_WEAPON_ATTACK, AudioRegistry.AUDIO_GAIN_WEAPON_ATTACK,
                AudioRegistry.AUDIO_RADIUS_WEAPON_ATTACK);
        audioManager.newAudio(x, y, z, params);
    }

    @Override
    public void onModelRemoved(Model model) {
        renderer.getRenderState().onModelRemoved(model);
        landscapeRenderer.onStaticModelRemoved(model);
    }

    @Override
    public void onHeightEdited(int gridX, int gridY, float height) {
        landscapeRenderer.getHeightMapVisual().editHeight(gridX, gridY, height);
    }

    private void playAudioWithVisual(Model model, AudioParameters sound, EmojiType emoji) {
        audioManager.newAudio(model.getPositionX(), model.getPositionY(), model.getPositionZ(), sound);
        if (AccessibilitySettings.from(peer.getSettings()).sound_emojis) {
            addVisualSound(model, emoji, sound.distance());
        }
    }

    private void addVisualSound(Model model, EmojiType emoji, float audioDistance) {
        AssetRegistry.getInstance().getEmojiSprite(emoji)
                .map(sprite -> new VisualSoundAccessory(sprite, emoji.getDuration(), audioDistance))
                .ifPresent(accessory -> renderer.getRenderState().addAccessory(model, accessory));
    }
}
