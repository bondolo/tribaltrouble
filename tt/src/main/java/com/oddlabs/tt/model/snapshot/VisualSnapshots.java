package com.oddlabs.tt.model.snapshot;

import com.oddlabs.tt.model.Building.BuildStage;
import com.oddlabs.tt.model.BuildingType;
import com.oddlabs.tt.model.Race;
import com.oddlabs.tt.model.SupplyType;
import com.oddlabs.tt.model.UnitVisualType;
import com.oddlabs.tt.model.BoundingBox;
import com.oddlabs.tt.model.BoundsProvider;
import com.oddlabs.util.Color;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;


/**
 * Container class for visual snapshot record definitions.
 */
public final class VisualSnapshots {

    private VisualSnapshots() {
    }

    /**
     * Snapshot representing a unit.
     */
    public record UnitSnapshot(
                               int id,
                               float x,
                               float y,
                               float z,
                               float dirX,
                               float dirY,
                               @NonNull BoundingBox bounds,
                               @NonNull UnitVisualType visualType,
                               @NonNull Race race,
                               int animation,
                               float animationTicks,
                               Color.@NonNull Linear teamColor,
                               boolean isDead,
                               boolean isMounted,
                               float mountOffset,
                               float selectionRadius,
                               float selectionHeight,
                               float shadowDiameter,
                               boolean isStunned,
                               float stunTimeLeft
    ) implements EntitySnapshot {
    }

    /**
     * Snapshot representing a building.
     */
    public record BuildingSnapshot(
                                   int id,
                                   float x,
                                   float y,
                                   float z,
                                   float dirX,
                                   float dirY,
                                   @NonNull BoundingBox bounds,
                                   @NonNull BuildingType buildingType,
                                   @NonNull Race race,
                                   @NonNull BuildStage buildStage,
                                   Color.@NonNull Linear teamColor,
                                   float selectionRadius,
                                   float selectionHeight,
                                   float shadowDiameter,
                                   boolean hasRallyPoint,
                                   float rallyX,
                                   float rallyY,
                                   float rallyZ
    ) implements EntitySnapshot {
        @Override
        public int animation() {
            return 0;
        }

        @Override
        public float animationTicks() {
            return 0f;
        }
    }

    /**
     * Snapshot representing a supply item (iron, rock, wood, rubber).
     */
    public record SupplySnapshot(
                                 int id,
                                 float x,
                                 float y,
                                 float z,
                                 float dirX,
                                 float dirY,
                                 @NonNull BoundingBox bounds,
                                 int animation,
                                 float animationTicks,
                                 @NonNull BoundsProvider boundsProvider,
                                 @NonNull SupplyType supplyType,
                                 float rotation,
                                 float shadowDiameter,
                                 float crackOpacity,
                                 float crackDecalDiameter,
                                 Color.@Nullable Linear crackDecalColor,
                                 float crackDecalPattern,
                                 Color.@Nullable Linear spawnColorTint,
                                 boolean isHit
    ) implements EntitySnapshot {
    }

    /**
     * Snapshot representing scenery (trees, rocks, etc.).
     */
    public record ScenerySnapshot(
                                  int id,
                                  float x,
                                  float y,
                                  float z,
                                  float dirX,
                                  float dirY,
                                  @NonNull BoundingBox bounds,
                                  int animation,
                                  float animationTicks,
                                  @NonNull BoundsProvider boundsProvider,
                                  @NonNull String templateName,
                                  float size
    ) implements EntitySnapshot {
    }

    /**
     * Snapshot representing a transient spell or magic effect.
     */
    public record EffectSnapshot(
                                 int id,
                                 float x,
                                 float y,
                                 float z,
                                 float dirX,
                                 float dirY,
                                 @NonNull BoundingBox bounds,
                                 @NonNull EffectType effectType,
                                 float progressTime,
                                 float maxRadius,
                                 float duration,
                                 Color.@NonNull Linear color
    ) implements EntitySnapshot {
        public enum EffectType {
            LIGHTNING_CLOUD,
            POISON_FOG,
            STUN
        }

        @Override
        public int animation() {
            return 0;
        }

        @Override
        public float animationTicks() {
            return 0f;
        }
    }
}
