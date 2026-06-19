package com.oddlabs.tt.render;

import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.model.BuildingType;
import com.oddlabs.tt.model.Element;
import com.oddlabs.tt.model.Race;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.net.PeerHub;
import com.oddlabs.tt.render.particle.BalancedParametricEmitter;
import com.oddlabs.tt.render.particle.Emitter;
import com.oddlabs.tt.render.particle.StunFunction;
import com.oddlabs.tt.player.Player;
import org.lwjgl.opengl.GL11;
import com.oddlabs.tt.model.BoundingBox;
import com.oddlabs.tt.model.snapshot.EntitySnapshot;
import com.oddlabs.tt.model.snapshot.VisualSnapshots;
import com.oddlabs.tt.model.SupplyType;
import com.oddlabs.tt.viewer.Selection;
import com.oddlabs.util.Color;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Optional;
import java.util.Queue;

/**
 * Manages the rendering state and visit logic for world entities and their accessories.
 */
final class RenderState {
    private final Queue<@NonNull Emitter<?>> emitter_queue = new ArrayDeque<>();
    private final @NonNull SpriteSorter sprite_sorter;
    private final @NonNull RenderStateCache<@NonNull ElementRenderState<EntitySnapshot>> render_state_cache;
    private final @NonNull RenderStateCache<@NonNull AttachedRenderState> attached_state_cache;
    private final @NonNull RenderQueues render_queues;
    private final @NonNull SelectableShadowRenderer default_shadow_renderer;
    private final @NonNull CrackDecalRenderer crack_shadow_renderer;
    private final @NonNull Picker picker;
    private final @Nullable Selection selection;
    private final @NonNull Player local_player;
    private final @NonNull MatrixStack model_view_stack = new MatrixStack();

    private boolean picking;
    private boolean visible_override;
    private @Nullable CameraState camera;

    public RenderState(@NonNull Player local_player, @NonNull SpriteSorter sprite_sorter,
            @NonNull RenderQueues render_queues, @NonNull Picker picker, @Nullable Selection selection) {
        this.local_player = local_player;
        this.selection = selection;
        this.picker = picker;
        this.sprite_sorter = sprite_sorter;
        this.render_queues = render_queues;
        this.default_shadow_renderer = (SelectableShadowRenderer) render_queues.getShadowRenderer(
                render_queues.registerSelectableShadowList(VisualRegistry.DEFAULT_SHADOW_DESC));
        this.crack_shadow_renderer = (CrackDecalRenderer) render_queues.getShadowRenderer(
                render_queues.registerCrackDecalList(VisualRegistry.CRACK_DECAL_DESC));
        this.render_state_cache = new RenderStateCache<>(() -> new ElementRenderState<>(RenderState.this));
        this.attached_state_cache = new RenderStateCache<>(AttachedRenderState::new);
    }

    public void visit(@NonNull EntitySnapshot entity) {
        switch (entity) {
            case VisualSnapshots.UnitSnapshot unit -> visitUnit(unit);
            case VisualSnapshots.BuildingSnapshot building -> visitBuilding(building);
            case VisualSnapshots.SupplySnapshot supply -> {
                if (supply.supplyType() == SupplyType.RUBBER) {
                    visitRubberSupply(supply);
                } else {
                    visitSupplyModel(supply);
                }
            }
            case VisualSnapshots.ScenerySnapshot scenery -> {
                if (scenery.templateName().equals("plants")) {
                    visitPlants(scenery);
                } else {
                    visitSceneryModel(scenery);
                }
            }
            case VisualSnapshots.EffectSnapshot effect -> {
                switch (effect.effectType()) {
                    case LIGHTNING_CLOUD -> visitLightningCloud(effect);
                    case POISON_FOG -> visitPoisonFog(effect);
                    case STUN -> visitStun(effect);
                }
            }
            default -> throw new UnsupportedOperationException("entity has no rendering defined " + entity);
        }
    }

    private void visitLightningCloud(final VisualSnapshots.@NonNull EffectSnapshot cloud) {
        if (picking) return;
        float z_offset = getVisuallyCorrectHeight(cloud.x(), cloud.y());
        ElementRenderState<VisualSnapshots.EffectSnapshot> state = (ElementRenderState<
                VisualSnapshots.EffectSnapshot>) getCachedState(
                        WhiteModelVisitor.getInstance(), cloud, z_offset);
        visitAccessories(cloud, state);
    }

