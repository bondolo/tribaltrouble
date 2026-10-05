package com.oddlabs.tt.engine.render;

import com.oddlabs.tt.engine.render.shader.VertexLayout;
import com.oddlabs.tt.engine.render.state.CullMode;
import com.oddlabs.tt.engine.render.state.DepthMode;
import com.oddlabs.tt.engine.render.state.RenderContext;
import com.oddlabs.tt.engine.util.GLUtils;
import com.oddlabs.tt.engine.vbo.VertexArray;
import com.oddlabs.util.Color;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

/**
 * Renders 2D GUI elements using a shader-based multi-texture batching system.
 * Supports multi-texturing to batch draw calls across different textures.
 */
public final class GUIRenderer implements AutoCloseable {
    private static final int MAX_QUADS = 2048;
    private static final int VERTICES_PER_QUAD = 4;
    private static final int INDICES_PER_QUAD = 6;
    private static final int MAX_TEXTURES = 8;

    private record ScissorRect(int x, int y, int width, int height) {
    }

    private final GUIShader shader;
    private final MatrixStack matrixStack = new MatrixStack(); // No flush callback
    private final Matrix4f projectionMatrix = new Matrix4f();
    private final VertexLayout<GUIShader.Attribute> layout;

    private final VertexArray vao;
    private final int vbo;
    private final int ibo;
    private final ByteBuffer vertexBuffer;

    // Texture batching state
    private final @Nullable Texture[] currentTextures = new Texture[MAX_TEXTURES];
    private int textureCount = 0;
    private int lastBoundTextureCount = 0;
    private int quadCount = 0;

    // Modulation stack
    private final Deque<Color.Linear> modulationStack = new ArrayDeque<>();
    private Color.Linear currentModulation = Color.Linear.WHITE;

    // Scissor stack (physical framebuffer coordinates)
    private final Deque<ScissorRect> scissorStack = new ArrayDeque<>();
    private float scaleX = 1.0f;
    private float scaleY = 1.0f;
    private float lastWidth = -1.0f;
    private float lastHeight = -1.0f;

    private @Nullable RenderContext currentContext;

    public GUIRenderer() {
        this.shader = new GUIShader();
        this.layout = new VertexLayout<>(
                GUIShader.Attribute.POSITION,
                GUIShader.Attribute.COLOR,
                GUIShader.Attribute.TEX_COORD,
                GUIShader.Attribute.TEX_INDEX
        );
        this.modulationStack.push(Color.Linear.WHITE);

        this.vao = new VertexArray();
        this.vbo = GL15.glGenBuffers();
        this.ibo = GL15.glGenBuffers();
        this.vertexBuffer = BufferUtils.createByteBuffer(MAX_QUADS * VERTICES_PER_QUAD * layout.getStride());

        setupBuffers(ibo);
    }

    public void pushModulation(Color.Linear modulation) {
        flush();
        modulationStack.push(modulation);
        currentModulation = modulation;
    }

    public void popModulation() {
        flush();
        modulationStack.pop();
        currentModulation = modulationStack.peek();
    }

