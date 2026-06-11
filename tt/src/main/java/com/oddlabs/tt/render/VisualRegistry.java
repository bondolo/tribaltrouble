package com.oddlabs.tt.render;

import com.oddlabs.tt.model.BuildingType;
import com.oddlabs.tt.model.EmojiType;
import com.oddlabs.tt.model.Race;
import com.oddlabs.tt.model.UnitVisualType;
import com.oddlabs.tt.model.WeaponVisualType;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Client-side registry mapping simulation visual types to graphics-bound SpriteKeys and ShadowListKeys.
 */
public final class VisualRegistry {
    private static final VisualRegistry INSTANCE = new VisualRegistry();

    public static @NonNull VisualRegistry getInstance() {
        return INSTANCE;
    }

    public record BuildingVisuals(
                                  @NonNull SpriteKey start,
                                  @NonNull SpriteKey halfbuilt,
                                  @NonNull SpriteKey built,
                                  @NonNull ShadowListKey shadow
    ) {
    }

    // Indexes: 0 = RACE_NATIVES, 1 = RACE_VIKINGS
    private final EnumMap<UnitVisualType, SpriteKey>[] units = new EnumMap[2];
    private final EnumMap<BuildingType, BuildingVisuals>[] buildings = new EnumMap[2];
    private final EnumMap<WeaponVisualType, SpriteKey>[] weapons = new EnumMap[2];
    private final EnumMap<EmojiType, SpriteKey> emojis = new EnumMap<>(EmojiType.class);
    private final SpriteKey[] rallyPoints = new SpriteKey[2];
    private @NonNull SpriteKey @Nullable [] chickenCluckSprites;
    private @Nullable ShadowListKey defaultUnitShadow;

    @SuppressWarnings("unchecked")
    private VisualRegistry() {
        units[0] = new EnumMap<>(UnitVisualType.class);
        units[1] = new EnumMap<>(UnitVisualType.class);
        buildings[0] = new EnumMap<>(BuildingType.class);
        buildings[1] = new EnumMap<>(BuildingType.class);
        weapons[0] = new EnumMap<>(WeaponVisualType.class);
        weapons[1] = new EnumMap<>(WeaponVisualType.class);
    }

    public void registerChickenCluckSprites(SpriteKey @NonNull [] sprites) {
        this.chickenCluckSprites = sprites.clone();
    }

    public void registerWeapon(@NonNull Race race, @NonNull WeaponVisualType type, @NonNull SpriteKey sprite) {
        weapons[race.getValue()].put(type, sprite);
    }

    public @NonNull SpriteKey getWeaponSprite(@NonNull Race race, @NonNull WeaponVisualType type) {
        SpriteKey sprite = weapons[race.getValue()].get(type);
        if (sprite == null) {
            throw new IllegalStateException("Weapon sprite not registered for race " + race + " and type " + type);
        }
        return sprite;
    }

    public void registerRallyPoint(@NonNull Race race, @NonNull SpriteKey sprite) {
        rallyPoints[race.getValue()] = sprite;
    }

    public @NonNull SpriteKey getRallyPoint(@NonNull Race race) {
        SpriteKey sprite = rallyPoints[race.getValue()];
        if (sprite == null) {
            throw new IllegalStateException("Rally point sprite not registered for race " + race);
        }
        return sprite;
    }

    public void registerUnit(@NonNull Race race, @NonNull UnitVisualType type, @NonNull SpriteKey sprite) {
        units[race.getValue()].put(type, sprite);
    }

    public @NonNull SpriteKey getUnitSprite(@NonNull Race race, @NonNull UnitVisualType type) {
        SpriteKey sprite = units[race.getValue()].get(type);
        if (sprite == null) {
            throw new IllegalStateException("Unit sprite not registered for race " + race + " and type " + type);
        }
        return sprite;
    }

    public void registerBuilding(@NonNull Race race, @NonNull BuildingType type,
            @NonNull BuildingVisuals visuals) {
        buildings[race.getValue()].put(type, visuals);
    }

    public @NonNull BuildingVisuals getBuildingVisuals(@NonNull Race race, @NonNull BuildingType type) {
        BuildingVisuals visuals = buildings[race.getValue()].get(type);
        if (visuals == null) {
            throw new IllegalStateException("Building visuals not registered for race " + race + " and type " + type);
        }
        return visuals;
    }

    public void registerEmoji(@NonNull EmojiType type, @NonNull SpriteKey sprite) {
        emojis.put(type, sprite);
    }

    public @NonNull Optional<SpriteKey> getEmojiSprite(@NonNull EmojiType type) {
        if (type == EmojiType.CHICKEN_CLUCK && chickenCluckSprites != null && chickenCluckSprites.length > 0) {
            return Optional.of(chickenCluckSprites[ThreadLocalRandom.current().nextInt(
                    chickenCluckSprites.length)]);
        }

        return Optional.ofNullable(emojis.get(type));
    }

    public void registerDefaultUnitShadow(@NonNull ShadowListKey shadow) {
        this.defaultUnitShadow = shadow;
    }

    public @NonNull ShadowListKey getDefaultUnitShadow() {
        if (defaultUnitShadow == null) {
            throw new IllegalStateException("Default unit shadow not registered");
        }
        return defaultUnitShadow;
    }
}
