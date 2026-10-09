package com.oddlabs.tt.client.render;

import com.oddlabs.tt.audio.AudioManager;
import com.oddlabs.tt.client.delegate.Delegate;
import com.oddlabs.tt.client.viewer.AmbientAudio;
import com.oddlabs.tt.client.viewer.Cheat;
import com.oddlabs.tt.client.viewer.Selection;
import com.oddlabs.tt.effects.render.EmitterRenderer;
import com.oddlabs.tt.effects.render.LightningRenderer;
import com.oddlabs.tt.effects.render.SonicBlastRenderer;
import com.oddlabs.tt.engine.render.BoundingMode;
import com.oddlabs.tt.engine.render.CameraState;
import com.oddlabs.tt.engine.render.InstancedSpriteRenderer;
import com.oddlabs.tt.engine.render.MatrixStack;
import com.oddlabs.tt.engine.render.PostProcessor;
import com.oddlabs.tt.engine.render.DebugFlags;
import com.oddlabs.tt.engine.render.GpuPass;
import com.oddlabs.tt.engine.render.RenderQueues;
import com.oddlabs.tt.engine.render.SpriteKey;
import com.oddlabs.tt.engine.render.SpriteRenderer;
import com.oddlabs.tt.engine.render.Texture;
import com.oddlabs.tt.engine.render.shader.DebugShaderRenderer;
import com.oddlabs.tt.engine.render.shader.ShaderProgram;
import com.oddlabs.tt.engine.render.state.GlobalUniforms;
import com.oddlabs.tt.engine.render.state.RenderContext;
import com.oddlabs.tt.gui.render.SceneRenderer;
import com.oddlabs.tt.scenery.LandscapeRenderer;
import com.oddlabs.tt.scenery.SeaBottom;
import com.oddlabs.tt.scenery.Sky;
import com.oddlabs.tt.scenery.Water;
import com.oddlabs.tt.client.resource.AssetRegistry;
import com.oddlabs.tt.engine.resource.WorldInfo;
import com.oddlabs.tt.engine.settings.AccessibilitySettings;
import com.oddlabs.tt.engine.settings.GraphicsSettings;
import com.oddlabs.tt.base.global.Settings;
import com.oddlabs.tt.engine.util.DebugRender;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.ToolTip;
import com.oddlabs.tt.simulation.landscape.TreeSupply;
import com.oddlabs.tt.simulation.landscape.World;
import com.oddlabs.tt.simulation.model.Building;
import com.oddlabs.tt.simulation.model.Target;
import com.oddlabs.tt.simulation.model.Unit;
import com.oddlabs.tt.simulation.player.Player;
import com.oddlabs.tt.window.WindowSettings;
import com.oddlabs.util.Color;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;

/**
 * Primary world renderer coordinating the rendering of landscape, units, buildings, and transient effects.
 */
public final class DefaultRenderer implements SceneRenderer, AutoCloseable {
    private final Picker picker;
    private final Water water;
    private final Sky sky;
    private final SeaBottom seaBottom;
    private final LandscapeRenderer landscape_renderer;
    private final World world;
    private final ElementRenderer<?> element_renderer;
    private final TreeRenderer tree_renderer;
    private final SpriteSorter sprite_sorter;
    private final RenderQueues render_queues;
    private final MatrixStack modelViewStack;
    private final MatrixStack projectionStack;
    private final Selection selection;
    private final LightningRenderer lightningRenderer;
    private final SonicBlastRenderer sonicBlastRenderer;
    private final EmitterRenderer emitterRenderer;
    private final InstancedSpriteRenderer treeSpriteRenderer = new InstancedSpriteRenderer();
    private final PostProcessor postProcessor;
    private final @Nullable Cheat cheat;
    private final AmbientAudio ambient;

    private final GlobalUniforms globalUniforms = new GlobalUniforms();

    private final Matrix4f rallyPointMatrix = new Matrix4f();

    private @Nullable Building selected_building;

