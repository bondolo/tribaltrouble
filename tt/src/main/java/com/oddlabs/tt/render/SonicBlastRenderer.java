package com.oddlabs.tt.render;

import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.particle.SonicBlastEffect;
import com.oddlabs.tt.procedural.GeneratorNoise;
import com.oddlabs.tt.render.shader.SonicBlastShader;
import com.oddlabs.tt.render.shader.VertexLayout;
import com.oddlabs.tt.render.state.BlendMode;
import com.oddlabs.tt.render.state.CullMode;
import com.oddlabs.tt.render.state.DepthMode;
import com.oddlabs.tt.render.state.RenderContext;
import com.oddlabs.tt.vbo.FloatVBO;
import com.oddlabs.tt.vbo.VertexArray;
import com.oddlabs.util.Color;
import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Queue;

/**
 * Specialized renderer for the Sonic Blast expanding ring effect.
 */
public final class SonicBlastRenderer implements AutoCloseable {
    private static final Color.Linear BLAST_COLOR = new Color.Linear(0.7f, 0.85f, 1.0f, 1.0f);
    private static final VertexLayout<SonicBlastShader.Attribute> LAYOUT = new VertexLayout<>(
            SonicBlastShader.Attribute.POSITION,
            SonicBlastShader.Attribute.TEX_COORD
    );
    private final @NonNull SonicBlastShader shader = new SonicBlastShader();
    private final @NonNull FloatVBO vbo;
    private final VertexArray vao = new VertexArray();
    private final @NonNull Texture[] noiseTextures;

    public SonicBlastRenderer() {
        // Create a simple quad centered at 0,0 on XY plane, scaled to 1x1
        try (var stack = MemoryStack.stackPush()) {
            FloatBuffer buffer = stack.mallocFloat(4 * 5); // 4 verts * (3 pos + 2 uv)
            float s = 0.5f;
            // Pos (x,y,z), UV (u,v)
            buffer.put(-s).put(-s).put(0).put(0).put(0);
            buffer.put(s).put(-s).put(0).put(1).put(0);
            buffer.put(-s).put(s).put(0).put(0).put(1);
            buffer.put(s).put(s).put(0).put(1).put(1);
            buffer.flip();

            vbo = new FloatVBO(GL15.GL_STATIC_DRAW, buffer);
        }

        // Utilize procedural generator for ring noise
        this.noiseTextures = new GeneratorNoise(64, 42).generate();

        vao.bind();
        vbo.bind();
        LAYOUT.bind(shader);
        vao.unbind();
    }

    private final @NonNull Deque<SonicBlastEffect> activeEffects = new ArrayDeque<>();

    public void prepare(@NonNull Queue<@NonNull SonicBlastEffect> queue) {
        activeEffects.clear();
        activeEffects.addAll(queue);
        queue.clear();
    }

    public void render(@NonNull RenderContext context, @NonNull RenderQueues render_queues, @NonNull CameraState state,
            @NonNull MatrixStack modelViewStack, @NonNull MatrixStack projectionStack) {
        if (activeEffects.isEmpty()) return;

        try (var _ = shader.use(); var _ = context.withBlendMode(BlendMode.ADDITIVE); var _ = context.withDepthMode(
                DepthMode.NONE); var _ = context.withCullMode(CullMode.NONE)) {

            shader.setUniformColor3(SonicBlastShader.Uniforms.COLOR, BLAST_COLOR);

            // Bind generated noise texture for ring turbulence
            context.setTexture(0, noiseTextures[0].getHandle());
            shader.setUniform(SonicBlastShader.Uniforms.TEXTURE_0, 0);

            vao.bind();

            for (SonicBlastEffect effect : activeEffects) {
                if (effect.isDead()) continue;

                modelViewStack.push();

                float x = effect.getPositionX();
                float y = effect.getPositionY();
                float z = effect.getPositionZ();
                // Visual radius is 20% larger than damage radius ("felt but no damage")
                float visualRadius = effect.getMaxRadius() * 1.2f;
                float r = visualRadius * 2.0f; // Quad size (diameter)

                // Position and scale the quad to be parallel to the ground
                modelViewStack.translate(x, y, z);
                modelViewStack.scale(r, r, 1.0f);

                shader.setUniform(SonicBlastShader.Uniforms.MODEL_VIEW_MATRIX, modelViewStack.current());
                shader.setUniform(SonicBlastShader.Uniforms.TIME, effect.getTime());
                shader.setUniform(SonicBlastShader.Uniforms.MAX_RADIUS, visualRadius);
                shader.setUniform(SonicBlastShader.Uniforms.EXPANSION_SPEED, visualRadius / Math.max(effect
                        .getDuration(), 0.001f));

                GL11.glDrawArrays(GL11.GL_TRIANGLE_STRIP, 0, 4);

                modelViewStack.pop();
            }

            vao.unbind();
            context.setActiveTexture(0);
        } finally {
            activeEffects.clear();
        }
    }

    @Override
    public void close() {
        shader.close();
        vbo.close();
        vao.close();
        for (Texture t : noiseTextures) {
            t.close();
        }
    }
}
