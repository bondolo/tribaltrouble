package com.oddlabs.tt.engine.render;


import com.oddlabs.tt.engine.image.GLImage;
import com.oddlabs.tt.engine.image.GLIntImage;
import com.oddlabs.tt.engine.render.shader.InstancedSpriteShader;
import com.oddlabs.tt.engine.render.state.BlendMode;
import com.oddlabs.tt.engine.render.state.CullMode;
import com.oddlabs.tt.engine.render.state.DepthMode;
import com.oddlabs.tt.engine.render.state.RenderContext;
import com.oddlabs.tt.engine.vbo.ByteVBO;
import com.oddlabs.tt.engine.vbo.FloatVBO;
import com.oddlabs.tt.engine.vbo.ShortVBO;
import com.oddlabs.tt.engine.vbo.VertexArray;
import com.oddlabs.util.Color;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL33;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Specialized renderer that handles high-performance 3D sprite rendering using hardware instancing and skeletal
 * skinning.
 * Batches sprites by texture and render state to minimize draw calls and state changes.
 */
public final class InstancedSpriteRenderer implements AutoCloseable {

    private final InstancedSpriteShader shader = new InstancedSpriteShader();
    private final Map<BatchKey, RenderBatch> batches = new HashMap<>();
    private final Texture whiteTexture;

    private FloatVBO boneMatrixVBO;
    private int boneMatrixTboHandle;
    private FloatBuffer boneMatrixBuffer;
    private int boneMatrixTexels = 0;
    private final Matrix4f[] scratchBones = new Matrix4f[48];
    private final Map<BoneKey, Integer> boneOffsetCache = new HashMap<>();

    private record BoneKey(SpriteList spriteList, int animation, float animTicks) {
    }

