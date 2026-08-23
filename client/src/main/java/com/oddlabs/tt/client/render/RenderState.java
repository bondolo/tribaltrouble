package com.oddlabs.tt.client.render;

import com.oddlabs.tt.audio.AudioImplementation;
import com.oddlabs.tt.base.animation.Animated;
import com.oddlabs.tt.client.viewer.Selection;
import com.oddlabs.tt.effects.particle.BalancedParametricEmitter;
import com.oddlabs.tt.effects.particle.Emitter;
import com.oddlabs.tt.effects.particle.Lightning;
import com.oddlabs.tt.effects.particle.PointEmitterModel;
import com.oddlabs.tt.effects.particle.SonicBlastEffect;
import com.oddlabs.tt.effects.particle.StunFunction;
import com.oddlabs.tt.effects.render.CrackDecalRenderer;
import com.oddlabs.tt.effects.render.EmitterAccessory;
import com.oddlabs.tt.effects.render.EmitterAttachedAccessory;
import com.oddlabs.tt.engine.procedural.GeneratorRing;
import com.oddlabs.tt.engine.render.Accessory;
import com.oddlabs.tt.engine.render.AttachedRenderState;
import com.oddlabs.tt.engine.render.CameraState;
import com.oddlabs.tt.engine.render.DecalRenderer;
import com.oddlabs.tt.engine.render.ElementSceneContext;
import com.oddlabs.tt.engine.render.LODObject;
import com.oddlabs.tt.engine.render.MatrixStack;
import com.oddlabs.tt.engine.render.ModelState;
import com.oddlabs.tt.engine.render.ModelVisitor;
import com.oddlabs.tt.engine.render.DebugFlags;
import com.oddlabs.tt.engine.render.RenderQueues;
import com.oddlabs.tt.engine.render.RenderTools;
import com.oddlabs.tt.engine.render.SceneContext;
import com.oddlabs.tt.engine.render.ShadowListKey;
import com.oddlabs.tt.engine.render.SpriteKey;
import com.oddlabs.tt.engine.render.StaticAccessory;
import com.oddlabs.tt.engine.resource.AssetRegistry;
import com.oddlabs.tt.net.PeerHub;
import com.oddlabs.tt.simulation.behaviour.StunController;
import com.oddlabs.tt.simulation.model.BoundingBox;
import com.oddlabs.tt.simulation.model.Building;
import com.oddlabs.tt.simulation.model.BuildingType;
import com.oddlabs.tt.simulation.model.Element;
import com.oddlabs.tt.simulation.model.Model;
import com.oddlabs.tt.simulation.model.Plants;
import com.oddlabs.tt.simulation.model.Race;
import com.oddlabs.tt.simulation.model.RubberSupply;
import com.oddlabs.tt.simulation.model.SceneryModel;
import com.oddlabs.tt.simulation.model.Selectable;
import com.oddlabs.tt.simulation.model.Shadowable;
import com.oddlabs.tt.simulation.model.SupplyModel;
import com.oddlabs.tt.simulation.model.Unit;
import com.oddlabs.tt.simulation.model.weapon.DirectedThrowingWeapon;
import com.oddlabs.tt.simulation.model.weapon.LightningCloud;
import com.oddlabs.tt.simulation.model.weapon.PoisonFog;
import com.oddlabs.tt.simulation.model.weapon.RotatingThrowingWeapon;
import com.oddlabs.tt.simulation.model.weapon.SonicBlast;
import com.oddlabs.tt.simulation.model.weapon.Stun;
import com.oddlabs.tt.simulation.player.Player;
import com.oddlabs.util.Color;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Optional;
import java.util.Queue;

/**
 * Manages the rendering state and visit logic for world entities and their accessories.
 */