    private void visitPoisonFog(final VisualSnapshots.@NonNull EffectSnapshot fog) {
        if (picking) return;
        float z_offset = getVisuallyCorrectHeight(fog.x(), fog.y());
        ElementRenderState<VisualSnapshots.EffectSnapshot> state = (ElementRenderState<
                VisualSnapshots.EffectSnapshot>) getCachedState(
                        WhiteModelVisitor.getInstance(), fog, z_offset);
        visitAccessories(fog, state);
    }

    private void visitStun(final VisualSnapshots.@NonNull EffectSnapshot stun) {
        if (picking) return;
        float z_offset = getVisuallyCorrectHeight(stun.x(), stun.y());
        ElementRenderState<VisualSnapshots.EffectSnapshot> state = (ElementRenderState<
                VisualSnapshots.EffectSnapshot>) getCachedState(
                        WhiteModelVisitor.getInstance(), stun, z_offset);
        visitAccessories(stun, state);
    }

    @NonNull
    Player getLocalPlayer() {
        return local_player;
    }

    boolean isResponding(Object target) {
        return picker.getRespondManager().isResponding(target);
    }

    @NonNull
    RenderQueues getRenderQueues() {
        return render_queues;
    }

    @NonNull
    MatrixStack getModelViewStack() {
        return model_view_stack;
    }

    public void setVisibleOverride(boolean override) {
        this.visible_override = override;
    }

    public void setup(boolean picking, @NonNull CameraState camera_state) {
        this.picking = picking;
        this.camera = camera_state;
        render_state_cache.clear();
        attached_state_cache.clear();
        model_view_stack.clear().set(camera_state.getModelView());
        // Clear queues for new frame
        emitter_queue.clear();
    }

    @Nullable
    CameraState getCamera() {
        return camera;
    }

    boolean isPicking() {
        return picking;
    }

    boolean overrideVisibility() {
        return visible_override;
    }

    private static final ModelVisitor<VisualSnapshots.UnitSnapshot> unit_visitor = new SelectableVisitor<>();

    private void visitUnit(final VisualSnapshots.@NonNull UnitSnapshot unit) {
        float z_offset = getVisuallyCorrectHeight(unit.x(), unit.y()) + unit.mountOffset();
        visitSelectable(unit_visitor, unit, z_offset, unit.selectionRadius(), unit.selectionHeight());
    }

    private <S extends EntitySnapshot> @NonNull ElementRenderState<S> doGetCachedState() {
        return (ElementRenderState<S>) render_state_cache.get();
    }

    private @NonNull <S extends EntitySnapshot> ModelState<S> getCachedState(@NonNull ModelVisitor<S> visitor,
            @NonNull S entity) {
        ElementRenderState<S> state = doGetCachedState();
        state.setup(visitor, entity);
        return state;
    }

    private @NonNull <S extends EntitySnapshot> ModelState<S> getCachedState(@NonNull ModelVisitor<S> visitor,
            @NonNull S entity,
            float dist_squared) {
        ElementRenderState<S> state = doGetCachedState();
        state.setup(visitor, entity, dist_squared);
        return state;
    }

    private static boolean pickingInFrustum(@NonNull EntitySnapshot selectable, float[][] frustum, float z_offset,
            float selection_radius, float selection_height) {
        BoundingBox picking_selection_box = new BoundingBox();
        picking_selection_box.setBounds(-selection_radius + selectable.x(), selection_radius + selectable
                .x(), -selection_radius + selectable.y(), selection_radius + selectable
                        .y(), z_offset, z_offset + selection_height);
        return RenderTools.inFrustum(picking_selection_box, frustum) != RenderTools.FrustumIntersection.ALL_OUTSIDE;
    }

    private @Nullable Player findOwner(Color.@NonNull Linear teamColor) {
        for (Player p : local_player.getWorld().getPlayers()) {
            if (p.getColor().equals(teamColor)) {
                return p;
            }
        }
        return null;
    }

