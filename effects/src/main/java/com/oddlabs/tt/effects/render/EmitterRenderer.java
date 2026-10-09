package com.oddlabs.tt.effects.render;


import com.oddlabs.tt.effects.particle.Emitter;
import com.oddlabs.tt.effects.particle.Particle;
import com.oddlabs.tt.engine.render.BoundingMode;
import com.oddlabs.tt.engine.render.CameraState;
import com.oddlabs.tt.engine.render.DebugFlags;
import com.oddlabs.tt.engine.render.MatrixStack;
import com.oddlabs.tt.engine.render.PolyDetail;
import com.oddlabs.tt.engine.render.RenderConfig;
import com.oddlabs.tt.engine.render.RenderQueues;
import com.oddlabs.tt.engine.render.SpriteKey;
import com.oddlabs.tt.engine.render.SpriteRenderer;
import com.oddlabs.tt.engine.render.Texture;
import com.oddlabs.tt.engine.render.TextureKey;
import com.oddlabs.tt.engine.render.shader.VertexLayout;
import com.oddlabs.tt.engine.render.state.BlendMode;
import com.oddlabs.tt.engine.render.state.DepthMode;
import com.oddlabs.tt.engine.render.state.RenderContext;
import com.oddlabs.tt.engine.vbo.FloatVBO;
import com.oddlabs.tt.engine.vbo.VertexArray;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL33;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

/**
 * Renders instanced particle systems from particle emitters.
 */
public final class EmitterRenderer implements AutoCloseable {
    private static final int MAX_PARTICLES = 50000;
    private static final float DEFAULT_SOFT_RANGE = 2.0f;

    private static final VertexLayout<ParticleShader.Attribute> VERTEX_LAYOUT = new VertexLayout<>(
            ParticleShader.Attribute.CENTER_POSITION,
            ParticleShader.Attribute.SIZE,
            ParticleShader.Attribute.COLOR,
            ParticleShader.Attribute.ANGLE,
            ParticleShader.Attribute.TEX_SLOT
    );

    private final FloatBuffer particle_buffer;
    private final FloatVBO particle_vbo;

    private final ParticleShader shader = new ParticleShader();

    private final VertexArray vao = new VertexArray();
    private int vbo_offset = 0;
    private final Matrix4f cachedViewMatrix = new Matrix4f();

    private static final class BatchGroup {
        int srcBlend;
        int dstBlend;
        boolean fogEnabled;
        final List<BatchEntry> entries = new ArrayList<>();

        boolean matches(int srcBlend, int dstBlend, boolean fogEnabled) {
            return this.srcBlend == srcBlend && this.dstBlend == dstBlend && this.fogEnabled == fogEnabled;
        }

        void reset(int srcBlend, int dstBlend, boolean fogEnabled) {
            this.srcBlend = srcBlend;
            this.dstBlend = dstBlend;
            this.fogEnabled = fogEnabled;
            this.entries.clear();
        }
    }

    private static final class BatchEntry {
        @Nullable
        Emitter<?> emitter;
        @Nullable
        List<? extends Particle> particles;
        @Nullable
        Texture texture;

        void set(Emitter<?> emitter, List<? extends Particle> particles, Texture texture) {
            this.emitter = emitter;
            this.particles = particles;
            this.texture = texture;
        }

        void clear() {
            this.emitter = null;
            this.particles = null;
            this.texture = null;
        }
    }

    private final List<BatchGroup> activeGroups = new ArrayList<>();
    private final List<BatchGroup> groupPool = new ArrayList<>();
    private final List<BatchEntry> entryPool = new ArrayList<>();
    private int entryPoolIndex = 0;

    public EmitterRenderer() {
        int floatsPerParticle = VERTEX_LAYOUT.getStride() / Float.BYTES;
        particle_buffer = BufferUtils.createFloatBuffer(MAX_PARTICLES * floatsPerParticle);
        particle_vbo = new FloatVBO(GL15.GL_STREAM_DRAW, particle_buffer.capacity());

        vao.bind();
        particle_vbo.bind();
        VERTEX_LAYOUT.bind(shader);

        // Configure all attributes as instance attributes
        for (ParticleShader.Attribute attr : ParticleShader.Attribute.values()) {
            int loc = shader.getAttributeLocation(attr.getName());
            if (loc >= 0) {
                GL33.glVertexAttribDivisor(loc, 1);
            }
        }

        vao.unbind();
    }