public final class RenderState implements SceneContext {
    private final Queue<Emitter<?>> emitter_queue = new ArrayDeque<>();
    private final Queue<Lightning> lightning_queue = new ArrayDeque<>();
    private final Queue<SonicBlastEffect> sonic_blast_queue = new ArrayDeque<>();
    private final SpriteSorter sprite_sorter;
    private final RenderStateCache<ElementSceneContext<Model>> render_state_cache;
    private final RenderStateCache<AttachedRenderState> attached_state_cache;
    private final RenderQueues render_queues;
    private final TargetRespondRenderer target_respond_renderer;
    private final SelectableShadowRenderer default_shadow_renderer;
    private final CrackDecalRenderer crack_shadow_renderer;
    private final Picker picker;
    private final @Nullable Selection selection;
    private final Player local_player;
    private final MatrixStack model_view_stack = new MatrixStack();
    private final AudioImplementation audio;

    private boolean picking;
    private boolean visible_override;
    private @Nullable CameraState camera;

    public RenderState(Player local_player, SpriteSorter sprite_sorter,
            RenderQueues render_queues, Picker picker, @Nullable Selection selection,
            AudioImplementation audio) {
        this.local_player = local_player;
        this.selection = selection;
        this.picker = picker;
        this.sprite_sorter = sprite_sorter;
        this.render_queues = render_queues;
        this.audio = audio;
        var respondDesc = new GeneratorRing(DecalRenderer.HALO_LUT_RESOLUTION,
                new float[][]{{0.40f, 0f}, {0.41f, 1f}, {0.48f, 1f}, {0.49f, 0f}});
        this.target_respond_renderer = new TargetRespondRenderer(respondDesc);
        render_queues.registerShadowRenderer(respondDesc, target_respond_renderer);

        this.default_shadow_renderer = new SelectableShadowRenderer(AssetRegistry.DEFAULT_SHADOW_DESC);
        render_queues.registerShadowRenderer(AssetRegistry.DEFAULT_SHADOW_DESC, default_shadow_renderer);

        this.crack_shadow_renderer = new CrackDecalRenderer(AssetRegistry.CRACK_DECAL_DESC);
        render_queues.registerShadowRenderer(AssetRegistry.CRACK_DECAL_DESC, crack_shadow_renderer);
        this.render_state_cache = new RenderStateCache<>(() -> new ElementSceneContext<>(RenderState.this));
        this.attached_state_cache = new RenderStateCache<>(AttachedRenderState::new);
    }

