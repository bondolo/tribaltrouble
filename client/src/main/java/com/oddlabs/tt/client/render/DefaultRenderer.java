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
import com.oddlabs.tt.engine.render.LandscapeRenderer;
import com.oddlabs.tt.engine.render.MatrixStack;
import com.oddlabs.tt.engine.render.PostProcessor;
import com.oddlabs.tt.engine.render.DebugFlags;
import com.oddlabs.tt.engine.render.RenderQueues;
import com.oddlabs.tt.engine.render.SpriteKey;
import com.oddlabs.tt.engine.render.SpriteRenderer;
import com.oddlabs.tt.engine.render.Texture;
import com.oddlabs.tt.engine.render.scenery.Sky;
import com.oddlabs.tt.engine.render.scenery.Water;
import com.oddlabs.tt.engine.render.shader.DebugMeshShader;
import com.oddlabs.tt.engine.render.shader.DebugShaderRenderer;
import com.oddlabs.tt.engine.render.shader.ShaderProgram;
import com.oddlabs.tt.engine.render.state.GlobalUniforms;
import com.oddlabs.tt.engine.render.state.RenderContext;
import com.oddlabs.tt.engine.resource.AssetRegistry;
import com.oddlabs.tt.engine.resource.WorldInfo;
import com.oddlabs.tt.engine.settings.Settings;
import com.oddlabs.tt.engine.util.DebugRender;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.ToolTip;
import com.oddlabs.tt.gui.render.UIRenderer;
import com.oddlabs.tt.simulation.landscape.World;
import com.oddlabs.tt.simulation.model.Building;
import com.oddlabs.tt.simulation.model.Target;
import com.oddlabs.tt.simulation.model.Unit;
import com.oddlabs.tt.simulation.player.Player;
import com.oddlabs.util.Color;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

/**
 * The primary world renderer responsible for coordinating the rendering
 * of the landscape, units, buildings, and transient effects.
 */
public final class DefaultRenderer implements UIRenderer, AutoCloseable {
    private final Picker picker;
    private final Water water;
    private final Sky sky;
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

    private @Nullable Building selected_building;

