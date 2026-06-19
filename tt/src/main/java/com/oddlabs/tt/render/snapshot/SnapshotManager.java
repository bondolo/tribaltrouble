package com.oddlabs.tt.render.snapshot;

import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.model.AbstractElementNode;
import com.oddlabs.tt.model.Element;
import com.oddlabs.tt.model.ElementLeaf;
import com.oddlabs.tt.model.ElementNode;
import com.oddlabs.tt.model.BoundingBox;
import com.oddlabs.tt.model.behaviour.StunController;
import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.model.Building.BuildStage;
import com.oddlabs.tt.model.Plants;
import com.oddlabs.tt.model.RubberSupply;
import com.oddlabs.tt.model.SceneryModel;
import com.oddlabs.tt.model.SupplyModel;
import com.oddlabs.tt.model.IronSupply;
import com.oddlabs.tt.model.RockSupply;
import com.oddlabs.tt.model.BoundsProvider;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.model.weapon.LightningCloud;
import com.oddlabs.tt.model.weapon.PoisonFog;
import com.oddlabs.tt.model.weapon.Stun;
import com.oddlabs.tt.model.snapshot.WorldSnapshot;
import com.oddlabs.tt.model.snapshot.EntitySnapshot;
import com.oddlabs.tt.model.snapshot.VisualSnapshots.*;
import com.oddlabs.tt.render.VisualModel;
import com.oddlabs.tt.render.IronSupplyVisualAccessory;
import com.oddlabs.tt.render.RockSupplyVisualAccessory;
import com.oddlabs.tt.render.VisualRegistry;
import com.oddlabs.util.Color;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Manages capturing and retrieving the latest visual snapshot of the simulation world.
 */
public final class SnapshotManager {
    private static final float IRON_FALL_DURATION_RATIO = 0.12f;
    private static final Color.Linear IRON_COLOR_FALLING = new Color.Linear(2.0f, 0.5f, 0.05f, 1.0f);
    private static final Color.Linear IRON_COLOR_LANDING = new Color.Linear(2.0f, 1.0f, 0.2f, 1.0f);
    private static final Color.Linear IRON_COLOR_HOT = new Color.Linear(2.0f, 0.2f, 0.1f, 1.0f);
    private static final Color.Linear IRON_COLOR_COOLING = new Color.Standard(0xFF_6B_6B_7A).linear();
    private static final Color.Linear IRON_COLOR_DECAL_COOLED = Color.Linear.BLACK;

    private static final Color.Linear ROCK_COLOR_ERUPTION = new Color.Standard(0xFF_FF_CC_00).linear();
    private static final Color.Linear ROCK_COLOR_COOLING = new Color.Standard(0xFF_FF_BE_94).linear();
    private static final Color.Linear ROCK_COLOR_DECAL_COOLED = new Color.Standard(0.3f, 0.3f).linear();

    private @Nullable WorldSnapshot latestSnapshot;

    public SnapshotManager() {
    }

    public @Nullable WorldSnapshot getLatestSnapshot() {
        return latestSnapshot;
    }

    public void capture(@NonNull World world) {
        List<EntitySnapshot> entities = new ArrayList<>();
        collectElements(world.getElementRoot(), entities, world);
        // Sort by ID to ensure stable diffable ordering
        entities.sort(Comparator.comparingInt(EntitySnapshot::id));
        latestSnapshot = new WorldSnapshot(world.getTick(), entities);
    }

    private void collectElements(AbstractElementNode<?> node, List<EntitySnapshot> dest, World world) {
        var models = node.getModels();
        var model = models.getFirst();
        while (model != null) {
            EntitySnapshot snapshot = captureElement(model, world);
            if (snapshot != null) {
                dest.add(snapshot);
            }
            model = model.getNext();
        }
        switch (node) {
            case ElementNode<?> elementNode -> {
                for (AbstractElementNode<?> child : elementNode.children()) {
                    collectElements(child, dest, world);
                }
            }
            case ElementLeaf<?> _ -> {
            }
        }
    }

