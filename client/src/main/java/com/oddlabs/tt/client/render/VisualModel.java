package com.oddlabs.tt.client.render;

import com.oddlabs.tt.audio.AudioFile;
import com.oddlabs.tt.audio.AudioImplementation;
import com.oddlabs.tt.audio.AudioParameters;
import com.oddlabs.tt.engine.render.Accessory;
import com.oddlabs.tt.engine.render.AnimatedAccessory;
import com.oddlabs.tt.engine.render.LightningAccessory;
import com.oddlabs.tt.engine.render.SonicBlastAccessory;
import com.oddlabs.tt.engine.resource.AssetRegistry;
import com.oddlabs.tt.engine.resource.AudioAssets;
import com.oddlabs.tt.simulation.model.EmojiType;
import com.oddlabs.tt.simulation.model.Model;
import com.oddlabs.tt.simulation.model.Race;
import com.oddlabs.tt.simulation.model.SupplyType;
import com.oddlabs.tt.simulation.model.Unit;
import com.oddlabs.tt.simulation.model.UnitVisualType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Manages the client-side visual state (accessories) and audio dispatch for a simulation model.
 */
public final class VisualModel implements AutoCloseable {
    public static final float DURATION_CHICKEN_CLUCK = 0.8f;
    public static final float DURATION_UNIT_DEATH = 1.5f;
    public static final float DURATION_HARVEST = 1.0f;
    public static final float DURATION_REPAIR = 1.0f;

    private final Model model;
    private final AudioImplementation audio;
    private final List<Accessory> accessories = new ArrayList<>();

    public VisualModel(Model model, AudioImplementation audio) {
        this.model = model;
        this.audio = audio;
    }

    public List<Accessory> getAccessories() {
        return accessories;
    }

    public Model getModel() {
        return model;
    }

    public boolean isExpired() {
        if (accessories.isEmpty()) {
            return true;
        }
        for (Accessory acc : accessories) {
            if (!acc.isExpired()) {
                return false;
            }
        }
        return true;
    }

    public void update(float t) {
        boolean hasExpired = false;
        for (Accessory acc : accessories) {
            if (acc instanceof AnimatedAccessory animated) {
                animated.animate(t);
            }
            if (acc.isExpired()) {
                hasExpired = true;
            }
        }
        if (hasExpired) {
            for (Accessory acc : accessories) {
                if (acc.isExpired()) {
                    acc.close();
                }
            }
            accessories.removeIf(Accessory::isExpired);
        }
    }

    @Override
    public void close() {
        for (Accessory acc : accessories) {
            acc.close();
        }
        accessories.clear();
    }

    public void onHarvest(SupplyType supplyType) {
        var params = AudioAssets.getHarvestSound(supplyType);
        audio.newAudio(model.getPositionX(), model.getPositionY(), model.getPositionZ(), params);
        addVisualSound(EmojiType.fromSupply(supplyType), DURATION_HARVEST,
                AudioAssets.AUDIO_DISTANCE_HARVEST);
    }

    public void onRepair() {
        audio.newAudio(model.getPositionX(), model.getPositionY(), model.getPositionZ(),
                AudioAssets.getHarvestSound(SupplyType.WOOD));
        var selectedEmoji = ThreadLocalRandom.current().nextBoolean() ? EmojiType.REPAIR_SAW : EmojiType.REPAIR_HAMMER;
        addVisualSound(selectedEmoji, DURATION_REPAIR, AudioAssets.AUDIO_DISTANCE_HARVEST);
    }

    public void onBuildingHit() {
        audio.newAudio(model.getPositionX(), model.getPositionY(), model.getPositionZ(),
                AudioAssets.BUILDING_HITS[ThreadLocalRandom.current().nextInt(AudioAssets.BUILDING_HITS.length)]);
    }

    public void onUnitDeath(Race race, UnitVisualType unitType, float pitchRange) {
        addVisualSound(EmojiType.GRAVESTONE, DURATION_UNIT_DEATH, AudioAssets.AUDIO_DISTANCE_DEATH);
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
                AudioAssets.AUDIO_DISTANCE_DEATH, AudioAssets.AUDIO_GAIN_DEATH, AudioAssets.AUDIO_RADIUS_DEATH,
                1f + (pitchRange > 0f ? ThreadLocalRandom.current().nextFloat(-0.5f * pitchRange, 0.5f * pitchRange)
                        : 0f));
        audio.newAudio(model.getPositionX(), model.getPositionY(), model.getPositionZ(), params);
    }

    public void onMeleeHit(float targetX, float targetY, float targetZ, float pitchRange) {
        AudioFile sound;
        if (model instanceof Unit unit && unit.getTemplate().getVisualType() == UnitVisualType.CHIEFTAIN) {
            AudioFile[] hits = (unit.getOwner().getRaceInfo().getRaceType() == Race.VIKINGS)
                    ? AudioAssets.SFX_VIKING_CHIEFTAIN_HITS
                    : AudioAssets.SFX_NATIVE_CHIEFTAIN_HITS;
            sound = hits[ThreadLocalRandom.current().nextInt(hits.length)];
        } else {
            sound = AudioAssets.SFX_IMPACT_MEATS[ThreadLocalRandom.current().nextInt(
                    AudioAssets.SFX_IMPACT_MEATS.length)];
        }
        var params = new AudioParameters(sound, AudioAssets.AUDIO_RANK_WEAPON_HIT,
                AudioAssets.AUDIO_DISTANCE_WEAPON_HIT, AudioAssets.AUDIO_GAIN_WEAPON_HIT,
                AudioAssets.AUDIO_RADIUS_WEAPON_HIT,
                1f + (pitchRange > 0f ? ThreadLocalRandom.current().nextFloat(-0.5f * pitchRange, 0.5f * pitchRange)
                        : 0f));
        audio.newAudio(targetX, targetY, targetZ, params);
    }

    public void onChickenCluck() {
        audio.newAudio(model.getPositionX(), model.getPositionY(), model.getPositionZ(),
                AudioAssets.CHICKEN_IDLES[ThreadLocalRandom.current().nextInt(AudioAssets.CHICKEN_IDLES.length)]);
        addVisualSound(EmojiType.CHICKEN_CLUCK, DURATION_CHICKEN_CLUCK, AudioAssets.AUDIO_DISTANCE_CHICKEN);
    }

    public void onChickenPeck() {
        audio.newAudio(model.getPositionX(), model.getPositionY(), model.getPositionZ(), AudioAssets.CHICKEN_PECK);
    }

    public void onChickenDeath() {
        audio.newAudio(model.getPositionX(), model.getPositionY(), model.getPositionZ(), AudioAssets.CHICKEN_DEATH);
    }

    public void addVisualSound(EmojiType emoji, float duration, float audioDistance) {
        AssetRegistry.getInstance().getEmojiSprite(emoji)
                .map(sprite -> new VisualSoundAccessory(sprite, duration, audioDistance))
                .ifPresent(accessories::add);
    }

    public void addLightningStrike(float targetX, float targetY, float targetZ) {
        for (Accessory acc : accessories) {
            if (acc instanceof LightningAccessory cloudAcc) {
                cloudAcc.triggerStrike(targetX, targetY, targetZ);
            }
        }
    }

    public void addSonicBlast(float targetX, float targetY, float targetZ, float radius, float duration) {
        for (Accessory acc : accessories) {
            if (acc instanceof SonicBlastAccessory blastAcc) {
                blastAcc.triggerBlast(targetX, targetY, targetZ, radius, duration);
            }
        }
    }
}