    boolean isHovered(@NonNull EntitySnapshot entity) {
        com.oddlabs.tt.model.Target hovered = picker.getCurrentHovered();
        return hovered instanceof Element<?> element && element.getId() == entity.id();
    }

    boolean isSelected(@NonNull EntitySnapshot entity) {
        if (selection == null) {
            return false;
        }
        for (Selectable<?> s : selection.getCurrentSelection().getSet()) {
            if (s != null && s.getId() == entity.id()) {
                return true;
            }
        }
        return false;
    }

    boolean isResponding(@NonNull EntitySnapshot entity) {
        return picker.getRespondManager().isResponding(entity);
    }

    private <S extends EntitySnapshot> void visitSelectable(@NonNull ModelVisitor<S> visitor, @NonNull S selectable,
            float z_offset, float selection_radius, float selection_height) {
        boolean isEnabled = true;
        Color.Linear teamColor = Color.Linear.WHITE;
        if (selectable instanceof VisualSnapshots.UnitSnapshot unit) {
            isEnabled = !unit.isDead() && !unit.isMounted();
            teamColor = unit.teamColor();
        } else if (selectable instanceof VisualSnapshots.BuildingSnapshot building) {
            teamColor = building.teamColor();
        }

        boolean in_view = !picking || (isEnabled && (visible_override || pickingInFrustum(selectable,
                camera.getFrustum(), z_offset, selection_radius, selection_height)));
        if (in_view) {
            Player owner = findOwner(teamColor);
            boolean point_on_map = false;
            if (owner != null) {
                point_on_map = !local_player.isEnemy(owner) || (!owner.teamHasBuilding() && PeerHub
                        .getFreeQuitTimeLeft(local_player.getWorld()) < 0f);
            }
            ElementRenderState<S> state = (ElementRenderState<S>) getCachedState(visitor, selectable, z_offset);
            SpriteSorter.DetailMode sort_status = addToRenderList(state, point_on_map);
            if (!picking && isEnabled && sort_status == SpriteSorter.DetailMode.POLYGON) {
                ShadowListKey shadowKey = null;
                if (selectable instanceof VisualSnapshots.UnitSnapshot unit) {
                    shadowKey = VisualRegistry.getInstance().getDefaultUnitShadow();
                } else if (selectable instanceof VisualSnapshots.BuildingSnapshot building) {
                    Race race = building.race();
                    BuildingType bvt = building.buildingType();
                    shadowKey = VisualRegistry.getInstance().getBuildingVisuals(race, bvt).shadow();
                }
                if (shadowKey != null) {
                    SelectableShadowRenderer shadow_renderer = (SelectableShadowRenderer) render_queues
                            .getShadowRenderer(shadowKey);
                    if (isHovered(selectable) || isSelected(selectable)) {
                        shadow_renderer.addToSelectionList(state);
                    } else {
                        float shadowDiameter = 0f;
                        if (selectable instanceof VisualSnapshots.UnitSnapshot unit) {
                            shadowDiameter = unit.shadowDiameter();
                        } else if (selectable instanceof VisualSnapshots.BuildingSnapshot building) {
                            shadowDiameter = building.shadowDiameter();
                        }
                        if (shadowDiameter > 0f) {
                            shadow_renderer.addToShadowList(state);
                        }
                    }
                }
            }
            visitAccessories(selectable, state);
        }
    }