    private @Nullable EntitySnapshot captureElement(@NonNull Element<?> element, @NonNull World world) {
        BoundingBox bounds = new BoundingBox();
        bounds.setBounds(element);

        return switch (element) {
            case Unit unit -> {
                boolean isStunned = unit.getCurrentController() instanceof StunController;
                float stunTimeLeft = 0f;
                if (isStunned) {
                    stunTimeLeft = ((StunController) unit.getCurrentController()).getTime();
                }
                var raceType = unit.getOwner().getRaceInfo().getRaceType();
                var unitVisualType = unit.getTemplate().getVisualType();
                var unitVisuals = VisualRegistry.getInstance().getUnitVisuals(raceType, unitVisualType);

                yield new UnitSnapshot(
                        unit.getId(),
                        unit.getPositionX(),
                        unit.getPositionY(),
                        unit.getPositionZ(),
                        unit.getDirectionX(),
                        unit.getDirectionY(),
                        bounds,
                        unitVisualType,
                        raceType,
                        unit.getAnimation(),
                        unit.getAnimationTicks(),
                        unit.getOwner().getColor(),
                        unit.isDead(),
                        unit.isMounted(),
                        unit.isDead() ? 0f : unit.getMountOffset(),
                        unitVisuals.selectionRadius(),
                        unitVisuals.selectionHeight(),
                        unitVisuals.shadowDiameter(),
                        isStunned,
                        stunTimeLeft
                );
            }
            case Building building -> {
                var raceType = building.getOwner().getRaceInfo().getRaceType();
                var buildingType = building.getTemplate().getBuildingType();
                var buildingVisuals = VisualRegistry.getInstance().getBuildingVisuals(raceType, buildingType);

                float selectionRadius;
                float selectionHeight;
                if (building.getBuildStage() == BuildStage.START) {
                    selectionRadius = buildingVisuals.startSelectionRadius();
                    selectionHeight = buildingVisuals.startSelectionHeight();
                } else if (building.getBuildStage() == BuildStage.HALFBUILT) {
                    selectionRadius = buildingVisuals.halfbuiltSelectionRadius();
                    selectionHeight = buildingVisuals.halfbuiltSelectionHeight();
                } else {
                    selectionRadius = buildingVisuals.builtSelectionRadius();
                    selectionHeight = buildingVisuals.builtSelectionHeight();
                }

                float rallyX = 0f;
                float rallyY = 0f;
                float rallyZ = 0f;
                if (building.hasRallyPoint()) {
                    var rp = building.getRallyPoint();
                    rallyX = rp.getPositionX();
                    rallyY = rp.getPositionY();
                    rallyZ = world.getHeightMap().getNearestHeight(rallyX, rallyY);
                    if (rp instanceof Building rallyBuilding) {
                        var offset = rallyBuilding.getTemplate().getRally();
                        rallyX += offset.x();
                        rallyY += offset.y();
                        rallyZ += offset.z();
                    }
                }

                yield new BuildingSnapshot(
                        building.getId(),
                        building.getPositionX(),
                        building.getPositionY(),
                        building.getPositionZ(),
                        building.getDirectionX(),
                        building.getDirectionY(),
                        bounds,
                        buildingType,
                        raceType,
                        building.getBuildStage(),
                        building.getOwner().getColor(),
                        selectionRadius,
                        selectionHeight,
                        buildingVisuals.shadowDiameter(),
                        building.hasRallyPoint(),
                        rallyX,
                        rallyY,
                        rallyZ
                );
            }
            case SupplyModel supply -> {
                boolean isHit = false;
                if (supply instanceof RubberSupply rubberSupply) {
                    isHit = rubberSupply.isHit();
                }

                Color.Linear spawnColorTint = null;
                Color.Linear crackDecalColor = null;
                float crackDecalOpacity = 0.0f;
                float crackDecalDiameter = 0.0f;
                float crackDecalPattern = 0.0f;
                BoundsProvider boundsProvider = supply.getBoundsProvider();

                float shadowDiameter = supply.getShadowDiameter();

                if (supply instanceof IronSupply iron) {
                    var visualModelOpt = iron.getClientState(VisualModel.class);
                    IronSupplyVisualAccessory acc = visualModelOpt
                            .flatMap(vm -> vm.getAccessories().stream()
                                    .filter(IronSupplyVisualAccessory.class::isInstance)
                                    .map(IronSupplyVisualAccessory.class::cast)
                                    .findFirst())
                            .orElse(null);

                    float progress = acc != null ? acc.getSpawnProgress() : 1.0f;
                    boolean isSpawning = acc != null && acc.isSpawning();
                    float size = iron.getSize();
                    int fragmentIndex = iron.getFragmentIndex();

                    if (isSpawning) {
                        if (progress < IRON_FALL_DURATION_RATIO) {
                            spawnColorTint = IRON_COLOR_FALLING;
                            boundsProvider = world.getLandscapeResources().getRockBounds(fragmentIndex);
                            shadowDiameter = 0.0f;
                        } else {
                            float coolProgress = Math.min(1.0f, (progress - IRON_FALL_DURATION_RATIO) / (0.85f
                                    - IRON_FALL_DURATION_RATIO));

                            // Cracks pulse logic (fading out smoothly)
                            float crackDuration = 0.6f;
                            if (progress < IRON_FALL_DURATION_RATIO + crackDuration) {
                                float crackProgress = (progress - IRON_FALL_DURATION_RATIO) / crackDuration;
                                crackDecalOpacity = 1.0f - crackProgress;
                                crackDecalDiameter = size * 2.5f;
                                crackDecalPattern = 10.5f;
                                crackDecalColor = Color.Linear.WHITE.lerp(IRON_COLOR_DECAL_COOLED, crackProgress);
                            }

                            if (coolProgress < 0.3f) {
                                float factor = coolProgress / 0.3f;
                                spawnColorTint = IRON_COLOR_FALLING.lerp(IRON_COLOR_LANDING, factor);
                            } else if (coolProgress < 0.6f) {
                                float factor = (coolProgress - 0.3f) / 0.3f;
                                spawnColorTint = IRON_COLOR_LANDING.lerp(IRON_COLOR_HOT, factor);
                            } else if (coolProgress < 0.8f) {
                                float factor = (coolProgress - 0.6f) / 0.2f;
                                spawnColorTint = IRON_COLOR_HOT.lerp(IRON_COLOR_COOLING, factor);
                            } else if (coolProgress < 0.9f) {
                                float factor = (coolProgress - 0.8f) / 0.1f;
                                spawnColorTint = IRON_COLOR_COOLING.lerp(IRON_COLOR_COOLING.mul(0.35f), factor);
                            } else if (coolProgress < 1.0f) {
                                float factor = (coolProgress - 0.9f) / 0.1f;
                                Color.Linear ironStartTint = IRON_COLOR_COOLING.mul(0.9f);
                                spawnColorTint = ironStartTint.lerp(Color.Linear.WHITE, factor);
                            }

                            if (coolProgress < 1.0f) {
                                boundsProvider = world.getLandscapeResources().getRockBounds(fragmentIndex);
                            }
                        }
                    }
                } else if (supply instanceof RockSupply rock) {
                    var visualModelOpt = rock.getClientState(VisualModel.class);
                    RockSupplyVisualAccessory acc = visualModelOpt
                            .flatMap(vm -> vm.getAccessories().stream()
                                    .filter(RockSupplyVisualAccessory.class::isInstance)
                                    .map(RockSupplyVisualAccessory.class::cast)
                                    .findFirst())
                            .orElse(null);

                    float progress = acc != null ? acc.getSpawnProgress() : 1.0f;
                    boolean isSpawning = acc != null && acc.isSpawning();
                    float size = rock.getSize();

                    if (isSpawning) {
                        if (progress < 0.7f) {
                            shadowDiameter = 0.0f;
                        } else {
                            float coolProgress = (progress - 0.7f) / 0.3f;
                            shadowDiameter = shadowDiameter * coolProgress;
                        }

                        if (progress < 0.3f) {
                            float progressRatio = progress / 0.3f;
                            crackDecalOpacity = progressRatio;
                            crackDecalDiameter = size * 2.0f;
                            crackDecalPattern = 10.0f + 0.5f * progressRatio;
                            crackDecalColor = Color.Linear.WHITE;
                        } else if (progress < 0.7f) {
                            crackDecalOpacity = 1.0f;
                            crackDecalDiameter = size * 2.0f;
                            crackDecalPattern = 10.5f;
                            crackDecalColor = Color.Linear.WHITE;
                            spawnColorTint = ROCK_COLOR_ERUPTION;
                        } else {
                            float coolProgress = (progress - 0.7f) / 0.3f;
                            crackDecalOpacity = 1.0f - coolProgress;
                            crackDecalPattern = 10.5f;
                            crackDecalColor = Color.Linear.WHITE.lerp(ROCK_COLOR_DECAL_COOLED, coolProgress);

                            if (coolProgress < 0.5f) {
                                float factor = coolProgress / 0.5f;
                                spawnColorTint = ROCK_COLOR_ERUPTION.lerp(ROCK_COLOR_COOLING, factor);
                            } else {
                                float factor = (coolProgress - 0.5f) / 0.5f;
                                spawnColorTint = ROCK_COLOR_COOLING.lerp(Color.Linear.WHITE, factor);
                            }
                        }
                    }
                }

                yield new SupplySnapshot(
                        supply.getId(),
                        supply.getPositionX(),
                        supply.getPositionY(),
                        supply.getPositionZ(),
                        supply.getDirectionX(),
                        supply.getDirectionY(),
                        bounds,
                        supply.getAnimation(),
                        supply.getAnimationTicks(),
                        boundsProvider,
                        supply.getSupplyType(),
                        supply.getRotation(),
                        shadowDiameter,
                        crackDecalOpacity,
                        crackDecalDiameter,
                        crackDecalColor,
                        crackDecalPattern,
                        spawnColorTint,
                        isHit
                );
            }
            case Plants plants -> new ScenerySnapshot(
                    plants.getId(),
                    plants.getPositionX(),
                    plants.getPositionY(),
                    plants.getPositionZ(),
                    plants.getDirectionX(),
                    plants.getDirectionY(),
                    bounds,
                    plants.getAnimation(),
                    plants.getAnimationTicks(),
                    plants.getBoundsProvider(),
                    "plants",
                    1.0f
            );
            case SceneryModel scenery -> new ScenerySnapshot(
                    scenery.getId(),
                    scenery.getPositionX(),
                    scenery.getPositionY(),
                    scenery.getPositionZ(),
                    scenery.getDirectionX(),
                    scenery.getDirectionY(),
                    bounds,
                    scenery.getAnimation(),
                    scenery.getAnimationTicks(),
                    scenery.getBoundsProvider(),
                    scenery.getName(),
                    1.0f
            );
            case LightningCloud cloud -> new EffectSnapshot(
                    cloud.getId(),
                    cloud.getPositionX(),
                    cloud.getPositionY(),
                    cloud.getPositionZ(),
                    cloud.getDirectionX(),
                    cloud.getDirectionY(),
                    bounds,
                    EffectSnapshot.EffectType.LIGHTNING_CLOUD,
                    0f, 0f, 0f,
                    Color.Linear.WHITE
            );
            case PoisonFog fog -> new EffectSnapshot(
                    fog.getId(),
                    fog.getPositionX(),
                    fog.getPositionY(),
                    fog.getPositionZ(),
                    fog.getDirectionX(),
                    fog.getDirectionY(),
                    bounds,
                    EffectSnapshot.EffectType.POISON_FOG,
                    0f, 0f, 0f,
                    Color.Linear.WHITE
            );
            case Stun stun -> new EffectSnapshot(
                    stun.getId(),
                    stun.getPositionX(),
                    stun.getPositionY(),
                    stun.getPositionZ(),
                    stun.getDirectionX(),
                    stun.getDirectionY(),
                    bounds,
                    EffectSnapshot.EffectType.STUN,
                    0f, 0f, 0f,
                    Color.Linear.WHITE
            );
            default -> null;
        };
    }
}