    private BatchGroup getGroup(int srcBlend, int dstBlend, boolean fogEnabled) {
        for (int i = 0; i < activeGroups.size(); i++) {
            BatchGroup g = activeGroups.get(i);
            if (g.matches(srcBlend, dstBlend, fogEnabled)) {
                return g;
            }
        }
        BatchGroup g;
        if (!groupPool.isEmpty()) {
            g = groupPool.removeLast();
        } else {
            g = new BatchGroup();
        }
        g.reset(srcBlend, dstBlend, fogEnabled);
        activeGroups.add(g);
        return g;
    }

    private BatchEntry obtainEntry(Emitter<?> emitter, List<? extends Particle> particles, Texture texture) {
        BatchEntry entry;
        if (entryPoolIndex < entryPool.size()) {
            entry = entryPool.get(entryPoolIndex++);
        } else {
            entry = new BatchEntry();
            entryPool.add(entry);
            entryPoolIndex++;
        }
        entry.set(emitter, particles, texture);
        return entry;
    }

    public void clear() {
        for (int i = 0; i < activeGroups.size(); i++) {
            BatchGroup g = activeGroups.get(i);
            g.entries.clear();
            groupPool.add(g);
        }
        activeGroups.clear();
        for (int i = 0; i < entryPoolIndex; i++) {
            entryPool.get(i).clear();
        }
        entryPoolIndex = 0;
    }

    /**
     * Returns true if there are visible particles batched for rendering this frame.
     */
    public boolean hasVisibleParticles() {
        return !activeGroups.isEmpty();
    }

    public void prepare(RenderQueues render_queues, Queue<? extends Emitter<?>> emitters,
            CameraState state, MatrixStack modelViewStack) {
        clear();
        cachedViewMatrix.set(modelViewStack.current());
        if (DebugFlags.draw_particles)
            for (Emitter<?> emitter : emitters) {
                collectParticles(render_queues, emitter, state);
            }
    }

    public void render(RenderContext context, RenderQueues render_queues, CameraState state,
            MatrixStack modelViewStack, MatrixStack projectionStack, Texture depthTexture) {
        if (activeGroups.isEmpty()) return;

        // Reset offset and orphan at start of frame to prevent flickering
        vbo_offset = 0;
        particle_vbo.orphan();

        vao.bind();
        try (var _ = shader.use(); var _ = context.withBlendMode(BlendMode.ALPHA); var _ = context.withDepthMode(
                DepthMode.READ_ONLY)) {

            shader.setUniform(shader.locModelViewMatrix, modelViewStack.current());

            // Bind global effect texture array to unit 2
            context.setTexture(2, render_queues.getEffectTextureArray().getHandle(),
                    render_queues.getEffectTextureArray().getTarget());
            shader.setUniform(shader.locTextureArray, 2);

            context.setActiveTexture(1);
            context.setTexture(1, depthTexture.getHandle());
            shader.setUniform(shader.locDepthMap, 1);

            shader.setUniform(shader.locNearFar, RenderConfig.VIEW_MIN, RenderConfig.VIEW_MAX);
            shader.setUniform(shader.locSoftRange, DEFAULT_SOFT_RANGE);

            flushBatches(context);
        } finally {
            vao.unbind();
            context.setTexture(1, null);
            context.setTexture(2, null);
        }
    }

    private void renderParticle(Particle particle, Emitter<?> emitter, float layer) {
        particle_buffer.put(particle.getPosX()).put(particle.getPosY()).put(particle.getPosZ()); // World Position
        particle_buffer.put(particle.getRadiusX() * emitter.getScaleX()).put(particle.getRadiusY() * emitter
                .getScaleY()).put(particle.getRadiusZ() * emitter.getScaleZ()); // Size (3D)

        particle_buffer.put(particle.getColorR()).put(particle.getColorG()).put(particle.getColorB())
                .put(particle.getColorA());

        particle_buffer.put(particle.getAngle());
        particle_buffer.put(layer);
    }