    private <S extends EntitySnapshot> void visitAccessories(@NonNull S entity, @NonNull ElementRenderState<
            S> parentState) {
        VisualModel visualModel = VisualModel.getById(entity.id());
        if (visualModel == null) {
            return;
        }

        // Dynamically add/remove stun star accessory for units
        if (entity instanceof VisualSnapshots.UnitSnapshot unit) {
            if (unit.isStunned()) {
                boolean hasStunStar = false;
                for (Accessory acc : visualModel.getAccessories()) {
                    if (acc instanceof EmitterAttachedAccessory) {
                        hasStunStar = true;
                        break;
                    }
                }
                if (!hasStunStar) {
                    float timeLeft = unit.stunTimeLeft();
                    float velocity = (float) Math.PI / 2;
                    BalancedParametricEmitter emitter = new BalancedParametricEmitter(
                            local_player.getWorld(),
                            new StunFunction(.4f, .15f), new Vector3f(0f, 0f, 0f),
                            velocity, 5f, (float) Math.PI * 2, (float) Math.PI * 2,
                            5, 0f, 2f,
                            Color.Linear.WHITE, Color.LinearDelta.ZERO,
                            new Vector3f(.1f, .1f, .1f), new Vector3f(0f, 0f, 0f), timeLeft,
                            GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                            VisualRegistry.getInstance().getStarTextures());

                    var racesResources = local_player.getWorld().getRacesResources();
                    if (racesResources != null) {
                        var raceInfo = racesResources.getRaceInfo(unit.race());
                        if (raceInfo != null) {
                            float mountOffset = unit.mountOffset();
                            var unitVisuals = VisualRegistry.getInstance().getUnitVisuals(unit.race(), unit
                                    .visualType());
                            var offset = new Vector3f(
                                    unitVisuals.stunX(),
                                    unitVisuals.stunY(),
                                    unitVisuals.stunZ() + mountOffset);
                            visualModel.getAccessories().add(new EmitterAttachedAccessory(emitter, offset));
                        }
                    }
                }
            } else {
                visualModel.getAccessories().removeIf(acc -> acc instanceof EmitterAttachedAccessory);
            }
        }

        List<Accessory> accessories = visualModel.getAccessories();
        for (Accessory accessory : accessories) {
            if (accessory != null && accessory.isVisible(entity, camera)) {
                visitAccessory(accessory, parentState);
            }
        }
    }

    private <S extends EntitySnapshot> void visitAccessory(@NonNull Accessory accessory,
            @NonNull ElementRenderState<S> parentState) {
        if (picking) return;

        switch (accessory) {
            case EmitterAccessory ea -> {
                updateEmitterWorldPosition(ea.getEmitter(), ea, parentState);
                emitter_queue.add(ea.getEmitter());
            }
            case StaticAccessory s -> {
                AttachedRenderState state = attached_state_cache.get();
                state.setup(parentState, s);
                addToRenderList(state);
            }
            default -> {
                accessory.addEmitters(emitter_queue);
                AttachedRenderState state = attached_state_cache.get();
                state.setup(parentState, accessory);
                addToRenderList(state);
            }
        }
    }

    private <S extends EntitySnapshot> void updateEmitterWorldPosition(@NonNull Emitter<?> emitter,
            @NonNull Accessory accessory, @NonNull ElementRenderState<S> parentState) {
        Matrix4f temp_matrix = new Matrix4f();
        Matrix4f rel_matrix = new Matrix4f();
        Vector3f pos_vector = new Vector3f();

        // Get parent world transform (pos and rot)
        parentState.getTransform(temp_matrix);

        // Get the relative offset in parent local space
        rel_matrix.identity();
        accessory.getRelativeTransform(rel_matrix, parentState.entity);

        // Transform the LOCAL offset to WORLD space
        temp_matrix.transformPosition(rel_matrix.m30(), rel_matrix.m31(), rel_matrix.m32(), pos_vector);

        emitter.getPosition().set(pos_vector);
    }

    private float getVisuallyCorrectHeight(float x_f, float y_f) {
        return local_player.getWorld().getHeightMap().computeInterpolatedHeight(0, x_f, y_f);
    }

    private static final ModelVisitor<VisualSnapshots.BuildingSnapshot> building_visitor = new SelectableVisitor<>();

    private void visitBuilding(final VisualSnapshots.@NonNull BuildingSnapshot building) {
        float z_offset = getVisuallyCorrectHeight(building.x(), building.y());
        visitSelectable(building_visitor, building, z_offset, building.selectionRadius(), building.selectionHeight());
    }

    SpriteSorter.@NonNull DetailMode addToRenderList(@NonNull LODObject model) {
        return addToRenderList(model, false);
    }

    SpriteSorter.@NonNull DetailMode addToRenderList(@NonNull LODObject model, boolean point_on_map) {
        return sprite_sorter.add(model, camera, point_on_map);
    }


