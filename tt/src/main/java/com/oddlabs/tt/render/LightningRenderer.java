package com.oddlabs.tt.render;

import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.global.Globals;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import com.oddlabs.tt.render.shader.LightningShader;
import com.oddlabs.tt.render.shader.VertexLayout;
import com.oddlabs.tt.render.state.BlendMode;
import com.oddlabs.tt.render.state.CullMode;
import com.oddlabs.tt.render.state.DepthMode;
import com.oddlabs.tt.render.state.RenderContext;
import com.oddlabs.tt.render.vbo.FloatVBO;
import com.oddlabs.tt.render.vbo.ShortVBO;
import com.oddlabs.tt.render.vbo.VertexArray;
import org.joml.Matrix4fc;
import org.jspecify.annotations.NonNull;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.List;
import java.util.Objects;

/**
 * Specialized renderer for handling transient lightning effects.
 */
public final class LightningRenderer implements AutoCloseable {
    private static final int MAX_PARTICLES = 100;
    private static final int VERTICES_PER_PARTICLE = 8;
    private static final int INDICES_PER_PARTICLE = 12; // 4 triangles * 3 indices
    private static final int FLOATS_PER_VERTEX = 9; // x,y,z,u,v,r,g,b,a
    private static final VertexLayout<LightningShader.Attribute> LAYOUT = new VertexLayout<>(
            LightningShader.Attribute.POSITION,
            LightningShader.Attribute.TEX_COORD,
            LightningShader.Attribute.COLOR
    );

    private final @NonNull FloatBuffer particle_buffer = Objects.requireNonNull(BufferUtils.createFloatBuffer(
            MAX_PARTICLES * VERTICES_PER_PARTICLE * FLOATS_PER_VERTEX));
    private final @NonNull FloatVBO particle_vbo = new FloatVBO(GL15.GL_STREAM_DRAW, particle_buffer.capacity());
    private final @NonNull ShortVBO particle_ibo;

    private final @NonNull LightningShader shader;
    private final VertexArray vao = new VertexArray();
    private int vbo_offset = 0;

    public LightningRenderer() {
        shader = new LightningShader();

        ShortBuffer iboBuffer = Objects.requireNonNull(BufferUtils.createShortBuffer(MAX_PARTICLES
                * INDICES_PER_PARTICLE));
        for (int i = 0; i < MAX_PARTICLES; i++) {
            int offset = i * VERTICES_PER_PARTICLE;
            // First quad
            iboBuffer.put((short) (offset + 0)).put((short) (offset + 1)).put((short) (offset + 2))
                    .put((short) (offset + 2)).put((short) (offset + 3)).put((short) (offset + 0));
            // Second quad
            iboBuffer.put((short) (offset + 4)).put((short) (offset + 5)).put((short) (offset + 6))
                    .put((short) (offset + 6)).put((short) (offset + 7)).put((short) (offset + 4));
        }
        iboBuffer.flip();
        particle_ibo = new ShortVBO(GL15.GL_STATIC_DRAW, iboBuffer);

        vao.bind();
        particle_vbo.bind();
        particle_ibo.bind();
        LAYOUT.bind(shader);
        vao.unbind();
    }

    private final List<com.oddlabs.tt.render.particle.@NonNull Lightning> activeLightnings = new ArrayList<>();

    public void prepare(@NonNull Collection<com.oddlabs.tt.render.particle.@NonNull Lightning> list) {
        activeLightnings.clear();
        activeLightnings.addAll(list);
    }

