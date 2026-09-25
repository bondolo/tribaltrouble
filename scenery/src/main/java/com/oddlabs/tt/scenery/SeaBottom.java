package com.oddlabs.tt.scenery;

import com.oddlabs.tt.engine.render.CameraState;
import com.oddlabs.tt.engine.render.DebugFlags;
import com.oddlabs.tt.engine.render.MatrixStack;
import com.oddlabs.tt.engine.render.SceneRenderer;
import com.oddlabs.tt.engine.render.Texture;
import com.oddlabs.tt.engine.render.state.BlendMode;
import com.oddlabs.tt.engine.render.state.CullMode;
import com.oddlabs.tt.engine.render.state.DepthMode;
import com.oddlabs.tt.engine.render.state.RenderContext;
import com.oddlabs.tt.engine.vbo.ShortVBO;
import com.oddlabs.tt.engine.vbo.VBO;
import com.oddlabs.tt.engine.vbo.VertexArray;
import com.oddlabs.tt.procedural.landscape.LandscapeConfig;
import com.oddlabs.tt.simulation.model.Terrain;
import com.oddlabs.util.Color;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import java.util.Map;

/**
 * Renders the ocean floor scenery surrounding the island using concentric rings.
 */
public final class SeaBottom implements SceneRenderer, AutoCloseable {

    public static final Map<Terrain, Color.Linear> SEA_BOTTOM_COLOR = Map.of(
            Terrain.NATIVE, new Color.Standard(0xFF_73_40_99).linear(),
            Terrain.VIKING, Color.Linear.BLACK
    );

    private final SeaBottomShader seaBottomShader = new SeaBottomShader();
    private final VertexArray seaBottomVAO;
    private final ConcentricRingMesh ringMesh;
    private final Texture detail;
    private final Texture detailNormal;
    private final Color.Linear seaBottomColor;

    public SeaBottom(Terrain terrain, Texture detail,
            Texture detailNormal, ConcentricRingMesh ringMesh) {
        this.detail = detail;
        this.detailNormal = detailNormal;
        this.seaBottomColor = SEA_BOTTOM_COLOR.get(terrain);
        this.ringMesh = ringMesh;

        this.seaBottomVAO = new VertexArray();
        seaBottomVAO.bind();
        ringMesh.bottomVertices().bind();
        GL20.glEnableVertexAttribArray(0); // Position
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 0, 0);
        seaBottomVAO.unbind();
    }

    @Override
    public void render(RenderContext context, CameraState state,
            MatrixStack modelView, MatrixStack projection) {
        try (var _ = seaBottomShader.use(); var _ = context.withBlendMode(BlendMode.NONE); var _ = context
                .withDepthMode(DepthMode.READ_WRITE); var _ = context.withCullMode(CullMode.BACK)) {

            seaBottomShader.setUniform(seaBottomShader.locModelViewMatrix, modelView.current());
            seaBottomShader.setUniform(seaBottomShader.locBaseColor, seaBottomColor);

            if (DebugFlags.draw_detail) {
                context.setTexture(1, detail);
                seaBottomShader.setUniform(seaBottomShader.locTexture1, 1);
                context.setTexture(2, detailNormal);
                seaBottomShader.setUniform(seaBottomShader.locTextureNormal, 2);
                seaBottomShader.setUniform(seaBottomShader.locDetailScale,
                        LandscapeConfig.LANDSCAPE_DETAIL_REPEAT_RATE);
            } else {
                seaBottomShader.setUniform(seaBottomShader.locDetailScale, 0f);
            }

            seaBottomVAO.bind();
            ShortVBO indices = ringMesh.indices();
            indices.drawElements(GL11.GL_TRIANGLES, indices.capacity(), 0);
            seaBottomVAO.unbind();

            context.setActiveTexture(0);
        } finally {
            VBO.releaseIndexVBO();
        }
    }

    @Override
    public void close() {
        seaBottomShader.close();
        seaBottomVAO.close();
    }
}
