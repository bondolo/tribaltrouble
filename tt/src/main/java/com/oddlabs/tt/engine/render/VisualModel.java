package com.oddlabs.tt.engine.render;

import com.oddlabs.tt.engine.resource.AssetRegistry;

import com.oddlabs.tt.audio.AudioImplementation;
import com.oddlabs.tt.audio.AudioParameters;
import com.oddlabs.tt.engine.resource.AudioAssets;
import com.oddlabs.tt.engine.resource.AudioFile;
import com.oddlabs.tt.simulation.model.EmojiType;
import com.oddlabs.tt.simulation.model.Model;
import com.oddlabs.tt.simulation.model.ModelClient;
import com.oddlabs.tt.simulation.model.Race;
import com.oddlabs.tt.simulation.model.SupplyType;
import com.oddlabs.tt.simulation.model.Unit;
import com.oddlabs.tt.simulation.model.UnitVisualType;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Manages the client-side visual state (accessories) and audio dispatch for a simulation model.
 */
public final class VisualModel implements ModelClient {
    private final @NonNull Model model;
    private final @NonNull AudioImplementation audio;
    private final @NonNull List<@NonNull Accessory> accessories = new ArrayList<>();

    public VisualModel(@NonNull Model model, @NonNull AudioImplementation audio) {
        this.model = model;
        this.audio = audio;
    }

    public @NonNull List<@NonNull Accessory> getAccessories() {
        return accessories;
    }

    @Override
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

    @Override
    public void onHarvest(@NonNull SupplyType supplyType) {
        var params = AudioAssets.getHarvestSound(supplyType);
        audio.newAudio(model.getPositionX(), model.getPositionY(), model.getPositionZ(), params);
        addVisualSound(EmojiType.fromSupply(supplyType), ModelClient.DURATION_HARVEST,
                AudioAssets.AUDIO_DISTANCE_HARVEST);
    }

    @Override
    public void onRepair() {
        audio.newAudio(model.getPositionX(), model.getPositionY(), model.getPositionZ(),
                AudioAssets.getHarvestSound(SupplyType.WOOD));
        var selectedEmoji = ThreadLocalRandom.current().nextBoolean() ? EmojiType.REPAIR_SAW : EmojiType.REPAIR_HAMMER;
        addVisualSound(selectedEmoji, ModelClient.DURATION_REPAIR, AudioAssets.AUDIO_DISTANCE_HARVEST);
    }

    @Override
    public void onBuildingHit() {
        audio.newAudio(model.getPositionX(), model.getPositionY(), model.getPositionZ(),
                AudioAssets.BUILDING_HITS[ThreadLocalRandom.current().nextInt(AudioAssets.BUILDING_HITS.length)]);
    }

    @Override
    public void onUnitDeath(@NonNull Race race, @NonNull UnitVisualType unitType, float pitchRange) {
        addVisualSound(EmojiType.GRAVESTONE, ModelClient.DURATION_UNIT_DEATH, AudioAssets.AUDIO_DISTANCE_DEATH);
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

    @Override
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

    @Override
    public void onChickenCluck() {
        audio.newAudio(model.getPositionX(), model.getPositionY(), model.getPositionZ(),
                AudioAssets.CHICKEN_IDLES[ThreadLocalRandom.current().nextInt(AudioAssets.CHICKEN_IDLES.length)]);
        addVisualSound(EmojiType.CHICKEN_CLUCK, ModelClient.DURATION_CHICKEN_CLUCK, AudioAssets.AUDIO_DISTANCE_CHICKEN);
    }

    @Override
    public void onChickenPeck() {
        audio.newAudio(model.getPositionX(), model.getPositionY(), model.getPositionZ(), AudioAssets.CHICKEN_PECK);
    }

    @Override
    public void onChickenDeath() {
        audio.newAudio(model.getPositionX(), model.getPositionY(), model.getPositionZ(), AudioAssets.CHICKEN_DEATH);
    }

    @Override
    public void addVisualSound(@NonNull EmojiType emoji, float duration, float audioDistance) {
        AssetRegistry.getInstance().getEmojiSprite(emoji)
                .map(sprite -> new VisualSoundAccessory(sprite, duration, audioDistance))
                .ifPresent(accessories::add);
    }

    @Override
    public void addLightningStrike(float targetX, float targetY, float targetZ) {
        for (Accessory acc : accessories) {
            if (acc instanceof LightningAccessory cloudAcc) {
                cloudAcc.triggerStrike(targetX, targetY, targetZ);
            }
        }
    }

    @Override
    public void addSonicBlast(float targetX, float targetY, float targetZ, float radius, float duration) {
        for (Accessory acc : accessories) {
            if (acc instanceof SonicBlastAccessory blastAcc) {
                blastAcc.triggerBlast(targetX, targetY, targetZ, radius, duration);
            }
        }
    }
}
