package com.oddlabs.tt.engine.render;


import com.oddlabs.tt.base.animation.Animated;
import com.oddlabs.tt.base.animation.AnimationManager;
import com.oddlabs.tt.engine.render.scenery.Sky;
import com.oddlabs.tt.engine.render.scenery.Water;
import com.oddlabs.tt.engine.render.shader.LandscapeShader;
import com.oddlabs.tt.engine.render.state.BlendMode;
import com.oddlabs.tt.engine.render.state.CullMode;
import com.oddlabs.tt.engine.render.state.DepthMode;
import com.oddlabs.tt.engine.render.state.RenderContext;
import com.oddlabs.tt.engine.resource.WorldInfo;
import com.oddlabs.tt.engine.vbo.FloatVBO;
import com.oddlabs.tt.procedural.LandscapeConfig;
import com.oddlabs.tt.simulation.landscape.AbstractPatchGroup;
import com.oddlabs.tt.simulation.landscape.HeightMap;
import com.oddlabs.tt.simulation.landscape.LandscapeLeaf;
import com.oddlabs.tt.simulation.landscape.PatchGroup;
import com.oddlabs.tt.simulation.landscape.World;
import com.oddlabs.util.Color;
import org.jspecify.annotations.Nullable;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL33;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Renders the 3D terrain landscape.
 */
public final class LandscapeRenderer implements SceneRenderer, Animated {
    private final List<LandscapeLeaf> render_list = new ArrayList<>();
    private final World world;
    private final Texture diffuseMap;
    private final Texture normalMap;
    private final Texture detailMap;
    private final Texture detailNormalMap;
    private final PatchMesh patchMesh = new PatchMesh();
    private final LandscapeShader shader = new LandscapeShader();
    private final HeightMapVisual heightMapVisual;
    private @Nullable Water water;
    private FloatVBO instanceVBO = new FloatVBO(GL15.GL_STREAM_DRAW, 1024 * 3);
    private FloatBuffer instanceBuffer = BufferUtils.createFloatBuffer(1024 * 3);

    public HeightMapVisual getHeightMapVisual() {
        return heightMapVisual;
    }

    public LandscapeShader getShader() {
        return shader;
    }

    public void setWater(Water water) {
        this.water = water;
    }

    public LandscapeRenderer(World world, WorldInfo<Texture> world_info,
            AnimationManager manager) {
        this.world = world;
        this.heightMapVisual = new HeightMapVisual(world.getHeightMap());
        this.diffuseMap = world_info.maps().diffuse();
        this.normalMap = world_info.maps().normal();
        this.detailMap = world_info.detail();
        this.detailNormalMap = world_info.detailNormal();

        manager.registerAnimation(this);
    }

    public Collection<LandscapeLeaf> getVisiblePatches() {
        return render_list;
    }

    public HeightMap getHeightMap() {
        return world.getHeightMap();
    }

    public void pick(CameraState camera, boolean visible_override, Set<LandscapeLeaf> set) {
        doPrepareAll(camera, visible_override, set);
    }

    public void prepareAll(CameraState camera, boolean visible_override) {
        render_list.clear();
        doPrepareAll(camera, visible_override, render_list);
    }

    private void doPrepareAll(CameraState camera, final boolean visible_override, Collection<
            LandscapeLeaf> result) {
        traverse(world.getPatchRoot(), camera, visible_override, result);
    }

    private void traverse(AbstractPatchGroup node, CameraState camera, boolean visible_override,
            Collection<LandscapeLeaf> result) {
        switch (node) {
            case PatchGroup group -> {
                RenderTools.FrustumIntersection frustum_state = RenderTools.FrustumIntersection.ALL_OUTSIDE;
                if (visible_override || (frustum_state = RenderTools.inFrustum(group, camera.getFrustum()))
                        != RenderTools.FrustumIntersection.ALL_OUTSIDE) {
                    boolean next_visible_override = visible_override || frustum_state
                            == RenderTools.FrustumIntersection.ALL_INSIDE;
                    for (AbstractPatchGroup child : group.children()) {
                        traverse(child, camera, next_visible_override, result);
                    }
                }
            }
            case LandscapeLeaf leaf -> {
                if (visible_override || RenderTools.inFrustum(leaf, camera.getFrustum())
                        != RenderTools.FrustumIntersection.ALL_OUTSIDE) {
                    result.add(leaf);
                }
            }
        }
    }

