package com.oddlabs.tt.render;

import com.oddlabs.tt.resource.GLImage;
import com.oddlabs.tt.util.GLUtils;
import com.oddlabs.util.DXTImage;
import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;

import java.nio.ByteBuffer;

/**
 * Represents a 2D Array Texture (GL_TEXTURE_2D_ARRAY).
 */
public final class TextureArray extends Texture {
    private final int depth;

    public TextureArray(int width, int height, int depth, int internal_format, int min_filter, int mag_filter,
            int wrap) {
        super(GL30.GL_TEXTURE_2D_ARRAY, width, height, min_filter, mag_filter, wrap, wrap, 1000);
        this.depth = depth;

        int format = switch (internal_format) {
            case GL11.GL_RGB, GL11.GL_RGB8 -> GL11.GL_RGB;
            case GL11.GL_RED, GL30.GL_R8 -> GL11.GL_RED;
            default -> GL11.GL_RGBA;
        };

        // Allocate storage for all mipmap levels
        int numLevels = 1 + (int) Math.floor(Math.log(Math.max(width, height)) / Math.log(2));
        int w = width;
        int h = height;
        GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, getHandle());
        for (int level = 0; level < numLevels; level++) {
            GL12.glTexImage3D(GL30.GL_TEXTURE_2D_ARRAY, level, internal_format, w, h, depth, 0, format,
                    GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
            w = Math.max(1, w / 2);
            h = Math.max(1, h / 2);
        }
        GLUtils.checkAndThrow("TextureArray storage allocation");
    }

    /**
     * Uploads a single layer to the texture array.
     */
    public void uploadLayer(int layerIndex, @NonNull GLImage[] mipmaps, int internal_format) {
        if (layerIndex < 0 || layerIndex >= depth) {
            throw new IndexOutOfBoundsException("Layer index " + layerIndex + " out of bounds [0, " + depth + ")");
        }

        GL11.glBindTexture(getTarget(), getHandle());
        for (int level = 0; level < mipmaps.length; level++) {
            GLImage mip = mipmaps[level];
            ByteBuffer pixels = mip.getPixels();
            pixels.rewind();

            GL12.glTexSubImage3D(getTarget(), level, 0, 0, layerIndex, mip.getWidth(), mip.getHeight(), 1,
                    mip.getGLFormat(), mip.getGLType(), pixels);
        }
        GLUtils.checkAndThrow("TextureArray uploadLayer level " + layerIndex);
    }

    /**
     * Uploads a single layer to the texture array using compressed data.
     */
    public void uploadLayer(int layerIndex, @NonNull DXTImage dxt, int internal_format) {
        if (layerIndex < 0 || layerIndex >= depth) {
            throw new IndexOutOfBoundsException("Layer index " + layerIndex + " out of bounds [0, " + depth + ")");
        }

        GL11.glBindTexture(getTarget(), getHandle());
        for (int level = 0; level < dxt.getNumMipMaps(); level++) {
            ByteBuffer pixels = dxt.getMipMap(level);
            pixels.rewind();

            GL13.glCompressedTexSubImage3D(getTarget(), level, 0, 0, layerIndex, dxt.getWidth(level),
                    dxt.getHeight(level), 1, internal_format, pixels);
        }
        GLUtils.checkAndThrow("TextureArray uploadLayer DXT level " + layerIndex);
    }

    @Override
    public int getDepth() {
        return depth;
    }
}