    private <P extends Particle> void collectParticles(RenderQueues render_queues, Emitter<P> emitter,
            CameraState state) {
        if (!state.inNoDetailMode() && !emitter.getBounds().testFrustum(state.getFrustum())) {
            return;
        }

        TextureKey[] textures = emitter.getTextures();
        List<P>[] particles = emitter.getParticles();
        SpriteKey[] sprite_renderers = emitter.getSpriteRenderers();

        if (textures != null) {
            for (int j = 0; j < particles.length; j++) {
                List<P> pList = particles[j];
                if (pList.isEmpty()) continue;
                Texture texture = render_queues.getTexture(textures[j]);
                BatchGroup group = getGroup(emitter.getSrcBlendFunc(), emitter.getDstBlendFunc(), emitter
                        .isFogEnabled());
                group.entries.add(obtainEntry(emitter, pList, texture));
            }
        } else if (sprite_renderers != null) {
            for (int j = 0; j < particles.length; j++) {
                SpriteRenderer renderer = render_queues.getRenderer(sprite_renderers[j]);
                List<P> pList = particles[j];
                for (int k = 0; k < pList.size(); k++) {
                    Particle particle = pList.get(k);
                    renderer.addToRenderList(PolyDetail.LOW_POLY, new ParticleModelState(particle, cachedViewMatrix),
                            false);
                }
            }
        }
    }

    private void flushBatches(RenderContext context) {
        int floatsPerParticle = VERTEX_LAYOUT.getStride() / Float.BYTES;

        for (int i = 0; i < activeGroups.size(); i++) {
            BatchGroup group = activeGroups.get(i);
            context.setBlendFunc(group.srcBlend, group.dstBlend);
            shader.setUniform(shader.locIsAdditive, group.dstBlend == GL11.GL_ONE ? 1.0f : 0.0f);
            shader.setUniform(shader.locFogEnabled, group.fogEnabled);

            particle_buffer.clear();
            int particleCount = 0;

            for (int j = 0; j < group.entries.size(); j++) {
                BatchEntry batchEntry = group.entries.get(j);
                float layer = (float) batchEntry.texture.getLayer();
                particleCount = processBatchEntry(batchEntry, layer, particleCount, floatsPerParticle);
            }
            flush(particleCount);
        }
    }

    private int processBatchEntry(BatchEntry batch, float layer, int particleCount,
            int floatsPerParticle) {
        List<? extends Particle> particles = batch.particles;
        Emitter<?> emitter = batch.emitter;
        if (particles == null || emitter == null) return particleCount;

        // Iterate backwards as per original logic
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle particle = particles.get(i);
            if (particleCount >= MAX_PARTICLES || particle_buffer.remaining() < floatsPerParticle) {
                flush(particleCount);
                particle_buffer.clear();
                particleCount = 0;
            }
            renderParticle(particle, emitter, layer);
            particleCount++;
        }
        return particleCount;
    }

    private void flush(int particleCount) {
        if (particleCount == 0) return;
        particle_buffer.flip();

        if (vbo_offset + particleCount > MAX_PARTICLES) {
            // This case should be rare since we reset at start of frame
            particle_vbo.orphan();
            vbo_offset = 0;
        }

        int stride = VERTEX_LAYOUT.getStride();
        int floatsPerParticle = stride / Float.BYTES;
        particle_vbo.putSubData(vbo_offset * floatsPerParticle, particle_buffer);

        // Shifting attribute pointers to account for vbo_offset since we use instanced rendering
        for (ParticleShader.Attribute attr : ParticleShader.Attribute.values()) {
            int loc = shader.getAttributeLocation(attr.getName());
            if (loc >= 0) {
                attr.setPointer(loc, stride, VERTEX_LAYOUT.getOffset(attr) + vbo_offset * stride);
            }
        }

        GL31.glDrawArraysInstanced(GL11.GL_TRIANGLE_STRIP, 0, 4, particleCount);

        vbo_offset += particleCount;
    }

    public void debugRender(Queue<Emitter<?>> emitter_queue) {
        if (DebugFlags.isBoundsEnabled(BoundingMode.PLAYERS)) {
            for (Emitter<?> emitter : emitter_queue) {
                emitter.debugRender();
            }
        }
    }

    @Override
    public void close() {
        vao.close();
        particle_vbo.close();
        shader.close();
    }
}