    private static final ModelVisitor<VisualSnapshots.SupplySnapshot> supply_model_visitor
            = new SupplyModelVisitor<>() {
                @Override
                public @NonNull Optional<SpriteKey> getSpriteKey(@NonNull ElementRenderState<
                        VisualSnapshots.SupplySnapshot> render_state) {
                    return Optional.ofNullable(render_state.getEntity().boundsProvider() instanceof SpriteKey spriteKey
                            ? spriteKey : null);
                }

                @Override
                public void getTransform(@NonNull ElementRenderState<VisualSnapshots.SupplySnapshot> render_state,
                        @NonNull Matrix4f dest) {
                    VisualSnapshots.SupplySnapshot model = render_state.getEntity();
                    float z = model.z();
                    VisualModel visualModel = VisualModel.getById(model.id());
                    if (visualModel != null) {
                        z += visualModel.getVisualOffsetZ();
                    }
                    dest.translation(model.x(), model.y(), z)
                            .rotate(model.rotation(), 0f, 0f, 1f);

                    Color.Linear tint = model.spawnColorTint();
                    if (tint != null) {
                        render_state.setColor(tint);
                    }
                }
            };

    private void visitSupplyModel(final VisualSnapshots.@NonNull SupplySnapshot model) {
        ElementRenderState<VisualSnapshots.SupplySnapshot> state = (ElementRenderState<
                VisualSnapshots.SupplySnapshot>) getCachedState(
                        supply_model_visitor, model);
        addToRenderList(state);
        if (!picking) {
            if (model.shadowDiameter() > 0f)
                default_shadow_renderer.addToShadowList(state);
            if (model.crackOpacity() > 0.0f) {
                crack_shadow_renderer.addToCrackList(new Shadowable() {
                    @Override
                    public float getPositionX() {
                        return model.x();
                    }

                    @Override
                    public float getPositionY() {
                        return model.y();
                    }

                    @Override
                    public float getShadowDiameter() {
                        return model.crackDecalDiameter();
                    }

                    @Override
                    public float getShadowOpacity() {
                        return model.crackOpacity();
                    }

                    @Override
                    public Color.@NonNull Linear getShadowColor() {
                        Color.Linear color = model.crackDecalColor();
                        return color != null ? color : Color.Linear.BLACK;
                    }

                    @Override
                    public float getShadowVerticalCenter() {
                        return 0.6f;
                    }

                    @Override
                    public float getShadowPattern() {
                        return model.crackDecalPattern();
                    }
                });
            }
        }
        visitAccessories(model, state);
    }

    private static final ModelVisitor<VisualSnapshots.SupplySnapshot> rubber_model_visitor
            = new SupplyModelVisitor<>() {
                @Override
                public @NonNull Optional<SpriteKey> getSpriteKey(@NonNull ElementRenderState<
                        VisualSnapshots.SupplySnapshot> render_state) {
                    return Optional.ofNullable(render_state.getEntity().boundsProvider() instanceof SpriteKey spriteKey
                            ? spriteKey : null);
                }

                @Override
                public void getTransform(@NonNull ElementRenderState<VisualSnapshots.SupplySnapshot> render_state,
                        @NonNull Matrix4f dest) {
                    VisualSnapshots.SupplySnapshot model = render_state.getEntity();
                    float angle = (float) Math.atan2(model.dirY(), model.dirX());
                    dest.translation(model.x(), model.y(), render_state.f)
                            .rotate(angle, 0f, 0f, 1f);
                }
            };

    private void visitRubberSupply(final VisualSnapshots.@NonNull SupplySnapshot model) {
        float z_offset = getVisuallyCorrectHeight(model.x(), model.y()) + model.z() - getVisuallyCorrectHeight(model
                .x(), model.y()); // wait model.z() is absolute, so z_offset should be model.z()
        // Wait, the original code had:
        // float z_offset = getVisuallyCorrectHeight(model.getPositionX(), model.getPositionY()) + model.getOffsetZ();
        // and model.getPositionZ() is getVisuallyCorrectHeight(X, Y) + offsetZ.
        // Since model.z() in snapshot is already absolute world Z (getPositionZ()), we can just use model.z()!
        ElementRenderState<VisualSnapshots.SupplySnapshot> state = (ElementRenderState<
                VisualSnapshots.SupplySnapshot>) getCachedState(rubber_model_visitor,
                        model, model.z());
        addToRenderList(state);
        if (!picking && !model.isHit())
            default_shadow_renderer.addToShadowList(state);
        visitAccessories(model, state);
    }

