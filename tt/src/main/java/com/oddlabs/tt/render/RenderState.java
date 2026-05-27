package com.oddlabs.tt.render;

import com.oddlabs.tt.animation.Animated;
import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.landscape.LandscapeTargetRespond;
import com.oddlabs.tt.model.Abilities;
import com.oddlabs.tt.model.AccessorizableModel;
import com.oddlabs.tt.model.Accessory;
import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.model.BuildingDamagedAccessory;
import com.oddlabs.tt.model.BuildingProductionAccessory;
import com.oddlabs.tt.model.Element;
import com.oddlabs.tt.model.EmitterAttachedAccessory;
import com.oddlabs.tt.model.Model;
import com.oddlabs.tt.model.Plants;
import com.oddlabs.tt.model.PointEmitterModel;
import com.oddlabs.tt.model.RacesResources;
import com.oddlabs.tt.model.RubberSupply;
import com.oddlabs.tt.model.SceneryModel;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.StaticAccessory;
import com.oddlabs.tt.model.SupplyModel;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.model.UnitSupplyContainer;
import com.oddlabs.tt.model.weapon.DirectedThrowingWeapon;
import com.oddlabs.tt.model.weapon.RotatingThrowingWeapon;
import com.oddlabs.tt.model.weapon.SonicBlast;
import com.oddlabs.tt.net.PeerHub;
import com.oddlabs.tt.particle.Emitter;
import com.oddlabs.tt.particle.Lightning;
import com.oddlabs.tt.particle.SonicBlastEffect;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.procedural.GeneratorRing;
import com.oddlabs.tt.util.BoundingBox;
import com.oddlabs.tt.viewer.Selection;
import com.oddlabs.util.Color;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

/**
 * Manages the rendering state and visit logic for world entities and their accessories.
 */
