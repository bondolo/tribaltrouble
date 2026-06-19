package com.oddlabs.tt.render;

import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.landscape.HeightMap;
import com.oddlabs.tt.render.procedural.GeneratorNoise;
import com.oddlabs.tt.render.shader.SonicBlastShader;
import com.oddlabs.tt.render.shader.VertexLayout;
import com.oddlabs.tt.render.state.BlendMode;
import com.oddlabs.tt.render.state.CullMode;
import com.oddlabs.tt.render.state.DepthMode;
import com.oddlabs.tt.render.state.RenderContext;
import com.oddlabs.tt.render.vbo.FloatVBO;
import com.oddlabs.tt.render.vbo.VertexArray;
import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Specialized renderer for expanding-ring shockwave effects.
 */
public final class SonicBlastRenderer implements AutoCloseable {
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

    private final @NonNull List<@NonNull ClientSonicBlast> activeEffects = new ArrayList<>();

    public void prepare(@NonNull Collection<@NonNull ClientSonicBlast> blasts) {
        activeEffects.clear();
        activeEffects.addAll(blasts);
    }

    public void render(@NonNull RenderContext context, @NonNull RenderQueues render_queues, @NonNull CameraState state,
            @NonNull MatrixStack modelViewStack, @NonNull MatrixStack projectionStack, @NonNull HeightMap heightMap) {
        if (activeEffects.isEmpty()) return;

        try (var _ = shader.use(); var _ = context.withBlendMode(BlendMode.ADDITIVE); var _ = context.withDepthMode(
                DepthMode.READ_ONLY); var _ = context.withCullMode(CullMode.NONE)) {

            // Bind generated noise texture for ring turbulence
            context.setTexture(0, noiseTextures[0].getHandle());
            shader.setUniform(SonicBlastShader.Uniforms.TEXTURE_0, 0);

            vao.bind();

            for (ClientSonicBlast effect : activeEffects) {
                modelViewStack.push();

                float x = effect.getX();
                float y = effect.getY();
                float z = effect.getZ();
                // Visual radius is 20% larger than damage radius ("felt but no damage")
                float visualRadius = effect.getMaxRadius() * 1.2f;
                float r = visualRadius * 2.0f; // Quad size (diameter)

                float h_l = heightMap.getNearestHeight(x - 0.5f, y);
                float h_r = heightMap.getNearestHeight(x + 0.5f, y);
                float h_d = heightMap.getNearestHeight(x, y - 0.5f);
                float h_u = heightMap.getNearestHeight(x, y + 0.5f);

                float dh_dx = h_r - h_l;
                float dh_dy = h_u - h_d;
                float angleX = (float) Math.atan2(dh_dx, 1.0f);
                float angleY = (float) Math.atan2(dh_dy, 1.0f);

                // Position, align to terrain slope, and scale the quad
                modelViewStack.translate(x, y, z);
                modelViewStack.current().rotateY(-angleX);
                modelViewStack.current().rotateX(angleY);
                modelViewStack.scale(r, r, 1.0f);

                shader.setUniformColor3(SonicBlastShader.Uniforms.COLOR, effect.getColor());
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
