package com.oddlabs.tt.render;

import com.oddlabs.tt.model.BuildingVisualType;
import com.oddlabs.tt.model.UnitVisualType;
import com.oddlabs.tt.model.EmojiType;
import java.util.EnumMap;
import java.util.concurrent.ThreadLocalRandom;

import com.oddlabs.tt.model.WeaponVisualType;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

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
    private final EnumMap<BuildingVisualType, BuildingVisuals>[] buildings = new EnumMap[2];
    private final EnumMap<WeaponVisualType, SpriteKey>[] weapons = new EnumMap[2];
    private final EnumMap<EmojiType, SpriteKey> emojis = new EnumMap<>(EmojiType.class);
    private final SpriteKey[] rallyPoints = new SpriteKey[2];
    private @NonNull SpriteKey @Nullable [] chickenCluckSprites;
    private @Nullable ShadowListKey defaultUnitShadow;

    @SuppressWarnings("unchecked")
    private VisualRegistry() {
        units[0] = new EnumMap<>(UnitVisualType.class);
        units[1] = new EnumMap<>(UnitVisualType.class);
        buildings[0] = new EnumMap<>(BuildingVisualType.class);
        buildings[1] = new EnumMap<>(BuildingVisualType.class);
        weapons[0] = new EnumMap<>(WeaponVisualType.class);
        weapons[1] = new EnumMap<>(WeaponVisualType.class);
    }

    public void registerChickenCluckSprites(SpriteKey @NonNull [] sprites) {
        this.chickenCluckSprites = sprites.clone();
    }

    public void registerWeapon(int race, @NonNull WeaponVisualType type, @NonNull SpriteKey sprite) {
        weapons[race].put(type, sprite);
    }

    public @NonNull SpriteKey getWeaponSprite(int race, @NonNull WeaponVisualType type) {
        SpriteKey sprite = weapons[race].get(type);
        if (sprite == null) {
            throw new IllegalStateException("Weapon sprite not registered for race " + race + " and type " + type);
        }
        return sprite;
    }

    public void registerRallyPoint(int race, @NonNull SpriteKey sprite) {
        rallyPoints[race] = sprite;
    }

    public @NonNull SpriteKey getRallyPoint(int race) {
        SpriteKey sprite = rallyPoints[race];
        if (sprite == null) {
            throw new IllegalStateException("Rally point sprite not registered for race " + race);
        }
        return sprite;
    }

    public void registerUnit(int race, @NonNull UnitVisualType type, @NonNull SpriteKey sprite) {
        units[race].put(type, sprite);
    }

    public @NonNull SpriteKey getUnitSprite(int race, @NonNull UnitVisualType type) {
        SpriteKey sprite = units[race].get(type);
        if (sprite == null) {
            throw new IllegalStateException("Unit sprite not registered for race " + race + " and type " + type);
        }
        return sprite;
    }

    public void registerBuilding(int race, @NonNull BuildingVisualType type, @NonNull BuildingVisuals visuals) {
        buildings[race].put(type, visuals);
    }

    public @NonNull BuildingVisuals getBuildingVisuals(int race, @NonNull BuildingVisualType type) {
        BuildingVisuals visuals = buildings[race].get(type);
        if (visuals == null) {
            throw new IllegalStateException("Building visuals not registered for race " + race + " and type " + type);
        }
        return visuals;
    }

    public void registerEmoji(@NonNull EmojiType type, @NonNull SpriteKey sprite) {
        emojis.put(type, sprite);
    }

    public @NonNull SpriteKey getEmojiSprite(@NonNull EmojiType type) {
        if (type == EmojiType.CHICKEN_CLUCK && chickenCluckSprites != null && chickenCluckSprites.length > 0) {
            return chickenCluckSprites[ThreadLocalRandom.current().nextInt(
                    chickenCluckSprites.length)];
        }
        SpriteKey sprite = emojis.get(type);
        if (sprite == null) {
            throw new IllegalStateException("Emoji sprite not registered for type " + type);
        }
        return sprite;
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