    public InstancedSpriteRenderer() {
        GLImage whiteImage = new GLIntImage(1, 1, GL11.GL_RGBA);
        whiteImage.putPixel(0, 0, Color.WHITE_INT);
        whiteTexture = new Texture(new GLImage[]{whiteImage}, GL11.GL_RGBA8, GL11.GL_NEAREST, GL11.GL_NEAREST,
                GL12.GL_CLAMP_TO_EDGE, GL12.GL_CLAMP_TO_EDGE);

        int initialFloats = 65536;
        boneMatrixBuffer = BufferUtils.createFloatBuffer(initialFloats);
        boneMatrixVBO = new FloatVBO(GL15.GL_STREAM_DRAW, initialFloats);
        boneMatrixTboHandle = GL11.glGenTextures();
        GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, boneMatrixTboHandle);
        GL31.glTexBuffer(GL31.GL_TEXTURE_BUFFER, GL30.GL_RGBA32F, boneMatrixVBO.getHandle());
        GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, 0);

        for (int i = 0; i < scratchBones.length; i++) {
            scratchBones[i] = new Matrix4f();
        }
    }

    Texture getWhiteTexture() {
        return whiteTexture;
    }

    private void ensureBoneCapacity(int requiredFloats) {
        if (requiredFloats > boneMatrixBuffer.capacity()) {
            int newCapacity = Math.max(boneMatrixBuffer.capacity() * 2, requiredFloats);
            FloatBuffer newBuffer = BufferUtils.createFloatBuffer(newCapacity);
            boneMatrixBuffer.position(0);
            boneMatrixBuffer.limit(boneMatrixTexels * 4);
            newBuffer.put(boneMatrixBuffer);
            newBuffer.clear();
            boneMatrixBuffer = newBuffer;

            boneMatrixVBO.close();
            boneMatrixVBO = new FloatVBO(GL15.GL_STREAM_DRAW, newCapacity);
        }
    }

    public void add(SpriteList spriteList, int spriteIndex, int animation, float animTicks,
            Texture texture, @Nullable Texture teamTexture, @Nullable Texture bumpTexture,
            boolean respond, boolean blend, boolean depthWrite, boolean depthTest, Matrix4fc modelMatrix,
            Color color, Color decalColor) {
        int boneBaseOffset = -1;
        if (spriteList.isSkeletal()) {
            int boneCount = spriteList.getBoneCount();
            if (boneCount > 0) {
                BoneKey key = new BoneKey(spriteList, animation, animTicks);
                Integer cached = boneOffsetCache.get(key);
                if (cached != null) {
                    boneBaseOffset = cached;
                } else {
                    spriteList.evaluateSkeleton(animation, animTicks, scratchBones);
                    boneBaseOffset = boneMatrixTexels;
                    int neededFloats = boneCount * 16;
                    ensureBoneCapacity(boneMatrixTexels * 4 + neededFloats);
                    for (int b = 0; b < boneCount; b++) {
                        scratchBones[b].get(boneMatrixTexels * 4 + b * 16, boneMatrixBuffer);
                    }
                    boneMatrixTexels += boneCount * 4;
                    boneOffsetCache.put(key, boneBaseOffset);
                }
            }
        }

        BatchKey key = new BatchKey(spriteList, texture, teamTexture, bumpTexture, respond, blend,
                depthWrite, depthTest);
        RenderBatch batch = batches.computeIfAbsent(key, RenderBatch::new);
        batch.addInstance(spriteIndex, boneBaseOffset, modelMatrix, color, decalColor);
    }

    public void renderAll(RenderContext context, CameraState cameraState,
            MatrixStack projectionStack) {
        if (batches.isEmpty()) return;

        if (boneMatrixTexels > 0) {
            boneMatrixVBO.bind();
            boneMatrixBuffer.limit(boneMatrixTexels * 4).position(0);
            GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, boneMatrixBuffer);
        }

        try (var _ = shader.use()) {
            shader.setUniform(shader.locBoneMatrixBuffer, 5);
            context.setTexture(5, boneMatrixTboHandle, GL31.GL_TEXTURE_BUFFER);
            GL31.glTexBuffer(GL31.GL_TEXTURE_BUFFER, GL30.GL_RGBA32F, boneMatrixVBO.getHandle());

            List<RenderBatch> sortedBatches = new ArrayList<>(batches.values());
            sortedBatches.sort(RenderBatch.COMPARATOR);

            for (RenderBatch batch : sortedBatches) {
                batch.render(context, shader, whiteTexture);
            }
        } finally {
            // Restore default state to prevent leakage to other renderers (Sky, Landscape, etc.)
            context.setTexture(5, 0, GL31.GL_TEXTURE_BUFFER);
            context.bindVertexArray(0);
            context.setDepthMode(DepthMode.READ_WRITE);
            context.setBlendMode(BlendMode.NONE);
            context.setCullMode(CullMode.BACK);
            context.setSampleAlphaToCoverage(false);
            context.setDrawBuffers(true);
            context.setColorMask(true, true, true, true);
            clear();
        }
    }

    public void clear() {
        for (RenderBatch batch : batches.values()) {
            batch.clear();
        }
        boneMatrixTexels = 0;
        boneMatrixBuffer.clear();
        boneOffsetCache.clear();
    }

    @Override
    public void close() {
        for (RenderBatch batch : batches.values()) {
            batch.close();
        }
        batches.clear();
        shader.close();
        whiteTexture.close();
        boneMatrixVBO.close();
        GL11.glDeleteTextures(boneMatrixTboHandle);
    }

    private record BatchKey(SpriteList spriteList, Texture texture,
                            @Nullable Texture teamTexture, @Nullable Texture bumpTexture, boolean respond,
                            boolean blend, boolean depthWrite, boolean depthTest) {
    }

    private static class RenderBatch implements AutoCloseable {
        private final BatchKey key;
        private final Map<Integer, InstanceGroup> groups = new HashMap<>();

        // mat4 (16) + color (4) + decalColor (4) + boneBaseOffset (1)
        private static final int FLOATS_PER_INSTANCE = 16 + 4 + 4 + 1;

        private static class InstanceGroup implements AutoCloseable {
            private final int spriteIndex;
            private final SpriteList spriteList;
            private FloatVBO vbo;
            private final VertexArray vao;
            private FloatBuffer buffer;
            private int count = 0;
            private int capacity = 32;

            InstanceGroup(int spriteIndex, BatchKey key, int floatsPerInstance) {
                this.spriteIndex = spriteIndex;
                this.spriteList = key.spriteList;
                this.buffer = BufferUtils.createFloatBuffer(capacity * floatsPerInstance);
                this.vbo = new FloatVBO(GL15.GL_STREAM_DRAW, capacity * floatsPerInstance);

                this.vao = new VertexArray();
                vao.bind();

                ShortVBO ibo = spriteList.getIndices();
                FloatVBO positionsVBO = spriteList.getPositions();
                FloatVBO normalsVBO = spriteList.getNormals();
                FloatVBO texCoordVBO = spriteList.getTexcoords();

                ibo.bind();

                // Position (Location 0)
                positionsVBO.bind();
                GL20.glEnableVertexAttribArray(0);
                Sprite sprite = spriteList.getSprite(spriteIndex);
                GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 0, (long) sprite.vertices_offset * Float.BYTES);

                // Normal (Location 1)
                normalsVBO.bind();
                GL20.glEnableVertexAttribArray(1);
                GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, 0, (long) sprite.normals_offset * Float.BYTES);

                // TexCoord (Location 2)
                texCoordVBO.bind();
                GL20.glEnableVertexAttribArray(2);
                GL20.glVertexAttribPointer(2, 2, GL11.GL_FLOAT, false, 0, (long) sprite.texcoords_offset * Float.BYTES);

                // If skeletal: Bone Indices (Location 3) and Bone Weights (Location 11)
                if (spriteList.isSkeletal()) {
                    ByteVBO boneIndicesVBO = spriteList.getBoneIndices();
                    FloatVBO boneWeightsVBO = spriteList.getBoneWeights();

                    if (boneIndicesVBO != null) {
                        boneIndicesVBO.bind();
                        GL20.glEnableVertexAttribArray(3);
                        GL30.glVertexAttribIPointer(3, 4, GL11.GL_UNSIGNED_BYTE, 0, (long) sprite.bone_indices_offset);
                    }
                    if (boneWeightsVBO != null) {
                        boneWeightsVBO.bind();
                        GL20.glEnableVertexAttribArray(11);
                        GL20.glVertexAttribPointer(11, 4, GL11.GL_FLOAT, false, 0,
                                (long) sprite.bone_weights_offset * Float.BYTES);
                    }
                }

                setupInstanceAttributes();

                vao.unbind();
            }

            private void setupInstanceAttributes() {
                vbo.bind();
                int instanceStride = FLOATS_PER_INSTANCE * Float.BYTES;

                // Model Matrix (Locations 4-7)
                for (int i = 0; i < 4; i++) {
                    int loc = 4 + i;
                    GL20.glEnableVertexAttribArray(loc);
                    GL20.glVertexAttribPointer(loc, 4, GL11.GL_FLOAT, false, instanceStride, (long) i * 4
                            * Float.BYTES);
                    GL33.glVertexAttribDivisor(loc, 1);
                }

                // Color (Location 8)
                int colorLoc = 8;
                GL20.glEnableVertexAttribArray(colorLoc);
                GL20.glVertexAttribPointer(colorLoc, 4, GL11.GL_FLOAT, false, instanceStride, 16 * Float.BYTES);
                GL33.glVertexAttribDivisor(colorLoc, 1);

                // Decal Color (Location 9)
                int decalColorLoc = 9;
                GL20.glEnableVertexAttribArray(decalColorLoc);
                GL20.glVertexAttribPointer(decalColorLoc, 4, GL11.GL_FLOAT, false, instanceStride, 20 * Float.BYTES);
                GL33.glVertexAttribDivisor(decalColorLoc, 1);

                // Bone Base Offset (Location 10)
                int boneOffsetLoc = 10;
                GL20.glEnableVertexAttribArray(boneOffsetLoc);
                GL20.glVertexAttribPointer(boneOffsetLoc, 1, GL11.GL_FLOAT, false, instanceStride, 24 * Float.BYTES);
                GL33.glVertexAttribDivisor(boneOffsetLoc, 1);
            }

            void add(int boneBaseOffset, Matrix4fc modelMatrix, Color color, Color decalColor) {
                if (count >= capacity) {
                    int newCapacity = capacity * 2;
                    FloatBuffer newBuffer = BufferUtils.createFloatBuffer(newCapacity * FLOATS_PER_INSTANCE);
                    buffer.position(0);
                    buffer.limit(count * FLOATS_PER_INSTANCE);
                    newBuffer.put(buffer);
                    buffer = newBuffer;

                    vbo.close();
                    vbo = new FloatVBO(GL15.GL_STREAM_DRAW, newCapacity * FLOATS_PER_INSTANCE);
                    vbo.orphan();

                    vao.bind();
                    setupInstanceAttributes();
                    vao.unbind();

                    capacity = newCapacity;
                }

                int base = count * FLOATS_PER_INSTANCE;
                modelMatrix.get(base, buffer);
                color.get(base + 16, buffer);
                decalColor.get(base + 20, buffer);
                buffer.put(base + 24, (float) boneBaseOffset);

                count++;
            }

            void upload(RenderContext context) {
                vbo.bind();
                vbo.orphan();
                buffer.limit(count * FLOATS_PER_INSTANCE).position(0);
                GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, buffer);
            }

            void draw(RenderContext context) {
                vao.bind();
                Sprite sprite = spriteList.getSprite(spriteIndex);
                context.setCullMode(sprite.culled ? CullMode.BACK : CullMode.NONE);
                GL31.glDrawElementsInstanced(GL11.GL_TRIANGLES, sprite.getTriangleCount() * 3, GL11.GL_UNSIGNED_SHORT,
                        (long) sprite.indices_offset * Short.BYTES, count);
            }

            void clear() {
                count = 0;
                buffer.clear();
            }

            @Override
            public void close() {
                vao.close();
                vbo.close();
            }
        }

        private static final Comparator<RenderBatch> COMPARATOR = Comparator
                .comparing((RenderBatch b) -> b.key.blend)
                .thenComparingInt(b -> b.key.texture.getHandle())
                .thenComparingInt(b -> System.identityHashCode(b.key.spriteList))
                .thenComparingInt(b -> b.key.teamTexture != null ? b.key.teamTexture.getHandle() : 0)
                .thenComparingInt(b -> b.key.bumpTexture != null ? b.key.bumpTexture.getHandle() : 0);

        RenderBatch(BatchKey key) {
            this.key = key;
        }

        void addInstance(int spriteIndex, int boneBaseOffset, Matrix4fc modelMatrix, Color color,
                Color decalColor) {
            InstanceGroup group = groups.computeIfAbsent(spriteIndex, k -> new InstanceGroup(k, key,
                    FLOATS_PER_INSTANCE));
            group.add(boneBaseOffset, modelMatrix, color, decalColor);
        }

        void render(RenderContext context, InstancedSpriteShader shader, Texture whiteTexture) {
            boolean hasInstances = false;
            for (InstanceGroup group : groups.values()) {
                if (group.count > 0) {
                    hasInstances = true;
                    break;
                }
            }
            if (!hasInstances) return;

            InstanceGroup representativeGroup = null;
            for (InstanceGroup group : groups.values()) {
                if (group.count > 0) {
                    representativeGroup = group;
                    break;
                }
            }
            if (representativeGroup == null) return;

            SpriteList spriteList = key.spriteList;
            Sprite representativeSprite = spriteList.getSprite(representativeGroup.spriteIndex);
            setupTextures(context, shader, representativeSprite, whiteTexture);

            for (InstanceGroup group : groups.values()) {
                if (group.count > 0) {
                    group.upload(context);
                }
            }

            if (key.respond) {
                try (var _ = context.withColorMask(false, false, false, false); var _ = context.withDepthMode(
                        DepthMode.READ_WRITE); var _ = context.withDepthFunc(GL11.GL_LEQUAL); var _ = context
                                .withBlendMode(BlendMode.NONE); var _ = context.withSampleAlphaToCoverage(false); var _
                                        = context.withDrawBuffers(false)) {
                    drawAll(context);
                }

                try (var _ = context.withColorMask(true, true, true, true); var _ = context.withDepthMode(
                        DepthMode.READ_ONLY); var _ = context.withDepthFunc(GL11.GL_EQUAL); var _ = context
                                .withBlendMode(BlendMode.ALPHA); var _ = context.withSampleAlphaToCoverage(false); var _
                                        = context.withDrawBuffers(true)) {
                    drawAll(context);
                }
            } else {
                context.setDepthMode(key.depthTest ? key.depthWrite ? DepthMode.READ_WRITE : DepthMode.READ_ONLY
                        : DepthMode.NONE);

                if (key.blend) {
                    context.setBlendMode(BlendMode.ALPHA);
                    context.setSampleAlphaToCoverage(false);
                    drawAll(context);
                } else {
                    context.setBlendMode(BlendMode.NONE);
                    try (var _ = context.withSampleAlphaToCoverage(true)) {
                        drawAll(context);
                    }
                }
            }
        }

        private void drawAll(RenderContext context) {
            for (InstanceGroup group : groups.values()) {
                if (group.count > 0) {
                    group.draw(context);
                }
            }
        }

        private void setupTextures(RenderContext context, InstancedSpriteShader shader,
                Sprite sprite, Texture whiteTexture) {
            context.setTexture(0, key.texture);
            shader.setUniform(shader.locTexture0, 0);

            boolean useLighting = DebugFlags.draw_light && sprite.lighted;
            shader.setUniform(shader.locEnableLighting, useLighting);
            shader.setUniform(shader.locReplaceMode, !useLighting && !sprite.modulate_color);
            shader.setUniform(shader.locDesaturate, key.respond ? 0.5f : 0.0f);

            if (sprite.modulate_color) {
                shader.setUniform(shader.locModulateColor, true);
                shader.setUniform(shader.locEnableTeamColor, false);
                shader.setUniform(shader.locAlphaTestValue, 0.0f);
            } else {
                shader.setUniform(shader.locModulateColor, false);
                shader.setUniform(shader.locAlphaTestValue, key.respond ? 0.5f : 0.1f);
                if (key.teamTexture != null || key.respond) {
                    shader.setUniform(shader.locEnableTeamColor, true);
                    Texture teamTexture = key.respond ? sprite.respond_texture : key.teamTexture;
                    context.setTexture(1, teamTexture);
                    shader.setUniform(shader.locTexture1, 1);
                } else {
                    shader.setUniform(shader.locEnableTeamColor, false);
                }
            }

            if (key.bumpTexture != null) {
                shader.setUniform(shader.locEnableNormalMap, true);
                context.setTexture(2, key.bumpTexture);
                shader.setUniform(shader.locNormalMap, 2);
            } else {
                shader.setUniform(shader.locEnableNormalMap, false);
            }
        }

        void clear() {
            groups.values().forEach(InstanceGroup::clear);
        }

        @Override
        public void close() {
            groups.values().forEach(InstanceGroup::close);
            groups.clear();
        }
    }
}