    public void render(@NonNull RenderContext context, @NonNull RenderQueues render_queues, @NonNull CameraState state,
            @NonNull MatrixStack modelViewStack, @NonNull MatrixStack projectionStack) {
        if (activeLightnings.isEmpty()) return;

        // Reset offset and orphan at start of frame to prevent flickering
        vbo_offset = 0;
        particle_vbo.orphan();

        try (var _ = shader.use(); var _ = context.withBlendMode(BlendMode.ADDITIVE); var _ = context.withDepthMode(
                DepthMode.READ_ONLY); var _ = context.withCullMode(CullMode.NONE)) {

            Matrix4fc mv = modelViewStack.current();
            shader.setUniform(LightningShader.Uniforms.MODEL_VIEW_MATRIX, mv);

            shader.setUniform(LightningShader.Uniforms.TEXTURE_0, 0);

            vao.bind();

            if (Globals.draw_particles) {
                for (com.oddlabs.tt.render.particle.Lightning emitter : activeLightnings) {
                    renderInternal(context, render_queues, emitter);
                }
            }
        } finally {
            activeLightnings.clear();
            vao.unbind();
        }
    }

    private void render2DParticle(com.oddlabs.tt.render.particle.@NonNull StretchParticle particle, float r, float g,
            float b,
            float a) {
        float src_x = particle.getSrcX();
        float src_y = particle.getSrcY();
        float src_z = particle.getSrcZ();
        float dst_x = particle.getDstX();
        float dst_y = particle.getDstY();
        float dst_z = particle.getDstZ();

        float sw = particle.getSrcWidth();
        float dw = particle.getDstWidth();

        // Quad 1 (World X-axis expansion)
        putVertex(dst_x - dw, dst_y, dst_z, 0f, 0f, r, g, b, a);
        putVertex(dst_x + dw, dst_y, dst_z, 1f, 0f, r, g, b, a);
        putVertex(src_x + sw, src_y, src_z, 1f, 1f, r, g, b, a);
        putVertex(src_x - sw, src_y, src_z, 0f, 1f, r, g, b, a);

        // Quad 2 (World Y-axis expansion)
        putVertex(dst_x, dst_y - dw, dst_z, 0f, 0f, r, g, b, a);
        putVertex(dst_x, dst_y + dw, dst_z, 1f, 0f, r, g, b, a);
        putVertex(src_x, src_y + sw, src_z, 1f, 1f, r, g, b, a);
        putVertex(src_x, src_y - sw, src_z, 0f, 1f, r, g, b, a);
    }

    private void putVertex(float x, float y, float z, float u, float v, float r, float g, float b, float a) {
        particle_buffer.put(x).put(y).put(z).put(u).put(v).put(r).put(g).put(b).put(a);
    }

    private void renderInternal(@NonNull RenderContext context, @NonNull RenderQueues render_queues,
            com.oddlabs.tt.render.particle.@NonNull Lightning lightning) {
        context.setTexture(0, render_queues.getTexture(VisualRegistry.getInstance().getLightningTexture()));

        particle_buffer.clear();
        var particles = lightning.getParticles();
        int particleCount = 0;

        float r = lightning.getColor().r();
        float g = lightning.getColor().g();
        float b = lightning.getColor().b();
        float a = lightning.getColor().a();

        for (com.oddlabs.tt.render.particle.StretchParticle particle : particles) {
            if (particleCount >= MAX_PARTICLES) {
                flush(particleCount);
                particleCount = 0;
                particle_buffer.clear();
            }
            render2DParticle(particle, r, g, b, a);
            particleCount++;
        }
        flush(particleCount);
    }

    private void flush(int count) {
        if (count == 0) return;
        particle_buffer.flip();
        if (vbo_offset + count > MAX_PARTICLES) {
            particle_vbo.orphan();
            vbo_offset = 0;
        }
        particle_vbo.putSubData(vbo_offset * VERTICES_PER_PARTICLE * FLOATS_PER_VERTEX, particle_buffer);
        GL11.glDrawElements(GL11.GL_TRIANGLES, count * INDICES_PER_PARTICLE, GL11.GL_UNSIGNED_SHORT, (long) vbo_offset
                * INDICES_PER_PARTICLE * Short.BYTES);
        vbo_offset += count;
    }

    public void debugRender(@NonNull Collection<com.oddlabs.tt.render.particle.@NonNull Lightning> emitter_queue) {
    }

    @Override
    public void close() {
        vao.close();
        particle_vbo.close();
        particle_ibo.close();
        shader.close();
    }
}