    public void visit(Element<?> element) {
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
            case PointEmitterModel emitterModel -> visitPointEmitterModel(emitterModel);
            case SonicBlast blast -> visitSonicBlast(blast);
            case LightningCloud cloud -> visitLightningCloud(cloud);
            case PoisonFog fog -> visitPoisonFog(fog);
            case Stun stun -> visitStun(stun);
            default -> throw new UnsupportedOperationException("element has no rendering defined " + element);
        }
    }

    private void visitLightningCloud(final LightningCloud cloud) {
        if (picking) return;
        float z_offset = getVisuallyCorrectHeight(cloud.getPositionX(), cloud.getPositionY());
        ElementSceneContext<LightningCloud> state = (ElementSceneContext<LightningCloud>) getCachedState(
                WhiteModelVisitor.getInstance(), cloud, z_offset);
        visitAccessories(cloud, state);
    }

    private void visitPoisonFog(final PoisonFog fog) {
        if (picking) return;
        float z_offset = getVisuallyCorrectHeight(fog.getPositionX(), fog.getPositionY());
        ElementSceneContext<PoisonFog> state = (ElementSceneContext<PoisonFog>) getCachedState(
                WhiteModelVisitor.getInstance(), fog, z_offset);
        visitAccessories(fog, state);
    }

    private void visitStun(final Stun stun) {
        if (picking) return;
        float z_offset = getVisuallyCorrectHeight(stun.getPositionX(), stun.getPositionY());
        ElementSceneContext<Stun> state = (ElementSceneContext<Stun>) getCachedState(
                WhiteModelVisitor.getInstance(), stun, z_offset);
        visitAccessories(stun, state);
    }

    private void visitPointEmitterModel(final PointEmitterModel emitterModel) {
        if (picking) return;
        emitter_queue.add(emitterModel.getEmitter());
        float z_offset = getVisuallyCorrectHeight(emitterModel.getPositionX(), emitterModel.getPositionY());
        ElementSceneContext<PointEmitterModel> state = (ElementSceneContext<PointEmitterModel>) getCachedState(
                WhiteModelVisitor.getInstance(), emitterModel, z_offset);
        visitAccessories(emitterModel, state);
    }

    private void visitSonicBlast(final SonicBlast blast) {
        if (picking) return;
        float z_offset = getVisuallyCorrectHeight(blast.getPositionX(), blast.getPositionY());
        ElementSceneContext<SonicBlast> state = (ElementSceneContext<SonicBlast>) getCachedState(
                WhiteModelVisitor.getInstance(), blast, z_offset);
        visitAccessories(blast, state);
    }

    Player getLocalPlayer() {
        return local_player;
    }

    public SelectableShadowRenderer getDefaultShadowRenderer() {
        return default_shadow_renderer;
    }

    @Override
    public boolean isResponding(Model target) {
        return picker.getRespondManager().isResponding(target);
    }

    @Override
    public RenderQueues getRenderQueues() {
        return render_queues;
    }

    MatrixStack getModelViewStack() {
        return model_view_stack;
    }

    public void setVisibleOverride(boolean override) {
        this.visible_override = override;
    }

    public void setup(boolean picking, CameraState camera_state) {
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

    @Override
    public @Nullable CameraState getCamera() {
        return camera;
    }

    boolean isPicking() {
        return picking;
    }

    boolean overrideVisibility() {
        return visible_override;
    }

    private static final ModelVisitor<Unit> unit_visitor = new SelectableVisitor<>();

    private void visitUnit(final Unit unit) {
        float z_offset = getVisuallyCorrectHeight(unit.getPositionX(), unit.getPositionY()) + unit.getOffsetZ();
        visitSelectable(unit_visitor, unit, z_offset, unit.getTemplate().getSelectionRadius(), unit.getTemplate()
                .getSelectionHeight());
    }

    private <M extends Model> ElementSceneContext<M> doGetCachedState() {
        return (ElementSceneContext<M>) render_state_cache.get();
    }

    private <M extends Model> ModelState<M> getCachedState(ModelVisitor<M> visitor,
            M model) {
        ElementSceneContext<M> state = doGetCachedState();
        state.setup(visitor, model);
        return state;
    }

    private <M extends Model> ModelState<M> getCachedState(ModelVisitor<M> visitor, M model,
            float dist_squared) {
        ElementSceneContext<M> state = doGetCachedState();
        state.setup(visitor, model, dist_squared);
        return state;
    }

    private static boolean pickingInFrustum(Selectable<?> selectable, float[][] frustum, float z_offset,
            float selection_radius, float selection_height) {
        BoundingBox picking_selection_box = new BoundingBox();
        picking_selection_box.setBounds(-selection_radius + selectable.getPositionX(), selection_radius + selectable
                .getPositionX(), -selection_radius + selectable.getPositionY(), selection_radius + selectable
                        .getPositionY(), z_offset, z_offset + selection_height);
        return RenderTools.inFrustum(picking_selection_box, frustum) != RenderTools.FrustumIntersection.ALL_OUTSIDE;
    }

    boolean isHovered(Selectable<?> selectable) {
        return selectable == picker.getCurrentHovered();
    }

    boolean isSelected(Selectable<?> selectable) {
        return selection != null && selection.getCurrentSelection().contains(selectable);
    }

    private <S extends Selectable<?>> void visitSelectable(ModelVisitor<S> visitor, S selectable,
            float z_offset, float selection_radius, float selection_height) {
        boolean in_view = !picking || (selectable.isEnabled() && (visible_override || pickingInFrustum(selectable,
                camera.getFrustum(), z_offset, selection_radius, selection_height)));
        if (in_view) {
            Player owner = selectable.getOwnerNoCheck();
            boolean point_on_map = !local_player.isEnemy(owner) || (!owner.teamHasBuilding() && PeerHub
                    .getFreeQuitTimeLeft(local_player.getWorld()) < 0f);
            ElementSceneContext<S> state = (ElementSceneContext<S>) getCachedState(visitor, selectable, z_offset);
            SpriteSorter.DetailMode sort_status = addToRenderList(state, point_on_map);
            if (!picking && selectable.isEnabled() && sort_status == SpriteSorter.DetailMode.POLYGON) {
                ShadowListKey shadowKey = null;
                Race race = selectable.getOwnerNoCheck().getRaceInfo().getRaceType();
                if (selectable instanceof Unit) {
                    shadowKey = AssetRegistry.getInstance().getDefaultUnitShadow();
                } else if (selectable instanceof Building building) {
                    BuildingType bvt = building.getTemplate().getBuildingType();
                    shadowKey = AssetRegistry.getInstance().getBuildingVisuals(race, bvt).shadow();
                }
                if (shadowKey != null) {
                    SelectableShadowRenderer shadow_renderer = (SelectableShadowRenderer) render_queues
                            .getShadowRenderer(shadowKey);
                    if (isHovered(selectable) || isSelected(selectable)) {
                        shadow_renderer.addToSelectionList(state);
                    } else if (selectable.getShadowDiameter() > 0f) {
                        shadow_renderer.addToShadowList(state);
                    }
                }
            }
            visitAccessories(selectable, state);
        }
    }

    private VisualModel getOrCreateVisualModel(Model model) {
        return model.getClientState(VisualModel.class).orElseGet(() -> {
            VisualModel visualModel = new VisualModel(model, audio);
            model.setClientState(visualModel);
            return visualModel;
        });
    }

    private <M extends Model> void visitAccessories(M model, ElementSceneContext<M> parentState) {
        VisualModel visualModel = getOrCreateVisualModel(model);

        // Dynamically add/remove stun star accessory for units
        if (model instanceof Unit unit) {
            if (unit.getCurrentController() instanceof StunController stunController) {
                boolean hasStunStar = false;
                for (Accessory acc : visualModel.getAccessories()) {
                    if (acc instanceof EmitterAttachedAccessory) {
                        hasStunStar = true;
                        break;
                    }
                }
                if (!hasStunStar) {
                    float timeLeft = stunController.getTime();
                    float velocity = (float) Math.PI / 2;
                    BalancedParametricEmitter emitter = new BalancedParametricEmitter(
                            local_player.getWorld(),
                            new StunFunction(.4f, .15f), new Vector3f(0f, 0f, 0f),
                            velocity, 5f, (float) Math.PI * 2, (float) Math.PI * 2,
                            5, 0f, 2f,
                            Color.Linear.WHITE, Color.LinearDelta.ZERO,
                            new Vector3f(.1f, .1f, .1f), new Vector3f(0f, 0f, 0f), timeLeft,
                            GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                            AssetRegistry.getInstance().getStarTextures());

                    float mountOffset = unit.getMountOffset();
                    var offset = new Vector3f(
                            unit.getTemplate().getStunX(),
                            unit.getTemplate().getStunY(),
                            unit.getTemplate().getStunZ() + mountOffset);
                    visualModel.getAccessories().add(new EmitterAttachedAccessory(emitter, offset));
                }
            } else {
                visualModel.getAccessories().removeIf(acc -> acc instanceof EmitterAttachedAccessory);
            }
        }

        List<Accessory> accessories = visualModel.getAccessories();
        for (Accessory accessory : accessories) {
            if (accessory != null && accessory.isVisible(model, camera)) {
                visitAccessory(accessory, parentState);
            }
        }
    }

    private <M extends Model> void visitAccessory(Accessory accessory,
            ElementSceneContext<M> parentState) {
        if (picking) return;

        switch (accessory) {
            case EmitterAccessory ea -> {
                Emitter<?> emitter = ea.getEmitter();
                if (emitter != null) {
                    updateEmitterWorldPosition(emitter, ea, parentState);
                }
                ea.addEmitters(emitter_queue);
            }
            case StaticAccessory s -> {
                AttachedRenderState state = attached_state_cache.get();
                state.setup(parentState, s);
                addToRenderList(state);
            }
            default -> {
                AttachedRenderState state = attached_state_cache.get();
                state.setup(parentState, accessory);
                addToRenderList(state);
            }
        }
    }

    private <M extends Model> void updateEmitterWorldPosition(Emitter<?> emitter,
            Accessory accessory, ElementSceneContext<M> parentState) {
        Matrix4f transform = new Matrix4f();
        parentState.getTransform(transform);
        accessory.getRelativeTransform(transform, parentState.model);
        emitter.getPosition().set(transform.m30(), transform.m31(), transform.m32());
    }

    private float getVisuallyCorrectHeight(float x_f, float y_f) {
        return local_player.getWorld().getHeightMap().computeInterpolatedHeight(0, x_f, y_f);
    }

    private static float getBuildingSelectionRadius(Building building) {
        Building.BuildStage render_level = building.getBuildStage();
        var template = building.getTemplate();
        return switch (render_level) {
            case START -> template.getStartSelectionRadius();
            case HALFBUILT -> template.getHalfbuiltSelectionRadius();
            case UNPLACED, BUILT -> template.getBuiltSelectionRadius();
        };
    }

    private static float getBuildingSelectionHeight(Building building) {
        Building.BuildStage render_level = building.getBuildStage();
        var template = building.getTemplate();
        return switch (render_level) {
            case START -> template.getStartSelectionHeight();
            case HALFBUILT -> template.getHalfbuiltSelectionHeight();
            case UNPLACED, BUILT -> template.getBuiltSelectionHeight();
        };
    }

    private static final ModelVisitor<Building> building_visitor = new SelectableVisitor<>();

    private void visitBuilding(final Building building) {
        float z_offset = getVisuallyCorrectHeight(building.getPositionX(), building.getPositionY());
        visitSelectable(building_visitor, building, z_offset, getBuildingSelectionRadius(building),
                getBuildingSelectionHeight(building));
    }

    SpriteSorter.DetailMode addToRenderList(LODObject model) {
        return addToRenderList(model, false);
    }

    SpriteSorter.DetailMode addToRenderList(LODObject model, boolean point_on_map) {
        return sprite_sorter.add(model, camera, point_on_map);
    }

    private void visitEmitter(final Emitter<?> emitter) {
        if (!picking)
            emitter_queue.add(emitter);
    }

    private void visitLightning(Lightning lightning) {
        if (!picking)
            lightning_queue.add(lightning);
    }

    private void visitSonicBlastEffect(SonicBlastEffect effect) {
        if (!picking)
            sonic_blast_queue.add(effect);
    }

    private void visitRespond(final LandscapeTargetRespond respond) {
        if (!picking)
            target_respond_renderer.addToTargetList(respond);
    }

    private static final ModelVisitor<SupplyModel> supply_model_visitor = new SupplyModelVisitor<>() {
        @Override
        public Optional<SpriteKey> getSpriteKey(ElementSceneContext<SupplyModel> render_state) {
            return Optional.ofNullable(render_state.getModel().getBoundsProvider() instanceof SpriteKey spriteKey
                    ? spriteKey : null);
        }

        @Override
        public void getTransform(ElementSceneContext<SupplyModel> render_state, Matrix4f dest) {
            SupplyModel model = render_state.getModel();
            dest.translation(model.getPositionX(), model.getPositionY(), model.getPositionZ())
                    .rotate(model.getRotation(), 0f, 0f, 1f);

            Color.Linear tint = model.getSpawnColorTint();
            if (tint != null) {
                render_state.setColor(tint);
            }
        }
    };

    private void visitSupplyModel(final SupplyModel model) {
        ElementSceneContext<SupplyModel> state = (ElementSceneContext<SupplyModel>) getCachedState(
                supply_model_visitor, model);
        addToRenderList(state);
        if (!picking) {
            if (model.getShadowDiameter() > 0f)
                default_shadow_renderer.addToShadowList(state);
            if (model.getCrackDecalOpacity() > 0.0f) {
                crack_shadow_renderer.addToCrackList(new Shadowable() {
                    @Override
                    public float getPositionX() {
                        return model.getPositionX();
                    }

                    @Override
                    public float getPositionY() {
                        return model.getPositionY();
                    }

                    @Override
                    public float getShadowDiameter() {
                        return model.getCrackDecalDiameter();
                    }

                    @Override
                    public float getShadowOpacity() {
                        return model.getCrackDecalOpacity();
                    }

                    @Override
                    public Color.Linear getShadowColor() {
                        Color.Linear color = model.getCrackDecalColor();
                        return color != null ? color : Color.Linear.BLACK;
                    }

                    @Override
                    public float getShadowVerticalCenter() {
                        return 0.6f;
                    }

                    @Override
                    public float getShadowPattern() {
                        return model.getCrackDecalPattern();
                    }
                });
            }
        }
        visitAccessories(model, state);
    }

    private static final ModelVisitor<RubberSupply> rubber_model_visitor = new SupplyModelVisitor<>() {
        @Override
        public Optional<SpriteKey> getSpriteKey(ElementSceneContext<RubberSupply> render_state) {
            return Optional.ofNullable(render_state.getModel().getBoundsProvider() instanceof SpriteKey spriteKey
                    ? spriteKey : null);
        }

        @Override
        public void getTransform(ElementSceneContext<RubberSupply> render_state, Matrix4f dest) {
            Model model = render_state.model;
            float angle = (float) Math.atan2(model.getDirectionY(), model.getDirectionX());
            dest.translation(model.getPositionX(), model.getPositionY(), render_state.f)
                    .rotate(angle, 0f, 0f, 1f);
        }
    };

    private void visitRubberSupply(final RubberSupply model) {
        float z_offset = getVisuallyCorrectHeight(model.getPositionX(), model.getPositionY()) + model.getOffsetZ();
        ElementSceneContext<RubberSupply> state = (ElementSceneContext<RubberSupply>) getCachedState(
                rubber_model_visitor,
                model, z_offset);
        addToRenderList(state);
        if (!picking && !model.isHit())
            default_shadow_renderer.addToShadowList(state);
        visitAccessories(model, state);
    }

    private static final ModelVisitor<SceneryModel> scenery_model_visitor = new WhiteModelVisitor<>() {
        @Override
        public Optional<SpriteKey> getSpriteKey(ElementSceneContext<SceneryModel> render_state) {
            return Optional.ofNullable(render_state.getModel().getBoundsProvider() instanceof SpriteKey spriteKey
                    ? spriteKey : null);
        }
    };

    private void visitSceneryModel(final SceneryModel model) {
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
        public Optional<SpriteKey> getSpriteKey(ElementSceneContext<Plants> render_state) {
            return Optional.ofNullable(render_state.getModel().getBoundsProvider() instanceof SpriteKey spriteKey
                    ? spriteKey : null);
        }

        @Override
        public void getTransform(ElementSceneContext<Plants> render_state, Matrix4f dest) {
            Plants plants = render_state.getModel();
            float angle = (float) Math.atan2(plants.getDirectionY(), plants.getDirectionX());
            dest.translation(plants.getPositionX(), plants.getPositionY(), plants.getPositionZ())
                    .rotate(angle, 0f, 0f, 1f);

            float dist_squared = render_state.f;
            if (dist_squared > START_FADE_DIST * START_FADE_DIST) {
                float camera_dist = (float) Math.sqrt(dist_squared);
                float alpha = 1f - ((camera_dist - START_FADE_DIST) / (PLANTS_CUT_DIST - START_FADE_DIST));
                render_state.setColor(new Color.Linear(1f, 1f, 1f, alpha));
            }
        }
    };

    private void visitPlants(final Plants plants) {
        if (!picking && DebugFlags.draw_plants) {
            float camera_dist_sqr = RenderTools.getEyeDistanceSquared(plants, camera.getCurrentX(), camera
                    .getCurrentY(), camera.getCurrentZ());
            if (camera_dist_sqr <= PLANTS_CUT_DIST * PLANTS_CUT_DIST)
                addToRenderList(getCachedState(plants_model_visitor, plants, camera_dist_sqr));
        }
    }

    private static final ModelVisitor<DirectedThrowingWeapon> directed_weapon_model_visitor
            = new WhiteModelVisitor<>() {
                @Override
                public Optional<SpriteKey> getSpriteKey(ElementSceneContext<
                        DirectedThrowingWeapon> render_state) {
                    DirectedThrowingWeapon model = render_state.getModel();
                    Race race = model.getSrc().getOwner().getRaceInfo().getRaceType();
                    return Optional.of(AssetRegistry.getInstance().getWeaponSprite(race, model
                            .getWeaponVisualType()));
                }

                @Override
                public void getTransform(ElementSceneContext<DirectedThrowingWeapon> render_state,
                        Matrix4f dest) {
                    DirectedThrowingWeapon model = render_state.getModel();
                    float yawRad = (float) Math.atan2(model.getDirectionY(), model.getDirectionX());
                    float pitchRad = (float) Math.toRadians(model.getAngle());
                    dest.translation(model.getPositionX(), model.getPositionY(), model.getPositionZ())
                            .rotate(yawRad, 0f, 0f, 1f)
                            .rotate(-pitchRad, 0f, 1f, 0f);
                }

                @Override
                public Color getTeamColor(ElementSceneContext<DirectedThrowingWeapon> render_state) {
                    return render_state.getModel().getSrc().getOwner().getColor();
                }
            };

    private static final ModelVisitor<RotatingThrowingWeapon> rotating_weapon_model_visitor
            = new WhiteModelVisitor<>() {
                @Override
                public Optional<SpriteKey> getSpriteKey(ElementSceneContext<
                        RotatingThrowingWeapon> render_state) {
                    RotatingThrowingWeapon model = render_state.getModel();
                    Race race = model.getSrc().getOwner().getRaceInfo().getRaceType();
                    return Optional.of(AssetRegistry.getInstance().getWeaponSprite(race, model
                            .getWeaponVisualType()));
                }

                @Override
                public void getTransform(ElementSceneContext<RotatingThrowingWeapon> render_state,
                        Matrix4f dest) {
                    RotatingThrowingWeapon model = render_state.getModel();
                    float yawRad = (float) Math.atan2(model.getDirectionY(), model.getDirectionX());
                    float spinRad = (float) Math.toRadians(model.getAngle());
                    dest.translation(model.getPositionX(), model.getPositionY(), model.getPositionZ())
                            .rotate(yawRad, 0f, 0f, 1f)
                            .rotate(spinRad, 0f, 1f, 0f);
                }

                @Override
                public Color getTeamColor(ElementSceneContext<RotatingThrowingWeapon> render_state) {
                    return render_state.getModel().getSrc().getOwner().getColor();
                }
            };

    public Queue<Emitter<?>> getEmitterQueue() {
        return emitter_queue;
    }

    public Queue<Lightning> getLightningQueue() {
        return lightning_queue;
    }

    public Queue<SonicBlastEffect> getSonicBlastQueue() {
        return sonic_blast_queue;
    }
}
