package com.oddlabs.tt.client.render;

import com.oddlabs.tt.base.geom.BoundsProvider;
import com.oddlabs.tt.simulation.model.IronSupply;
import com.oddlabs.tt.simulation.model.RockSupply;
import com.oddlabs.tt.simulation.model.RubberSupply;
import com.oddlabs.tt.simulation.model.SupplyModel;
import com.oddlabs.util.Color;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Calculates visual presentation properties (spawn color tint, heat glow, crack decals, texture overrides,
 * Z offsets, rotations) for supply models during spawn animations.
 */
public final class SupplyVisualState {
    private static final float FALL_DURATION_RATIO = 0.12f;
    private static final Map<SupplyModel, Float> spawnProgressMap = new ConcurrentHashMap<>();

    // Iron Supply Colors
    private static final Color.Linear IRON_COLOR_FALLING = new Color.Linear(2.0f, 0.5f, 0.05f, 1.0f);
    private static final Color.Linear IRON_COLOR_LANDING = new Color.Linear(2.0f, 1.0f, 0.2f, 1.0f);
    private static final Color.Linear IRON_COLOR_HOT = new Color.Linear(2.0f, 0.2f, 0.1f, 1.0f);
    private static final Color.Linear IRON_COLOR_COOLING = new Color.Standard(0xFF_6B_6B_7A).linear();
    private static final Color.Linear IRON_COLOR_DECAL_COOLED = Color.Linear.BLACK;

    // Rock Supply Colors
    private static final Color.Linear ROCK_COLOR_ERUPTION = new Color.Standard(0xFF_FF_CC_00).linear();
    private static final Color.Linear ROCK_COLOR_COOLING = new Color.Standard(0xFF_FF_BE_94).linear();
    private static final Color.Linear ROCK_COLOR_DECAL_COOLED = new Color.Standard(0.3f, 0.3f).linear();

    public record DecalProperties(Color.@Nullable Linear color, float opacity, float diameter, float pattern) {
    }

    public record ShadowProperties(float diameter, float opacity, float verticalCenter) {
    }

    private SupplyVisualState() {
    }

    /** {@return the spawn duration for the given supply model} */
    public static float getSpawnDuration(SupplyModel model) {
        return switch (model.getSupplyType()) {
            case ROCK -> 6.0f;
            case IRON -> 4.5f;
            case RUBBER -> 2.0f;
            case WOOD -> 3.0f;
        };
    }

    public static void registerSpawn(SupplyModel model, float limit) {
        spawnProgressMap.put(model, 0.0f);
    }

    public static void updateSpawn(SupplyModel model, float progress) {
        spawnProgressMap.put(model, progress);
    }

    public static void completeSpawn(SupplyModel model) {
        spawnProgressMap.remove(model);
    }

    public static boolean isSpawning(SupplyModel model) {
        return spawnProgressMap.containsKey(model);
    }

    public static float getSpawnProgress(SupplyModel model) {
        return spawnProgressMap.getOrDefault(model, 1.0f);
    }

    public static float getRotation(SupplyModel model) {
        int hash = (model.getGridX() * 73 + model.getGridY() * 37) & 0x7FFF;
        return (float) ((hash % 360) * Math.PI / 180.0);
    }

    public static float getOffsetZ(SupplyModel model) {
        if (!isSpawning(model)) {
            return 0.0f;
        }
        float progress = getSpawnProgress(model);
        if (model instanceof IronSupply) {
            float fallProgress = Math.min(1.0f, progress / FALL_DURATION_RATIO);
            return (1.0f - fallProgress) * 200.0f;
        } else if (model instanceof RockSupply) {
            if (progress < 0.3f) return -2.0f;
            if (progress < 0.7f) {
                float riseProgress = (progress - 0.3f) / 0.4f;
                return (1.0f - riseProgress) * -2.0f;
            }
        }
        return 0.0f;
    }

    public static ShadowProperties getShadowProperties(SupplyModel model) {
        float ratio = model.getSupplyRatio();
        float diameter = model instanceof RubberSupply ? 1.2f : 7.0f * ratio;
        float opacity = isSpawning(model) ? 0.0f : 0.5f * ratio;
        if (model instanceof RockSupply rock && isSpawning(rock)) {
            float progress = getSpawnProgress(rock);
            if (progress >= 0.7f) {
                float coolProgress = (progress - 0.7f) / 0.3f;
                opacity = 0.5f * ratio * coolProgress;
            }
        }
        return new ShadowProperties(diameter, opacity, 0.3f);
    }