    public DefaultRenderer(@Nullable Cheat cheat, Player local_player,
            RenderQueues render_queues,
            WorldInfo<Texture> world_info, LandscapeRenderer landscape_renderer,
            Picker picker,
            Selection selection, MatrixStack modelViewStack, MatrixStack projectionStack,
            AudioManager audioManager, Settings settings, int width, int height) {
        this.world = local_player.getWorld();
        this.cheat = cheat;
        this.ambient = new AmbientAudio(audioManager);
        this.render_queues = render_queues;
        this.picker = picker;
        this.selection = selection;
        this.sprite_sorter = new SpriteSorter(GraphicsSettings.from(settings).graphic_detail);
        this.element_renderer = new ElementRenderer<>(local_player, render_queues, picker, false, sprite_sorter,
                selection, audioManager);
        this.tree_renderer = new TreeRenderer(cheat, picker.getRespondManager(), treeSpriteRenderer,
                picker.getAnimationManager(), landscape_renderer);
        this.landscape_renderer = landscape_renderer;
        this.sky = new Sky(world.getHeightMap(), world_info.landscapeData().terrain());
        this.seaBottom = new SeaBottom(world_info.landscapeData().terrain(),
                world_info.detail(), world_info.detailNormal(), sky.getRingMesh());
        this.modelViewStack = modelViewStack;
        this.projectionStack = projectionStack;
        this.water = new Water(landscape_renderer.getHeightMapVisual(), world.getHeightMap(), world_info.landscapeData()
                .terrain(), sky, modelViewStack);
        this.landscape_renderer.setWater(this.water);
        this.lightningRenderer = new LightningRenderer();
        this.sonicBlastRenderer = new SonicBlastRenderer();
        this.emitterRenderer = new EmitterRenderer();
        this.postProcessor = new PostProcessor(AccessibilitySettings.from(settings), width, height,
                WindowSettings.from(settings).view_samples);
        DebugRender.setShaderRenderer(new DebugShaderRenderer(
                modelViewStack, projectionStack
        ));
    }

    private void drawAxes() {
        float center = world.getHeightMap().getMetersPerWorld() / 2f;
        float z = world.getHeightMap().getNearestHeight(center, center);
        DebugRender.drawAxes(center, z);
    }

    @Override
    public boolean isCheater() {
        return cheat != null && cheat.isEnabled();
    }

    public RenderState getRenderState() {
        return element_renderer.getRenderState();
    }

    public void setSelectedBuilding(@Nullable Building building) {
        this.selected_building = building;
    }

    private void renderRallyPoint(RenderContext context, CameraState camera_state) {
        if (selected_building != null && !selected_building.isDead() && selected_building.hasRallyPoint())
            doRenderRallyPoint(context, camera_state,
                    selected_building.getRallyPoint(), AssetRegistry.getInstance().getRallyPoint(selected_building
                            .getOwner().getRaceInfo().getRaceType()),
                    SelectableVisitor.getTeamColor(selected_building));
    }

    private void doRenderRallyPoint(RenderContext context, CameraState camera_state,
            Target rally_point, SpriteKey rally_sprite, Color.Linear teamColor) {

        SpriteRenderer rally_point_renderer = render_queues.getRenderer(rally_sprite);

        float x = rally_point.getPositionX();
        float y = rally_point.getPositionY();
        float z = world.getHeightMap().getNearestHeight(rally_point.getPositionX(), rally_point.getPositionY());
        if (rally_point instanceof Building rally_building) {
            var rally = rally_building.getTemplate().getRally();
            x += rally.x();
            y += rally.y();
            z += rally.z();
        }

        float dx = camera_state.getCurrentX() - x;
        float dy = camera_state.getCurrentY() - y;
        if (dx * dx + dy * dy > 0.01f) {
            float angle = (float) Math.atan2(dy, dx);
            rallyPointMatrix.translation(x, y, z).rotate(angle, 0f, 0f, 1f);
        } else {
            rallyPointMatrix.translation(x, y, z);
        }

        rally_point_renderer.addInstance(
                0, // spriteIndex
                0, // animation
                0f, // animTicks
                false, // respond
                true,  // blend
                true,  // depthWrite
                true,  // depthTest
                rallyPointMatrix,
                Color.Linear.WHITE,
                teamColor
        );
    }

    @Override
    public void pickHover(boolean can_hover_behind, CameraState camera, int x, int y) {
        if (can_hover_behind) {
            picker.pickHoverPhysical(camera, x, y);
        } else {
            picker.resetCurrentHovered();
        }
    }

    @Override
    public @Nullable ToolTip getToolTip() {
        return picker.getCurrentToolTip();
    }

    public void onTreeFelled(TreeSupply tree) {
        tree_renderer.onTreeFelled(tree);
    }