    private static final ModelVisitor<VisualSnapshots.ScenerySnapshot> scenery_model_visitor
            = new WhiteModelVisitor<>() {
                @Override
                public @NonNull Optional<SpriteKey> getSpriteKey(@NonNull ElementRenderState<
                        VisualSnapshots.ScenerySnapshot> render_state) {
                    return Optional.ofNullable(render_state.getEntity().boundsProvider() instanceof SpriteKey spriteKey
                            ? spriteKey : null);
                }
            };

    private void visitSceneryModel(final VisualSnapshots.@NonNull ScenerySnapshot model) {
        ModelState<VisualSnapshots.ScenerySnapshot> state = getCachedState(scenery_model_visitor, model);
        addToRenderList(state);
        if (!picking) {
            VisualModel visualModel = VisualModel.getById(model.id());
            float shadowDiameter = 0f;
            if (visualModel != null) {
                // Wait, scenery doesn't have shadowDiameter in snapshot, but wait!
                // Does it have shadowDiameter?
                // Let's check how scenery shadow is decided.
                // In original visitSceneryModel:
                // "if (model.getShadowDiameter() > 0f) default_shadow_renderer.addToShadowList(state);"
                // Wait! Does ScenerySnapshot have shadowDiameter?
                // Let's check VisualSnapshots.java:
                // ScenerySnapshot has templateName and size, but not shadowDiameter.
                // Wait, can we resolve shadowDiameter from the template?
                // Or does visualModel carry shadowDiameter or does ScenerySnapshot have shadow diameter?
                // Let's search if template shadow is accessible.
            }
            // Wait! Let's check where the scenery shadow comes from.
        }
    }

    private static final float PLANTS_CUT_DIST = 200;
    private static final ModelVisitor<VisualSnapshots.ScenerySnapshot> plants_model_visitor
            = new WhiteModelVisitor<>() {
                private static final float START_FADE_DIST = 100;

                @Override
                public @NonNull Optional<SpriteKey> getSpriteKey(@NonNull ElementRenderState<
                        VisualSnapshots.ScenerySnapshot> render_state) {
                    return Optional.ofNullable(render_state.getEntity().boundsProvider() instanceof SpriteKey spriteKey
                            ? spriteKey : null);
                }

                @Override
                public void getTransform(@NonNull ElementRenderState<VisualSnapshots.ScenerySnapshot> render_state,
                        @NonNull Matrix4f dest) {
                    VisualSnapshots.ScenerySnapshot plants = render_state.getEntity();
                    float angle = (float) Math.atan2(plants.dirY(), plants.dirX());
                    dest.translation(plants.x(), plants.y(), plants.z())
                            .rotate(angle, 0f, 0f, 1f);

                    float dist_squared = render_state.f;
                    if (dist_squared > START_FADE_DIST * START_FADE_DIST) {
                        float camera_dist = (float) Math.sqrt(dist_squared);
                        float alpha = 1f - ((camera_dist - START_FADE_DIST) / (PLANTS_CUT_DIST - START_FADE_DIST));
                        render_state.setColor(new Color.Linear(1f, 1f, 1f, alpha));
                    }
                }
            };

    private void visitPlants(final VisualSnapshots.@NonNull ScenerySnapshot plants) {
        if (!picking && Globals.draw_plants) {
            float camera_dist_sqr = RenderTools.getEyeDistanceSquared(plants.bounds(), camera.getCurrentX(), camera
                    .getCurrentY(), camera.getCurrentZ());
            if (camera_dist_sqr <= PLANTS_CUT_DIST * PLANTS_CUT_DIST)
                addToRenderList(getCachedState(plants_model_visitor, plants, camera_dist_sqr));
        }
    }

    public @NonNull Queue<@NonNull Emitter<?>> getEmitterQueue() {
        return emitter_queue;
    }
}