final class RenderState {
    private final Queue<@NonNull Emitter<?>> emitter_queue = new ArrayDeque<>();
    private final Queue<@NonNull Lightning> lightning_queue = new ArrayDeque<>();
    private final Queue<@NonNull SonicBlastEffect> sonic_blast_queue = new ArrayDeque<>();
    private final @NonNull SpriteSorter sprite_sorter;
    private final @NonNull RenderStateCache<ElementRenderState<Model>> render_state_cache;
    private final @NonNull RenderStateCache<AttachedRenderState> attached_state_cache;
    private final @NonNull RenderQueues render_queues;
    private final @NonNull TargetRespondRenderer target_respond_renderer;
    private final @NonNull SelectableShadowRenderer default_shadow_renderer;
    private final @NonNull Picker picker;
    private final @Nullable Selection selection;
    private final @NonNull Player local_player;
    private final @NonNull MatrixStack model_view_stack = new MatrixStack();
    private final Matrix4f temp_matrix = new Matrix4f();
    private final Matrix4f rel_matrix = new Matrix4f();
    private final Vector3f pos_vector = new Vector3f();

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
        ShadowListKey key = render_queues.registerRespondRenderer(new GeneratorRing(DecalRenderer.HALO_LUT_RESOLUTION,
                new float[][]{{0.40f, 0f}, {0.41f, 1f}, {0.48f, 1f}, {0.49f, 0f}}));
        this.target_respond_renderer = (TargetRespondRenderer) render_queues.getShadowRenderer(key);
        this.default_shadow_renderer = (SelectableShadowRenderer) render_queues.getShadowRenderer(
                render_queues.registerSelectableShadowList(RacesResources.DEFAULT_SHADOW_DESC));
        this.render_state_cache = new RenderStateCache<>(() -> new ElementRenderState<>(RenderState.this));
        this.attached_state_cache = new RenderStateCache<>(AttachedRenderState::new);
    }

    public void visit(@NonNull Element<?> element) {
        if (!element.isRegistered() || element.isFinished()) {
            element.remove();
            if (element instanceof Animated animated) {
                local_player.getWorld().getAnimationManagerGameTime().removeAnimation(animated);
            }
            return;
        }
        switch (element) {
            case Unit unit -> visitUnit(unit);
            case Building building -> visitBuilding(building);
            case Lightning lightning -> visitLightning(lightning);
            case SonicBlastEffect effect -> visitSonicBlastEffect(effect);
            case LandscapeTargetRespond respond -> visitRespond(respond);
            case RubberSupply model -> visitRubberSupply(model);
            case SupplyModel model -> visitSupplyModel(model);
            case Plants plants -> visitPlants(plants);
            case SceneryModel model -> visitSceneryModel(model);
            case DirectedThrowingWeapon weapon -> addToRenderList(getCachedState(directed_weapon_model_visitor,
                    weapon));
            case RotatingThrowingWeapon weapon -> addToRenderList(getCachedState(rotating_weapon_model_visitor,
                    weapon));
            case AccessorizableModel model -> visitAccessorizableModel(model);
            default -> throw new UnsupportedOperationException("element has no rendering defined " + element);
        }
    }

    private void visitAccessorizableModel(final @NonNull AccessorizableModel model) {
        if (picking) return;
        switch (model) {
            case PointEmitterModel emitterModel -> {
                emitter_queue.add(emitterModel.getEmitter());
                float z_offset = getVisuallyCorrectHeight(emitterModel.getPositionX(), emitterModel.getPositionY());
                ElementRenderState<PointEmitterModel> state = (ElementRenderState<PointEmitterModel>) getCachedState(
                        WhiteModelVisitor.getInstance(), emitterModel, z_offset);
                visitAccessories(emitterModel, state);
            }
            case SonicBlast blast -> {
                // SonicBlast logic itself is a model, but its visuals are handled by the sonic_blast_queue
                sonic_blast_queue.add(blast.getSonicBlastEffect());
                float z_offset = getVisuallyCorrectHeight(blast.getPositionX(), blast.getPositionY());
                ElementRenderState<SonicBlast> state = (ElementRenderState<SonicBlast>) getCachedState(
                        WhiteModelVisitor.getInstance(), blast, z_offset);
                visitAccessories(blast, state);
            }
            default -> {
                // If it's a generic accessorizable model, we still want to visit its accessories
                float z_offset = getVisuallyCorrectHeight(model.getPositionX(), model.getPositionY());
                ElementRenderState<AccessorizableModel> state = (ElementRenderState<AccessorizableModel>) getCachedState(
                        WhiteModelVisitor.getInstance(), model, z_offset);
                visitAccessories(model, state);
            }
        }
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
        lightning_queue.clear();
        sonic_blast_queue.clear();
    }

    CameraState getCamera() {
        return camera;
    }

    boolean isPicking() {
        return picking;
    }

    boolean overrideVisibility() {
        return visible_override;
    }

    private static final ModelVisitor<Unit> unit_visitor = new SelectableVisitor<>() {
        @Override
        public void markDetailPolygon(@NonNull ElementRenderState<Unit> render_state, @NonNull PolyDetail detail) {
            Unit unit = render_state.model;
            super.markDetailPolygon(render_state, detail);
            UnitSupplyContainer supply_container = unit.getSupplyContainer();
            if (!render_state.render_state.isPicking() && unit.getAbilities().hasAbilities(Abilities.BUILD)
                    && supply_container.getSupplyType() != null) {
                if (supply_container.getNumSupplies() > 0) {
                    SpriteRenderer supply_sprite = render_state.getRenderer(supply_container.getSupplySpriteRenderer(
                            supply_container.getSupplyType()));
                    supply_sprite.addToRenderList(detail, render_state, false);
                }
            }
        }
    };

    private void visitUnit(final @NonNull Unit unit) {
        float z_offset = getVisuallyCorrectHeight(unit.getPositionX(), unit.getPositionY()) + unit.getOffsetZ();
        visitSelectable(unit_visitor, unit, z_offset, unit.getTemplate().getSelectionRadius(), unit.getTemplate()
                .getSelectionHeight());
    }

    private <M extends Model> @NonNull ElementRenderState<M> doGetCachedState() {
        return (ElementRenderState<M>) render_state_cache.get();
    }

    private @NonNull <M extends Model> ModelState<M> getCachedState(@NonNull ModelVisitor<M> visitor,
            @NonNull M model) {
        ElementRenderState<M> state = doGetCachedState();
        state.setup(visitor, model);
        return state;
    }

    private @NonNull <M extends Model> ModelState<M> getCachedState(@NonNull ModelVisitor<M> visitor, @NonNull M model,
            float dist_squared) {
        ElementRenderState<M> state = doGetCachedState();
        state.setup(visitor, model, dist_squared);
        return state;
    }

    private static final BoundingBox picking_selection_box = new BoundingBox();

    private static boolean pickingInFrustum(@NonNull Selectable<?> selectable, float[][] frustum, float z_offset,
            float selection_radius, float selection_height) {
        picking_selection_box.setBounds(-selection_radius + selectable.getPositionX(), selection_radius + selectable
                .getPositionX(), -selection_radius + selectable.getPositionY(), selection_radius + selectable
                        .getPositionY(), z_offset, z_offset + selection_height);
        return RenderTools.inFrustum(picking_selection_box, frustum) != RenderTools.FrustumIntersection.ALL_OUTSIDE;
    }

    boolean isHovered(Selectable<?> selectable) {
        return selectable == picker.getCurrentHovered();
    }

    boolean isSelected(@NonNull Selectable<?> selectable) {
        return selection != null && selection.getCurrentSelection().contains(selectable);
    }

    private <S extends Selectable<?>> void visitSelectable(@NonNull ModelVisitor<S> visitor, @NonNull S selectable,
            float z_offset, float selection_radius, float selection_height) {
        boolean in_view = !picking || (selectable.isEnabled() && (visible_override || pickingInFrustum(selectable,
                camera.getFrustum(), z_offset, selection_radius, selection_height)));
        if (in_view) {
            Player owner = selectable.getOwnerNoCheck();
            boolean point_on_map = !local_player.isEnemy(owner) || (!owner.teamHasBuilding() && PeerHub
                    .getFreeQuitTimeLeft(local_player.getWorld()) < 0f);
            ElementRenderState<S> state = (ElementRenderState<S>) getCachedState(visitor, selectable, z_offset);
            SpriteSorter.DetailMode sort_status = addToRenderList(state, point_on_map);
            if (!picking && selectable.isEnabled() && sort_status == SpriteSorter.DetailMode.POLYGON) {
                SelectableShadowRenderer shadow_renderer = (SelectableShadowRenderer) render_queues.getShadowRenderer(
                        selectable.getTemplate().getSelectableShadowRenderer());
                if (isHovered(selectable) || isSelected(selectable)) {
                    shadow_renderer.addToSelectionList(state);
                } else {
                    shadow_renderer.addToShadowList(state);
                }
            }
            visitAccessories(selectable, state);
        }
    }

    private <M extends AccessorizableModel> void visitAccessories(@NonNull M model, @NonNull ElementRenderState<
            M> parentState) {
        List<Accessory> accessories = model.getAttachedAccessories();
        for (int i = 0; i < accessories.size(); i++) {
            Accessory accessory = accessories.get(i);
            if (accessory != null && accessory.isVisible(model, camera)) {
                visitAccessory(accessory, parentState);
            }
        }
    }

    private <M extends AccessorizableModel> void visitAccessory(@NonNull Accessory accessory,
            @NonNull ElementRenderState<M> parentState) {
        if (picking) return;

        switch (accessory) {
            case EmitterAttachedAccessory e -> {
                updateEmitterWorldPosition(e.getEmitter(), e, parentState);
                emitter_queue.add(e.getEmitter());
            }
            case BuildingProductionAccessory bpa -> {
                updateEmitterWorldPosition(bpa.getEmitter(), bpa, parentState);
                emitter_queue.add(bpa.getEmitter());
            }
            case BuildingDamagedAccessory bda -> {
                updateEmitterWorldPosition(bda.getEmitter(), bda, parentState);
                emitter_queue.add(bda.getEmitter());
            }
            case StaticAccessory s -> {
                AttachedRenderState state = attached_state_cache.get();
                state.setup(parentState, s);
                addToRenderList(state);
            }
            default -> {
                // Handle generic animated accessories that might just be sprites
                AttachedRenderState state = attached_state_cache.get();
                state.setup(parentState, accessory);
                addToRenderList(state);
            }
        }
    }

    private <M extends AccessorizableModel> void updateEmitterWorldPosition(@NonNull Emitter<?> emitter,
            @NonNull Accessory accessory, @NonNull ElementRenderState<M> parentState) {
        // Get parent world transform (pos and rot)
        parentState.getTransform(temp_matrix);

        // Get the relative offset in parent local space
        rel_matrix.identity();
        accessory.getRelativeTransform(rel_matrix, parentState.model);

        // Transform the LOCAL offset to WORLD space
        temp_matrix.transformPosition(rel_matrix.m30(), rel_matrix.m31(), rel_matrix.m32(), pos_vector);

        emitter.getPosition().set(pos_vector);
    }

    private float getVisuallyCorrectHeight(float x_f, float y_f) {
        return local_player.getWorld().getHeightMap().computeInterpolatedHeight(0, x_f, y_f);
    }

    private static float getBuildingSelectionRadius(@NonNull Building building) {
        Building.BuildState render_level = building.getRenderLevel();
        return switch (render_level) {
            case START -> building.getTemplate().getStartSelectionRadius();
            case HALFBUILT -> building.getTemplate().getHalfbuiltSelectionRadius();
            case BUILT -> building.getTemplate().getBuiltSelectionRadius();
        };
    }

    private static float getBuildingSelectionHeight(@NonNull Building building) {
        Building.BuildState render_level = building.getRenderLevel();
        return switch (render_level) {
            case START -> building.getTemplate().getStartSelectionHeight();
            case HALFBUILT -> building.getTemplate().getHalfbuiltSelectionHeight();
            case BUILT -> building.getTemplate().getBuiltSelectionHeight();
        };
    }

    private static final ModelVisitor<Building> building_visitor = new SelectableVisitor<>();

    private void visitBuilding(final @NonNull Building building) {
        float z_offset = getVisuallyCorrectHeight(building.getPositionX(), building.getPositionY());
        visitSelectable(building_visitor, building, z_offset, getBuildingSelectionRadius(building),
                getBuildingSelectionHeight(building));
    }

    SpriteSorter.@NonNull DetailMode addToRenderList(@NonNull LODObject model) {
        return addToRenderList(model, false);
    }

    SpriteSorter.@NonNull DetailMode addToRenderList(@NonNull LODObject model, boolean point_on_map) {
        return sprite_sorter.add(model, camera, point_on_map);
    }

    private void visitEmitter(final @NonNull Emitter<?> emitter) {
        if (!picking)
            emitter_queue.add(emitter);
    }

    private void visitLightning(@NonNull Lightning lightning) {
        if (!picking)
            lightning_queue.add(lightning);
    }

    private void visitSonicBlastEffect(@NonNull SonicBlastEffect effect) {
        if (!picking)
            sonic_blast_queue.add(effect);
    }

    private void visitRespond(final @NonNull LandscapeTargetRespond respond) {
        if (!picking)
            target_respond_renderer.addToTargetList(respond);
    }

    private static final ModelVisitor<SupplyModel> supply_model_visitor = new SupplyModelVisitor<>() {
        @Override
        public void getTransform(@NonNull ElementRenderState<SupplyModel> render_state, @NonNull Matrix4f dest) {
            SupplyModel model = render_state.getModel();
            dest.translation(model.getPositionX(), model.getPositionY(), model.getPositionZ())
                    .rotate((float) Math.toRadians(model.getRotation()), 0f, 0f, 1f);
        }
    };

    private void visitSupplyModel(final @NonNull SupplyModel model) {
        addToRenderList(getCachedState(supply_model_visitor, model));
    }

    private static final ModelVisitor<RubberSupply> rubber_model_visitor = new SupplyModelVisitor<>() {
        @Override
        public void getTransform(@NonNull ElementRenderState<RubberSupply> render_state, @NonNull Matrix4f dest) {
            Model model = render_state.model;
            float angle = (float) Math.atan2(model.getDirectionY(), model.getDirectionX());
            dest.translation(model.getPositionX(), model.getPositionY(), render_state.f)
                    .rotate(angle, 0f, 0f, 1f);
        }
    };

    private void visitRubberSupply(final @NonNull RubberSupply model) {
        float z_offset = getVisuallyCorrectHeight(model.getPositionX(), model.getPositionY()) + model.getOffsetZ();
        ElementRenderState<RubberSupply> state = (ElementRenderState<RubberSupply>) getCachedState(rubber_model_visitor, model, z_offset);
        addToRenderList(state);
        if (!picking && !model.isHit())
            default_shadow_renderer.addToShadowList(state);
        visitAccessories(model, state);
    }

    private static final ModelVisitor<SceneryModel> scenery_model_visitor = new WhiteModelVisitor<>() {
    };

    private void visitSceneryModel(final @NonNull SceneryModel model) {
        ModelState<SceneryModel> state = getCachedState(scenery_model_visitor, model);
        addToRenderList(state);
        if (!picking) {
            if (model.getShadowDiameter() > 0f)
                default_shadow_renderer.addToShadowList(state);
        }
    }

    private static final float PLANTS_CUT_DIST = 200;
    private static final ModelVisitor<Plants> plants_model_visitor = new WhiteModelVisitor<>() {
        private static final float START_FADE_DIST = 100;

        @Override
        public void getTransform(@NonNull ElementRenderState<Plants> render_state, @NonNull Matrix4f dest) {
            Plants plants = render_state.getModel();
            float angle = (float) Math.atan2(plants.getDirectionY(), plants.getDirectionX());
            dest.translation(plants.getPositionX(), plants.getPositionY(), plants.getPositionZ())
                    .rotate(angle, 0f, 0f, 1f);

            float dist_squared = render_state.f;
            if (dist_squared > START_FADE_DIST * START_FADE_DIST) {
                float camera_dist = (float) Math.sqrt(dist_squared);
                float alpha = 1f - ((camera_dist - START_FADE_DIST) / (PLANTS_CUT_DIST - START_FADE_DIST));
                var linearOne = Color.toLinear(1f);
                render_state.setColor(linearOne, linearOne, linearOne, alpha);
            }
        }
    };

    private void visitPlants(final @NonNull Plants plants) {
        if (!picking && Globals.draw_plants) {
            float camera_dist_sqr = RenderTools.getEyeDistanceSquared(plants, camera.getCurrentX(), camera
                    .getCurrentY(), camera.getCurrentZ());
            if (camera_dist_sqr <= PLANTS_CUT_DIST * PLANTS_CUT_DIST)
                addToRenderList(getCachedState(plants_model_visitor, plants, camera_dist_sqr));
        }
    }

    private static final ModelVisitor<DirectedThrowingWeapon> directed_weapon_model_visitor
            = new WhiteModelVisitor<>() {
                @Override
                public void getTransform(@NonNull ElementRenderState<DirectedThrowingWeapon> render_state,
                        @NonNull Matrix4f dest) {
                    DirectedThrowingWeapon model = render_state.getModel();
                    float yawRad = (float) Math.atan2(model.getDirectionY(), model.getDirectionX());
                    float pitchRad = (float) Math.toRadians(model.getAngle());
                    dest.translation(model.getPositionX(), model.getPositionY(), model.getPositionZ())
                            .rotate(yawRad, 0f, 0f, 1f)
                            .rotate(-pitchRad, 0f, 1f, 0f);
                }

                @Override
                public @NonNull Color getTeamColor(@NonNull ElementRenderState<DirectedThrowingWeapon> render_state) {
                    return render_state.getModel().getSrc().getOwner().getColor();
                }
            };

    private static final ModelVisitor<RotatingThrowingWeapon> rotating_weapon_model_visitor
            = new WhiteModelVisitor<>() {
                @Override
                public void getTransform(@NonNull ElementRenderState<RotatingThrowingWeapon> render_state,
                        @NonNull Matrix4f dest) {
                    RotatingThrowingWeapon model = render_state.getModel();
                    float yawRad = (float) Math.atan2(model.getDirectionY(), model.getDirectionX());
                    float spinRad = (float) Math.toRadians(model.getAngle());
                    dest.translation(model.getPositionX(), model.getPositionY(), model.getPositionZ())
                            .rotate(yawRad, 0f, 0f, 1f)
                            .rotate(spinRad, 0f, 1f, 0f);
                }

                @Override
                public @NonNull Color getTeamColor(@NonNull ElementRenderState<RotatingThrowingWeapon> render_state) {
                    return render_state.getModel().getSrc().getOwner().getColor();
                }
            };

    public @NonNull Queue<@NonNull Emitter<?>> getEmitterQueue() {
        return emitter_queue;
    }

    public @NonNull Queue<@NonNull Lightning> getLightningQueue() {
        return lightning_queue;
    }

    public @NonNull Queue<@NonNull SonicBlastEffect> getSonicBlastQueue() {
        return sonic_blast_queue;
    }
}