    public void onTreeSpawned(TreeSupply tree) {
        tree_renderer.onTreeSpawned(tree);
    }

    private void renderDebugElements(CameraState frustum_state) {
        if (DebugFlags.draw_axes) drawAxes();
        landscape_renderer.debugRender(frustum_state);
        lightningRenderer.debugRender(element_renderer.getRenderState().getLightningQueue());
        emitterRenderer.debugRender(element_renderer.getRenderState().getEmitterQueue());
        tree_renderer.debugRender(tree_renderer.getRenderLists(), tree_renderer.getRespondRenderLists());

        if (DebugFlags.isBoundsEnabled(BoundingMode.REGIONS))
            PathfinderDebugRenderer.renderRegions(world.getUnitGrid(), frustum_state.getCurrentX(), frustum_state
                    .getCurrentY());
        if (DebugFlags.isBoundsEnabled(BoundingMode.OCCUPATION)) picker.debugRender();
        if (DebugFlags.isBoundsEnabled(BoundingMode.UNIT_GRID)) {
            PathfinderDebugRenderer.renderUnitGrid(world.getUnitGrid(), frustum_state.getCurrentX(), frustum_state
                    .getCurrentY());
            for (Object obj : selection.getCurrentSelection().getSet()) {
                if (obj instanceof Unit unit) PathfinderDebugRenderer.renderPathTracker(unit.getPathTracker());
            }
        }
        DebugRender.flush();
    }

    @Override
    public void render(RenderContext context, CameraState frustum_state, GUIRoot gui_root) {
        var gpu = context.gpuTimer();
        treeSpriteRenderer.clear();
        render_queues.getInstancedRenderer().clear();

        try (var _ = postProcessor.withScene(frustum_state.getWidth(), frustum_state.getHeight())) {
            context.setDrawBuffers(true); // Ensure both Color and Mask are cleared
            context.setColorMask(true, true, true, true);
            context.setDepthMask(true);
            context.setDepthTest(true);
            context.setDepthFunc(GL11.GL_LEQUAL);
            context.clear(true, true);

            context.setViewport(0, 0, frustum_state.getWidth(), frustum_state.getHeight());

            float currentTime = gui_root.getTime();
            globalUniforms.update(context, frustum_state, currentTime,
                    world.getHeightMap().getSeaLevelMeters(), water);

            ambient.updateSoundListener(frustum_state, world.getHeightMap());
            modelViewStack.current().set(frustum_state.getModelView());
            projectionStack.current().set(frustum_state.getProjectionMatrix());

            if (DebugFlags.line_mode || (cheat != null && cheat.line_mode)) {
                GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE);
            }

            renderEnvironment(context, frustum_state, currentTime);
            prepareCPU(frustum_state, currentTime);
            renderOpaqueGeometry(context, frustum_state, gui_root, currentTime);
            renderWater(context, frustum_state, currentTime);
            renderTransparents(context, frustum_state);
            renderOverlays(context, frustum_state);
        }