    public DefaultRenderer(RenderContext renderContext, @Nullable Cheat cheat, Player local_player,
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
        this.sprite_sorter = new SpriteSorter(settings.graphic_detail);
        this.element_renderer = new ElementRenderer<>(local_player, render_queues, picker, false, sprite_sorter,
                selection, audioManager);
        this.tree_renderer = new TreeRenderer(cheat, sprite_sorter, picker.getRespondManager(), treeSpriteRenderer);
        this.landscape_renderer = landscape_renderer;
        this.sky = new Sky(landscape_renderer, world_info.landscapeData().terrain(), world_info.detail(), world_info
                .detailNormal());
        this.modelViewStack = modelViewStack;
        this.projectionStack = projectionStack;
        this.water = new Water(landscape_renderer.getHeightMapVisual(), world.getHeightMap(), world_info.landscapeData()
                .terrain(), sky, modelViewStack);
        this.landscape_renderer.setWater(this.water);
        this.lightningRenderer = new LightningRenderer();
        this.sonicBlastRenderer = new SonicBlastRenderer();
        this.emitterRenderer = new EmitterRenderer();
        this.postProcessor = new PostProcessor(renderContext, settings.accessibility, width, height);
        DebugRender.setShaderRenderer(new DebugShaderRenderer(
                new DebugMeshShader(), modelViewStack, projectionStack, renderContext
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

        Matrix4f modelMatrix = new Matrix4f();
        float dx = camera_state.getCurrentX() - x;
        float dy = camera_state.getCurrentY() - y;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len > 0.1f) {
            float angle = (float) Math.atan2(dy / len, dx / len);
            modelMatrix.translation(x, y, z).rotate(angle, 0f, 0f, 1f);
        } else {
            modelMatrix.translation(x, y, z);
        }

        rally_point_renderer.addInstance(
                0, // spriteIndex
                0, // animation
                0f, // animTicks
                false, // respond
                true,  // blend
                true,  // depthWrite
                true,  // depthTest
                modelMatrix,
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

    private TreeRenderer getTreeRenderer() {
        return tree_renderer;
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
    public void startFrame(RenderContext context) {
        postProcessor.bindSceneFBO(context);
        context.clear(true, true);
    }

    @Override
    public void endFrame(RenderContext context, Consumer<RenderContext> guiRenderCallback) {
        postProcessor.renderComposite(context, guiRenderCallback);
    }

    @Override
    public void render(RenderContext context,
            CameraState frustum_state, GUIRoot gui_root) {
        treeSpriteRenderer.clear();
        render_queues.getInstancedRenderer().clear();

        postProcessor.resize(context, frustum_state.getWidth(), frustum_state.getHeight());
        postProcessor.bindSceneFBO(context);
        context.setDrawBuffers(true); // Ensure both Color and Mask are cleared
        context.setColorMask(true, true, true, true);
        context.setDepthMask(true);
        context.setDepthTest(true);
        context.setDepthFunc(GL11.GL_LEQUAL);
        context.clear(true, true);

        context.setViewport(0, 0, frustum_state.getWidth(), frustum_state.getHeight());

        float currentTime = gui_root.getTime();
        // Update Global UBO
        try (var stack = MemoryStack.stackPush()) {
            ByteBuffer buf = stack.malloc(512);
            globalUniforms.update(frustum_state, currentTime,
                    world.getHeightMap().getSeaLevelMeters(), water, buf);
            buf.flip();
            context.updateGlobalState(buf);
        }

        ambient.updateSoundListener(frustum_state, world.getHeightMap());
        modelViewStack.current().set(frustum_state.getModelView());
        projectionStack.current().set(frustum_state.getProjectionMatrix());

        if (DebugFlags.line_mode || (cheat != null && cheat.line_mode)) {
            GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE);
        }

        // Sky & Landscape don't write to mask -> Disable Mask Buffer
        context.setDrawBuffers(false);

        if (DebugFlags.draw_sky) {
            sky.render(context, frustum_state, modelViewStack, projectionStack, currentTime);
            sky.renderSeaBottom(context, frustum_state, modelViewStack, projectionStack);
        }

        if (DebugFlags.process_landscape) {
            landscape_renderer.prepareAll(frustum_state, false);
            landscape_renderer.render(context, frustum_state, modelViewStack, projectionStack);
        }
        // Trees & Units write to mask -> Enable Mask Buffer
        context.setDrawBuffers(true);

        if (DebugFlags.process_trees) {
            tree_renderer.setup(frustum_state);
            tree_renderer.visit(world.getTreeRoot());
        }
        if (DebugFlags.process_misc) {
            element_renderer.setup(frustum_state, currentTime);
            element_renderer.visit(world.getElementRoot());
        }

        // Process transient effects (smoke, lightning, fragments) immediately after visitation.
        if (DebugFlags.process_misc) {
            var renderState = element_renderer.getRenderState();
            emitterRenderer.prepare(render_queues, renderState.getEmitterQueue(), frustum_state,
                    modelViewStack);
            lightningRenderer.prepare(renderState.getLightningQueue());
            sonicBlastRenderer.prepare(renderState.getSonicBlastQueue());
        }
        sprite_sorter.distributeModels();
        if (DebugFlags.process_shadows) {
            render_queues.renderShadows(context, landscape_renderer, modelViewStack, projectionStack);
            if (DebugFlags.process_trees) {
                tree_renderer.renderShadows(element_renderer.getRenderState().getDefaultShadowRenderer());
            }
        }

        if (DebugFlags.process_trees) {
            tree_renderer.render(context, frustum_state, modelViewStack, projectionStack, currentTime);
        }
        if (DebugFlags.process_misc) {
            render_queues.renderAll(context, frustum_state, projectionStack);

            // Render trees AFTER opaque units/misc.
            // Trees use Alpha-to-Coverage with Depth-Write enabled.
            // Separate renderer ensures they are flushed here.
            treeSpriteRenderer.renderAll(context, frustum_state, projectionStack);

            render_queues.renderPlants(context, frustum_state, projectionStack);

            render_queues.renderNoDetail();
        }

        if (gui_root.getDelegate() instanceof Delegate delegate) {
            delegate.render3D(context, landscape_renderer, render_queues, frustum_state, modelViewStack,
                    projectionStack);
        }

        if (DebugFlags.debugRenderingEnabled()) {
            renderDebugElements(frustum_state);
        }

        // Enable Mask Buffer for Water interaction (occluding submerged unit outlines)
        context.setDrawBuffers(true);

        if (DebugFlags.draw_water) {
            water.render(context, frustum_state, landscape_renderer.getVisiblePatches(), currentTime);
        }

        if (DebugFlags.process_misc)
            render_queues.renderBlends(context, frustum_state, projectionStack);

        // Water & Particles don't write to mask -> Disable Mask Buffer
        context.setDrawBuffers(false);

        // Copy depth buffer for Soft Particles (smoke/effects)
        postProcessor.copyDepthBuffer(context);

        // Render transient effects (smoke, lightning) AFTER all other scene objects.
        // This ensures they are depth-tested against the complete scene (including water and blended units).
        lightningRenderer.render(context, render_queues, frustum_state, modelViewStack, projectionStack);
        emitterRenderer.render(context, render_queues, frustum_state, modelViewStack, projectionStack, postProcessor
                .getDepthCopyTexture());
        sonicBlastRenderer.render(context, render_queues, frustum_state, modelViewStack, projectionStack);
        // Rally point uses SpriteShader (Mask) -> Enable
        context.setDrawBuffers(true);
        renderRallyPoint(context, frustum_state);
        render_queues.getInstancedRenderer().renderAll(context, frustum_state, projectionStack);

        assert ShaderProgram.activeShader() == null : "Shader still active=" + ShaderProgram.activeShader();

        if (DebugFlags.line_mode || (cheat != null && cheat.line_mode)) {
            GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
        }

        // Ensure Mask is enabled for GUI clearing
        context.setDrawBuffers(true);

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
            lightningRenderer.close();
            sonicBlastRenderer.close();
            emitterRenderer.close();
            sky.close();
            water.close();
            tree_renderer.close();
            treeSpriteRenderer.close();
            postProcessor.close();
        }
    }
}