    private void setupBuffers(int ibo) {
        vao.bind();

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertexBuffer.capacity(), GL15.GL_STREAM_DRAW);

        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ibo);
        ByteBuffer indexBuffer = BufferUtils.createByteBuffer(MAX_QUADS * INDICES_PER_QUAD * Short.BYTES);
        for (int i = 0; i < MAX_QUADS; i++) {
            int offset = i * VERTICES_PER_QUAD;
            indexBuffer.putShort((short) (offset));
            indexBuffer.putShort((short) (offset + 1));
            indexBuffer.putShort((short) (offset + 2));
            indexBuffer.putShort((short) (offset + 2));
            indexBuffer.putShort((short) (offset + 3));
            indexBuffer.putShort((short) (offset));
        }
        indexBuffer.flip();
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL15.GL_STATIC_DRAW);

        layout.bind(shader);

        vao.unbind();
    }

    public void renderFrame(RenderContext context, float width, float height,
            Runnable frameCommands) {
        if (width <= 0 || height <= 0) return;
        GLUtils.checkGLError("Before GUI Render");
        this.currentContext = context;

        int fbWidth = context.getViewportWidth();
        int fbHeight = context.getViewportHeight();
        this.scaleX = (fbWidth > 0) ? (float) fbWidth / width : 1.0f;
        this.scaleY = (fbHeight > 0) ? (float) fbHeight / height : 1.0f;

        try (var _ = shader.use(); var _ = context.withDepthMode(DepthMode.NONE); var _ = context.withCullMode(
                CullMode.NONE)) {

            if (width != lastWidth || height != lastHeight) {
                projectionMatrix.identity().ortho(0, width, 0, height, -1, 1);
                shader.setProjectionMatrix(projectionMatrix);
                lastWidth = width;
                lastHeight = height;
            }

            matrixStack.clear();
            modulationStack.clear();
            modulationStack.push(Color.Linear.WHITE);
            currentModulation = Color.Linear.WHITE;

            scissorStack.clear();
            context.setScissorTest(false);

            frameCommands.run();

            flush();
        } finally {
            if (this.currentContext != null) {
                this.currentContext.setScissorTest(false);
                for (int i = 0; i < lastBoundTextureCount; i++) {
                    this.currentContext.setTexture(i, null);
                }
            }
            lastBoundTextureCount = 0;
            scissorStack.clear();
            this.currentContext = null;
        }
    }

    public void drawColoredQuad(float x, float y, float w, float h, Color.Linear color) {
        if (quadCount >= MAX_QUADS) {
            flush();
        }
        // Use -1 for "no texture"
        putQuad(x, y, w, h, -1, -1, -1, -1, -1f, color.r(), color.g(), color.b(), color.a());
    }

    public void drawModeIcon(ModeIconQuads iconQuad, ModeIconQuads.Mode skinMode, float x, float y) {
        drawIcon(iconQuad.quad(skinMode), x, y);
    }

    public void drawIcon(IconQuad iconQuad, float x, float y) {
        drawTexture(iconQuad.getTexture(), x, y, iconQuad.getWidth(), iconQuad.getHeight(), iconQuad.getU1(), iconQuad
                .getV1(), iconQuad.getU2(), iconQuad.getV2(), Color.Linear.WHITE);
    }

    public void drawIcon(IconQuad iconQuad, float x, float y, Color tint) {
        drawTexture(iconQuad.getTexture(), x, y, iconQuad.getWidth(), iconQuad.getHeight(), iconQuad.getU1(), iconQuad
                .getV1(), iconQuad.getU2(), iconQuad.getV2(), tint);
    }

    public void drawIcon(IconQuad iconQuad, float x, float y, Color.Linear tint) {
        drawTexture(iconQuad.getTexture(), x, y, iconQuad.getWidth(), iconQuad.getHeight(), iconQuad.getU1(), iconQuad
                .getV1(), iconQuad.getU2(), iconQuad.getV2(), tint);
    }

    public void drawIcon(IconQuad iconQuad, float x, float y, float w, float h) {
        drawTexture(iconQuad.getTexture(), x, y, w, h, iconQuad.getU1(), iconQuad.getV1(), iconQuad.getU2(), iconQuad
                .getV2(), Color.Linear.WHITE);
    }

    public void drawTexture(Texture texture, float x, float y, float w, float h, float u1, float v1, float u2,
            float v2, Color tint) {
        if (quadCount >= MAX_QUADS) {
            flush();
        }

        float texIndex = getTextureIndex(texture);
        float r;
        float g;
        float b;
        if (tint instanceof Color.Linear linear) {
            r = linear.r();
            g = linear.g();
            b = linear.b();
        } else {
            r = Color.toLinear(tint.r());
            g = Color.toLinear(tint.g());
            b = Color.toLinear(tint.b());
        }
        putQuad(x, y, w, h, u1, v1, u2, v2, texIndex, r, g, b, tint.a());
    }

    public void drawTexture(Texture texture, float x, float y, float w, float h, float u1, float v1, float u2,
            float v2, Color.Linear tint) {
        if (quadCount >= MAX_QUADS) {
            flush();
        }

        float texIndex = getTextureIndex(texture);
        putQuad(x, y, w, h, u1, v1, u2, v2, texIndex, tint.r(), tint.g(), tint.b(), tint.a());
    }

    private float getTextureIndex(Texture texture) {
        for (int i = 0; i < textureCount; i++) {
            if (currentTextures[i].getHandle() == texture.getHandle()) {
                return (float) i;
            }
        }

        if (textureCount >= MAX_TEXTURES) {
            flush();
            return getTextureIndex(texture); // Try again in empty batch
        }

        currentTextures[textureCount] = texture;
        return (float) textureCount++;
    }

    private void putQuad(float x, float y, float w, float h, float u1, float v1, float u2, float v2, float texIndex,
            float tintR, float tintG, float tintB, float tintA) {
        Matrix4f mat = matrixStack.current();

        float r = tintR * currentModulation.r();
        float g = tintG * currentModulation.g();
        float b = tintB * currentModulation.b();
        float a = tintA * currentModulation.a();

        // Transform vertices on CPU
        float x1 = x;
        float y1 = y;
        float x2 = x + w;
        float y2 = y + h;

        // P1 (x1, y1)
        vertexBuffer.putFloat(mat.m00() * x1 + mat.m10() * y1 + mat.m30())
                .putFloat(mat.m01() * x1 + mat.m11() * y1 + mat.m31())
                .putFloat(mat.m02() * x1 + mat.m12() * y1 + mat.m32())
                .putFloat(r).putFloat(g).putFloat(b).putFloat(a)
                .putFloat(u1).putFloat(v1).putFloat(texIndex);

        // P2 (x2, y1)
        vertexBuffer.putFloat(mat.m00() * x2 + mat.m10() * y1 + mat.m30())
                .putFloat(mat.m01() * x2 + mat.m11() * y1 + mat.m31())
                .putFloat(mat.m02() * x2 + mat.m12() * y1 + mat.m32())
                .putFloat(r).putFloat(g).putFloat(b).putFloat(a)
                .putFloat(u2).putFloat(v1).putFloat(texIndex);

        // P3 (x2, y2)
        vertexBuffer.putFloat(mat.m00() * x2 + mat.m10() * y2 + mat.m30())
                .putFloat(mat.m01() * x2 + mat.m11() * y2 + mat.m31())
                .putFloat(mat.m02() * x2 + mat.m12() * y2 + mat.m32())
                .putFloat(r).putFloat(g).putFloat(b).putFloat(a)
                .putFloat(u2).putFloat(v2).putFloat(texIndex);

        // P4 (x1, y2)
        vertexBuffer.putFloat(mat.m00() * x1 + mat.m10() * y2 + mat.m30())
                .putFloat(mat.m01() * x1 + mat.m11() * y2 + mat.m31())
                .putFloat(mat.m02() * x1 + mat.m12() * y2 + mat.m32())
                .putFloat(r).putFloat(g).putFloat(b).putFloat(a)
                .putFloat(u1).putFloat(v2).putFloat(texIndex);

        quadCount++;
    }

    public void flush() {
        if (quadCount == 0) return;
        RenderContext context = currentContext;
        if (context == null) return;

        // Bind all active textures and unbind unused units in the sampler array to avoid conflicts
        for (int i = 0; i < textureCount; i++) {
            context.setTexture(i, currentTextures[i]);
        }
        for (int i = textureCount; i < lastBoundTextureCount; i++) {
            context.setTexture(i, null);
        }
        lastBoundTextureCount = textureCount;

        vertexBuffer.flip();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, vertexBuffer);

        vao.bind();
        GL11.glDrawElements(GL11.GL_TRIANGLES, quadCount * INDICES_PER_QUAD, GL11.GL_UNSIGNED_SHORT, 0);
        vao.unbind();

        quadCount = 0;
        textureCount = 0;
        Arrays.fill(currentTextures, null);
        vertexBuffer.clear();
    }

    public void pushClip(float x, float y, float w, float h) {
        flush();

        int sx1 = Math.round(x * scaleX);
        int sy1 = Math.round(y * scaleY);
        int sx2 = Math.round((x + w) * scaleX);
        int sy2 = Math.round((y + h) * scaleY);

        ScissorRect rect;
        if (scissorStack.isEmpty()) {
            rect = new ScissorRect(sx1, sy1, Math.max(0, sx2 - sx1), Math.max(0, sy2 - sy1));
            if (currentContext != null) {
                currentContext.setScissorTest(true);
            }
        } else {
            ScissorRect parent = scissorStack.peek();
            assert parent != null;
            int newX1 = Math.max(parent.x(), sx1);
            int newY1 = Math.max(parent.y(), sy1);
            int newX2 = Math.min(parent.x() + parent.width(), sx2);
            int newY2 = Math.min(parent.y() + parent.height(), sy2);
            rect = new ScissorRect(newX1, newY1, Math.max(0, newX2 - newX1), Math.max(0, newY2 - newY1));
        }
        scissorStack.push(rect);
        if (currentContext != null) {
            currentContext.setScissor(rect.x(), rect.y(), rect.width(), rect.height());
        }
    }

    public void popClip() {
        flush();
        if (!scissorStack.isEmpty()) {
            scissorStack.pop();
        }
        if (scissorStack.isEmpty()) {
            if (currentContext != null) {
                currentContext.setScissorTest(false);
            }
        } else {
            ScissorRect parent = scissorStack.peek();
            assert parent != null;
            if (currentContext != null) {
                currentContext.setScissor(parent.x(), parent.y(), parent.width(), parent.height());
            }
        }
    }

    public MatrixStack getMatrixStack() {
        return matrixStack;
    }

    @Override
    public void close() {
        shader.close();
        vao.close();
        GL15.glDeleteBuffers(vbo);
        GL15.glDeleteBuffers(ibo);
    }
}