    @Override
    public void render(RenderContext context, CameraState state, MatrixStack modelViewStack,
            MatrixStack projectionStack) {
        try (var _ = shader.use(); var _ = context.withBlendMode(BlendMode.ALPHA); var _ = context.withDepthMode(
                DepthMode.READ_WRITE); var _ = context.withCullMode(CullMode.NONE)) {

            // Set VTF Uniforms
            shader.setUniform(LandscapeShader.Uniforms.WORLD_SIZE, (float) world.getHeightMap().getMetersPerWorld());
            shader.setUniform(LandscapeShader.Uniforms.DETAIL_SCALE, LandscapeConfig.LANDSCAPE_DETAIL_REPEAT_RATE);

            Color stdColor = Sky.SEA_BOTTOM_COLOR.get(world.getTerrainType());
            shader.setUniformColor3(LandscapeShader.Uniforms.SEA_BOTTOM_COLOR, stdColor);

            context.setTexture(0, diffuseMap);
            shader.setUniform(LandscapeShader.Uniforms.DIFFUSE_MAP, 0);

            context.setTexture(1, normalMap);
            shader.setUniform(LandscapeShader.Uniforms.NORMAL_MAP, 1);

            context.setTexture(2, detailMap);
            shader.setUniform(LandscapeShader.Uniforms.DETAIL_MAP, 2);

            context.setTexture(3, heightMapVisual.getHeightTexture());
            shader.setUniform(LandscapeShader.Uniforms.HEIGHT_MAP, 3);

            context.setTexture(4, detailNormalMap);
            shader.setUniform(LandscapeShader.Uniforms.DETAIL_NORMAL_MAP, 4);

            if (DebugFlags.draw_landscape && !render_list.isEmpty()) {
                int instanceCount = render_list.size();
                int requiredFloats = instanceCount * 3;

                // Resize buffer if needed
                if (instanceBuffer.capacity() < requiredFloats) {
                    int newCapacity = Math.max(instanceBuffer.capacity() * 2, requiredFloats);
                    instanceBuffer = BufferUtils.createFloatBuffer(newCapacity);
                    instanceVBO.close();
                    instanceVBO = new FloatVBO(GL15.GL_STREAM_DRAW, newCapacity);
                }

                instanceBuffer.clear();
                float patchSize = world.getHeightMap().getMetersPerPatch();
                int patchesPerWorld = world.getHeightMap().getPatchesPerWorld();
                Water activeWater = water;
                BitSet activeOceanPatches = activeWater != null ? activeWater.getOceanPatches() : null;
                for (LandscapeLeaf leaf : render_list) {
                    int px = leaf.getPatchX();
                    int py = leaf.getPatchY();
                    float waveScale = (activeOceanPatches != null && activeOceanPatches.get(py * patchesPerWorld + px))
                            ? 1.0f : 0.0f;
                    instanceBuffer.put(px * patchSize);
                    instanceBuffer.put(py * patchSize);
                    instanceBuffer.put(waveScale);
                }
                instanceBuffer.flip();

                instanceVBO.bind(context);
                GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, instanceBuffer);

                patchMesh.bind();

                // Setup instance attribute (Location 4: in_InstancePatchOffset)
                int offsetLoc = 4; // Hardcoded location from shader layout
                GL20.glEnableVertexAttribArray(offsetLoc);
                GL20.glVertexAttribPointer(offsetLoc, 3, GL11.GL_FLOAT, false, 0, 0);
                GL33.glVertexAttribDivisor(offsetLoc, 1);

                patchMesh.drawInstanced(instanceCount);

                // Cleanup instance attribute
                GL33.glVertexAttribDivisor(offsetLoc, 0);
                GL20.glDisableVertexAttribArray(offsetLoc);

                patchMesh.unbind();
                // Unbind instance VBO to avoid leaking
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
            }

            context.setActiveTexture(0);
        }
    }

    public void debugRender(CameraState frustum_state) {
        if (DebugFlags.isBoundsEnabled(BoundingMode.LANDSCAPE)) {
            for (LandscapeLeaf patch : render_list) {
                RenderTools.draw(patch, BoundingMode.LANDSCAPE, 1f, 0f, 0f);
            }
        }
    }

    @Override
    public void animate(float t) {
        // No animation needed for static VTF geometry
    }
}