        gpu.begin(GpuPass.COMPOSITE);
        postProcessor.renderComposite(context);
        gpu.end();
    }

    private void prepareCPU(CameraState frustum_state, float currentTime) {
        if (DebugFlags.process_trees) {
            tree_renderer.setup(frustum_state);
            tree_renderer.visit(world.getTreeRoot());
        }
        if (DebugFlags.process_misc) {
            element_renderer.setup(frustum_state, currentTime);
            element_renderer.visit(world.getElementRoot());
        }

        if (DebugFlags.process_misc) {
            var renderState = element_renderer.getRenderState();
            emitterRenderer.prepare(render_queues, renderState.getEmitterQueue(), frustum_state, modelViewStack);
            lightningRenderer.prepare(renderState.getLightningQueue());
            sonicBlastRenderer.prepare(renderState.getSonicBlastQueue());
        }
        sprite_sorter.distributeModels();
    }

    private void renderEnvironment(RenderContext context, CameraState frustum_state, float currentTime) {
        try (var _ = context.withDrawBuffers(false)) {
            if (DebugFlags.draw_sky) {
                var gpu = context.gpuTimer();
                gpu.begin(GpuPass.SKY);
                sky.render(context, frustum_state, modelViewStack, projectionStack, currentTime);
                seaBottom.render(context, frustum_state, modelViewStack, projectionStack);
                gpu.end();
            }

            if (DebugFlags.process_landscape) {
                var gpu = context.gpuTimer();
                gpu.begin(GpuPass.LANDSCAPE);
                landscape_renderer.prepareAll(frustum_state, false);
                landscape_renderer.render(context, frustum_state, modelViewStack, projectionStack);
                gpu.end();
            }
        }
    }

    private void renderOpaqueGeometry(RenderContext context, CameraState frustum_state, GUIRoot gui_root,
            float currentTime) {
        var gpu = context.gpuTimer();
        if (DebugFlags.process_shadows && (cheat == null || cheat.draw_shadows)) {
            gpu.begin(GpuPass.SHADOWS);
            if (DebugFlags.process_trees) {
                tree_renderer.renderShadows(element_renderer.getRenderState().getDefaultShadowRenderer(), currentTime);
            }
            render_queues.renderShadows(context, (float) world.getHeightMap().getMetersPerWorld(),
                    landscape_renderer.getHeightMapVisual().getHeightTexture(), modelViewStack, projectionStack);
            gpu.end();
        }

        if (DebugFlags.process_trees) {
            gpu.begin(GpuPass.TREES);
            tree_renderer.render(context, frustum_state, modelViewStack, projectionStack, currentTime);
            gpu.end();
        }
        if (DebugFlags.process_misc) {
            gpu.begin(GpuPass.UNITS);
            render_queues.renderAll(context, frustum_state, projectionStack);
            gpu.end();

            gpu.begin(GpuPass.TREES);
            treeSpriteRenderer.renderAll(context, frustum_state, projectionStack);
            gpu.end();

            gpu.begin(GpuPass.PLANTS);
            render_queues.renderPlants(context, frustum_state, projectionStack);
            gpu.end();

            render_queues.renderNoDetail();
        }

        if (gui_root.getDelegate() instanceof Delegate delegate) {
            delegate.render3D(context, landscape_renderer, render_queues, frustum_state, modelViewStack,
                    projectionStack);
        }

        if (DebugFlags.debugRenderingEnabled()) {
            renderDebugElements(frustum_state);
        }
    }

    private void renderWater(RenderContext context, CameraState frustum_state, float currentTime) {
        if (DebugFlags.draw_water) {
            var gpu = context.gpuTimer();
            gpu.begin(GpuPass.WATER);
            water.render(context, frustum_state, landscape_renderer.getVisiblePatches(), currentTime);
            gpu.end();
        }
    }

    private void renderTransparents(RenderContext context, CameraState frustum_state) {
        if (!DebugFlags.process_misc && !DebugFlags.draw_particles) {
            return;
        }

        var gpu = context.gpuTimer();
        gpu.begin(GpuPass.EFFECTS);
        if (DebugFlags.process_misc) {
            render_queues.renderBlends(context, frustum_state, projectionStack);
        }

        try (var _ = context.withDrawBuffers(false)) {
            if (emitterRenderer.hasVisibleParticles()) {
                postProcessor.copyDepthBuffer();
            }

            lightningRenderer.render(context, render_queues, frustum_state, modelViewStack, projectionStack);
            emitterRenderer.render(context, render_queues, frustum_state, modelViewStack, projectionStack, postProcessor
                    .getDepthCopyTexture());
            sonicBlastRenderer.render(context, render_queues, frustum_state, modelViewStack, projectionStack);
        }
        gpu.end();
    }

    private void renderOverlays(RenderContext context, CameraState frustum_state) {
        renderRallyPoint(context, frustum_state);
        render_queues.getInstancedRenderer().renderAll(context, frustum_state, projectionStack);

        assert ShaderProgram.activeShader() == null : "Shader still active=" + ShaderProgram.activeShader();

        if (DebugFlags.line_mode || (cheat != null && cheat.line_mode)) {
            GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
        }

        if (DebugFlags.debugRenderingEnabled()) {
            context.validate();
        }
    }

    private boolean closed = false;

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public void close() {
        if (!closed) {
            closed = true;
            ambient.close();
            element_renderer.close();
            lightningRenderer.close();
            sonicBlastRenderer.close();
            emitterRenderer.close();
            sky.close();
            seaBottom.close();
            water.close();
            tree_renderer.close();
            landscape_renderer.close();
            treeSpriteRenderer.close();
            postProcessor.close();
        }
    }
}
