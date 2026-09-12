package com.oddlabs.tt.client.render;

import com.oddlabs.tt.engine.render.DebugFlags;
import com.oddlabs.tt.engine.render.MatrixStack;
import com.oddlabs.tt.engine.render.Sprite;
import com.oddlabs.tt.engine.render.SpriteList;
import com.oddlabs.tt.engine.render.shader.PlacingShader;
import com.oddlabs.tt.engine.render.state.BlendMode;
import com.oddlabs.tt.engine.render.state.CullMode;
import com.oddlabs.tt.engine.render.state.DepthMode;
import com.oddlabs.tt.engine.render.state.RenderContext;
import com.oddlabs.tt.engine.vbo.VertexArray;
import com.oddlabs.util.Color;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Renders the translucent ghost building preview during placement.
 */
public final class PlacingRenderer implements AutoCloseable {
    private final PlacingShader shader = new PlacingShader();
    private final Map<SpriteList, VertexArray> vaos = new WeakHashMap<>();

    public void renderGhost(RenderContext context, Sprite sprite, SpriteList spriteList,
            Color.Linear color, Color.Linear teamColor, MatrixStack modelViewStack) {

        VertexArray vao = vaos.computeIfAbsent(spriteList, list -> {
            VertexArray newVao = new VertexArray();
            newVao.bind();

            list.getIndices().bind();

            GL20.glEnableVertexAttribArray(shader.locTexCoord);
            GL20.glEnableVertexAttribArray(shader.locPosition);
            GL20.glEnableVertexAttribArray(shader.locNormal);
            newVao.unbind();
            return newVao;
        });

        try (var _ = shader.use()) {
            shader.setUniform(shader.locDesaturate, 0.3f);

            // Setup uniform state
            context.setTexture(0, sprite.textures[0][Sprite.TEXTURE_NORMAL]);
            shader.setUniform(shader.locTexture0, 0);

            boolean useLighting = DebugFlags.draw_light && sprite.lighted;
            shader.setUniform(shader.locEnableLighting, useLighting);
            shader.setUniform(shader.locReplaceMode, !useLighting && !sprite.modulate_color);

            if (sprite.modulate_color) {
                shader.setUniform(shader.locModulateColor, true);
                shader.setUniform(shader.locEnableTeamColor, false);
                shader.setUniform(shader.locAlphaTestValue, 0.0f);
            } else {
                shader.setUniform(shader.locModulateColor, false);
                shader.setUniform(shader.locAlphaTestValue, 0.3f);
                if (sprite.hasTeamDecal()) {
                    shader.setUniform(shader.locEnableTeamColor, true);
                    context.setTexture(1, sprite.textures[0][Sprite.TEXTURE_TEAM]);
                    shader.setUniform(shader.locTexture1, 1);
                } else {
                    shader.setUniform(shader.locEnableTeamColor, false);
                }
            }

            if (sprite.hasBumpMap(0)) {
                shader.setUniform(shader.locEnableNormalMap, true);
                context.setTexture(2, sprite.textures[0][Sprite.TEXTURE_BUMP]);
                shader.setUniform(shader.locNormalMap, 2);
            } else {
                shader.setUniform(shader.locEnableNormalMap, false);
            }

            shader.setUniform(shader.locModulateColor, true);
            shader.setUniform(shader.locAlphaTestValue, 0.5f);
            shader.setUniform(shader.locColor, color);
            shader.setUniform(shader.locDecalColor, teamColor);
            shader.setUniform(shader.locModelViewMatrix, modelViewStack.current());

            try (var _ = context.withCullMode(CullMode.BACK)) {
                // Pass 1: Depth Prime (Write Depth, No Color)
                try (var _ = context.withDepthMode(DepthMode.READ_WRITE); var _ = context.withColorMask(false, false,
                        false, false); var _ = context.withBlendMode(BlendMode.NONE)) {
                    drawSprite(sprite, spriteList, vao);
                }

                // Pass 2: Color Render (No Depth Write, Equal Depth)
                try (var _ = context.withDepthMode(DepthMode.READ_ONLY); var _ = context.withColorMask(true, true, true,
                        true); var _ = context.withBlendMode(BlendMode.ALPHA)) {
                    drawSprite(sprite, spriteList, vao);
                }
            } finally {
                shader.setUniform(shader.locDesaturate, 0.0f);
                shader.setUniform(shader.locModulateColor, false);
                shader.setUniform(shader.locAlphaTestValue, 0.3f);
            }
        }
    }

    private void drawSprite(Sprite sprite, SpriteList spriteList, VertexArray vao) {
        vao.bind();
        try {
            spriteList.getTexcoords().vertexAttribPointer(shader.locTexCoord, 2, 0,
                    (long) sprite.texcoords_offset * Float.BYTES);
            spriteList.getPositions().vertexAttribPointer(shader.locPosition, 3, 0,
                    (long) sprite.vertices_offset * Float.BYTES);
            spriteList.getNormals().vertexAttribPointer(shader.locNormal, 3, 0,
                    (long) sprite.normals_offset * Float.BYTES);

            spriteList.getIndices().drawElements(GL11.GL_TRIANGLES, sprite.getTriangleCount() * 3,
                    sprite.indices_offset);
        } finally {
            vao.unbind();
        }
    }

    @Override
    public void close() {
        shader.close();
        for (VertexArray vao : vaos.values()) {
            vao.close();
        }
        vaos.clear();
    }
}