    public static Color.@Nullable Linear getSpawnColorTint(SupplyModel model) {
        if (!isSpawning(model)) {
            return null;
        }

        float progress = getSpawnProgress(model);

        if (model instanceof IronSupply) {
            if (progress < FALL_DURATION_RATIO) {
                return IRON_COLOR_FALLING;
            }
            float coolProgress = Math.min(1.0f, (progress - FALL_DURATION_RATIO) / (0.85f - FALL_DURATION_RATIO));
            if (coolProgress < 0.3f) {
                float factor = coolProgress / 0.3f;
                return IRON_COLOR_FALLING.lerp(IRON_COLOR_LANDING, factor);
            } else if (coolProgress < 0.6f) {
                float factor = (coolProgress - 0.3f) / 0.3f;
                return IRON_COLOR_LANDING.lerp(IRON_COLOR_HOT, factor);
            } else if (coolProgress < 0.8f) {
                float factor = (coolProgress - 0.6f) / 0.2f;
                return IRON_COLOR_HOT.lerp(IRON_COLOR_COOLING, factor);
            } else if (coolProgress < 0.9f) {
                float factor = (coolProgress - 0.8f) / 0.1f;
                return IRON_COLOR_COOLING.lerp(IRON_COLOR_COOLING.mul(0.35f), factor);
            } else if (coolProgress < 1.0f) {
                float factor = (coolProgress - 0.9f) / 0.1f;
                Color.Linear ironStartTint = IRON_COLOR_COOLING.mul(0.9f);
                return ironStartTint.lerp(Color.Linear.WHITE, factor);
            } else {
                return null;
            }
        } else if (model instanceof RockSupply) {
            if (progress < 0.3f) {
                return null;
            } else if (progress < 0.7f) {
                return ROCK_COLOR_ERUPTION;
            } else {
                float coolProgress = (progress - 0.7f) / 0.3f;
                if (coolProgress < 0.5f) {
                    float factor = coolProgress / 0.5f;
                    return ROCK_COLOR_ERUPTION.lerp(ROCK_COLOR_COOLING, factor);
                } else {
                    float factor = (coolProgress - 0.5f) / 0.5f;
                    return ROCK_COLOR_COOLING.lerp(Color.Linear.WHITE, factor);
                }
            }
        }
        return null;
    }

    public static DecalProperties getDecalProperties(SupplyModel model) {
        if (!isSpawning(model)) {
            return new DecalProperties(null, 0.0f, 0.0f, 0.0f);
        }

        float progress = getSpawnProgress(model);

        if (model instanceof IronSupply) {
            float crackDuration = 0.6f;
            if (progress >= FALL_DURATION_RATIO && progress < FALL_DURATION_RATIO + crackDuration) {
                float crackProgress = (progress - FALL_DURATION_RATIO) / crackDuration;
                float opacity = 1.0f - crackProgress;
                float diameter = model.getSize() * 2.5f;
                float pattern = 10.5f;
                Color.Linear color = Color.Linear.WHITE.lerp(IRON_COLOR_DECAL_COOLED, crackProgress);
                return new DecalProperties(color, opacity, diameter, pattern);
            }
        } else if (model instanceof RockSupply) {
            if (progress < 0.3f) {
                float progressRatio = progress / 0.3f;
                return new DecalProperties(Color.Linear.WHITE, progressRatio, model.getSize() * 2.0f, 10.0f + 0.5f
                        * progressRatio);
            } else if (progress < 0.7f) {
                return new DecalProperties(Color.Linear.WHITE, 1.0f, model.getSize() * 2.0f, 10.5f);
            } else {
                float coolProgress = (progress - 0.7f) / 0.3f;
                Color.Linear color = Color.Linear.WHITE.lerp(ROCK_COLOR_DECAL_COOLED, coolProgress);
                return new DecalProperties(color, 1.0f - coolProgress, model.getSize() * 2.0f, 10.5f);
            }
        }
        return new DecalProperties(null, 0.0f, 0.0f, 0.0f);
    }

    public static BoundsProvider getBoundsProvider(SupplyModel model) {
        if (model instanceof IronSupply iron && isSpawning(iron)) {
            float progress = getSpawnProgress(iron);
            float coolProgress = Math.min(1.0f, (progress - FALL_DURATION_RATIO) / (0.85f - FALL_DURATION_RATIO));
            if (coolProgress < 0.9f) {
                return model.getWorld().getLandscapeResources().getRockBounds(iron.getFragmentIndex());
            }
        }
        return model.getBoundsProvider();
    }
}
