package com.oddlabs.tt.landscape;

import com.oddlabs.tt.render.FBO;
import com.oddlabs.tt.render.Texture;
import com.oddlabs.tt.render.shader.ShaderProgram;
import com.oddlabs.tt.resource.BlendInfo;
import com.oddlabs.tt.resource.BlendLighting;
import com.oddlabs.tt.resource.StructureBlend;
import com.oddlabs.tt.resource.WorldInfo;
import com.oddlabs.tt.vbo.QuadVBO;
import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;

import static com.oddlabs.tt.util.GLUtils.checkGLError;

public final class LandscapeBaker {

    private static final String VERTEX_SHADER = """
            #version 410 core
            layout(location = 0) in vec2 in_Position;
            layout(location = 1) in vec2 in_TexCoord;
            out vec2 v_texCoord;
            void main() {
                gl_Position = vec4(in_Position, 0.0, 1.0);
                v_texCoord = in_TexCoord;
            }
            """;

    private static final String FRAGMENT_SHADER = """
            #version 410 core
            uniform sampler2D u_BaseDiffuse;
            uniform sampler2D u_LayerDiffuse;
            uniform sampler2D u_BaseNormal;
            uniform sampler2D u_LayerNormal;
            uniform sampler2D u_AlphaMap;
            uniform int u_Mode; // 0 = Blend, 1 = Light
            uniform float u_TextureScale;
            uniform vec3 u_Color;
            
            in vec2 v_texCoord;
            
            layout(location = 0) out vec4 out_Diffuse;
            layout(location = 1) out vec4 out_Normal;
            
            void main() {
                // Fetch base values
                vec4 baseDiff = texture(u_BaseDiffuse, v_texCoord);
                vec4 baseNorm = texture(u_BaseNormal, v_texCoord);
                float alpha = texture(u_AlphaMap, v_texCoord).r;
            
                if (u_Mode == 0) { // Structure Blend
                    // Fetch source textures
                    vec4 layerDiff = texture(u_LayerDiffuse, v_texCoord * u_TextureScale);
                    vec4 layerNorm = texture(u_LayerNormal, v_texCoord * u_TextureScale);
            
                    // IMPORTANT: To match legacy visual look, we must blend in sRGB space.
                    // Hardware fetch already gave us linear data, so we convert back to sRGB for the mix.
                    vec3 srgbBase = pow(baseDiff.rgb, vec3(1.0 / 2.2));
                    vec3 srgbLayer = pow(layerDiff.rgb, vec3(1.0 / 2.2));
                    vec3 srgbMixed = mix(srgbBase, srgbLayer, alpha);
            
                    // Convert back to linear for the HDR output
                    out_Diffuse = vec4(pow(srgbMixed, vec3(2.2)), mix(baseDiff.a, layerDiff.a, alpha));
                    out_Normal = mix(baseNorm, layerNorm, alpha);
                } else { // Lighting Blend
                    out_Diffuse = baseDiff + vec4(u_Color * alpha, 0.0);
                    out_Normal = baseNorm;
                }
            }
            """;

    private static class BlendShader extends ShaderProgram {
         BlendShader() {
            super(VERTEX_SHADER, FRAGMENT_SHADER);
            // Layouts are defined in shader, no need for explicit bindFragDataLocation
            link();
        }
    }

    private final int colormapSize;
    private final float textureScale;

    public LandscapeBaker(int colormapSize, float textureScale) {
        this.colormapSize = colormapSize;
        this.textureScale = textureScale;
    }

    public WorldInfo.@NonNull Maps bake(@NonNull BlendInfo @NonNull [] blendInfos) {
        checkGLError("Before bake");
        Texture[] diffuse = new Texture[2];
        Texture[] normal = new Texture[2];

        for (int i = 0; i < 2; i++) {
            diffuse[i] = new Texture(colormapSize, colormapSize, GL11.GL_RGBA8, GL11.GL_LINEAR, GL11.GL_LINEAR, GL11.GL_REPEAT);
            checkGLError("After diffuse texture " + i);
            normal[i] = new Texture(colormapSize, colormapSize, GL11.GL_RGBA8, GL11.GL_LINEAR, GL11.GL_LINEAR, GL11.GL_REPEAT);
            checkGLError("After normal texture " + i);
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            // Save current state
            IntBuffer viewport = stack.mallocInt(4);
            GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
            int savedFBO = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
            int savedDrawBuffer = GL11.glGetInteger(GL30.GL_DRAW_BUFFER0);

            try (FBO fbo = new FBO(colormapSize, colormapSize);
                 BlendShader shader = new BlendShader();
                 QuadVBO quad = new QuadVBO()) {

                checkGLError("After resource creation");

                int current = 0;

                try (var _ = shader.use()) {
                    checkGLError("After shader use");
                    shader.setUniform("u_BaseDiffuse", 0);
                    shader.setUniform("u_LayerDiffuse", 1);
                    shader.setUniform("u_BaseNormal", 2);
                    shader.setUniform("u_LayerNormal", 3);
                    shader.setUniform("u_AlphaMap", 4);
                    shader.setUniform("u_TextureScale", textureScale);

                    IntBuffer drawBuffers = stack.mallocInt(2);
                    drawBuffers.put(GL30.GL_COLOR_ATTACHMENT0).put(GL30.GL_COLOR_ATTACHMENT1).flip();

                    boolean needsClear = true;
                    for (BlendInfo info : blendInfos) {
                        int src = current;
                        int dst = 1 - current;

                        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo.getHandle());
                        GL11.glViewport(0, 0, colormapSize, colormapSize);
                        fbo.attachTexture(GL30.GL_COLOR_ATTACHMENT0, diffuse[dst]);
                        fbo.attachTexture(GL30.GL_COLOR_ATTACHMENT1, normal[dst]);
                        GL30.glDrawBuffers(drawBuffers);
                        fbo.checkStatus();

                        if (needsClear) {
                            // First layer initialization
                            GL11.glClearColor(0, 0, 0, 0);
                            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
                            needsClear = false;
                        }

                        GL13.glActiveTexture(GL13.GL_TEXTURE0);
                        GL11.glBindTexture(GL11.GL_TEXTURE_2D, diffuse[src].getHandle());
                        GL13.glActiveTexture(GL13.GL_TEXTURE2);
                        GL11.glBindTexture(GL11.GL_TEXTURE_2D, normal[src].getHandle());
                        GL13.glActiveTexture(GL13.GL_TEXTURE4);
                        GL11.glBindTexture(GL11.GL_TEXTURE_2D, info.getAlphaMap().getHandle());

                        if (info instanceof StructureBlend sb) {
                            shader.setUniform("u_Mode", 0);
                            GL13.glActiveTexture(GL13.GL_TEXTURE1);
                            GL11.glBindTexture(GL11.GL_TEXTURE_2D, sb.getStructureMap().getHandle());
                            GL13.glActiveTexture(GL13.GL_TEXTURE3);
                            GL11.glBindTexture(GL11.GL_TEXTURE_2D, sb.getNormalMap().getHandle());
                        } else if (info instanceof BlendLighting bl) {
                            shader.setUniform("u_Mode", 1);
                            shader.setUniformColor3("u_Color", bl.getColor());
                        }

                        quad.render();
                        current = dst; // Flip
                    }
                }

                GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);

                GL11.glBindTexture(GL11.GL_TEXTURE_2D, diffuse[current].getHandle());
                GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR);

                GL11.glBindTexture(GL11.GL_TEXTURE_2D, normal[current].getHandle());
                GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR);

                // Detach textures from FBO before returning them
                // This prevents GL_INVALID_OPERATION when they are later bound as source textures (feedback loop).
                fbo.detachAll();

                // Delete the unused pair
                diffuse[1 - current].close();
                normal[1 - current].close();

                return new WorldInfo.Maps(diffuse[current], normal[current]);
            } finally {
                // Restore state
                GL11.glViewport(viewport.get(0), viewport.get(1), viewport.get(2), viewport.get(3));
                GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, savedFBO);
                GL11.glDrawBuffer(savedDrawBuffer);
            }
        }
    }
}
