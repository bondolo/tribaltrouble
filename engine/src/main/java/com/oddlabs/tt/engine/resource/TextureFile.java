package com.oddlabs.tt.engine.resource;

import com.oddlabs.tt.base.resource.File;
import com.oddlabs.tt.engine.image.GLImage;
import com.oddlabs.tt.engine.image.GLIntImage;
import com.oddlabs.tt.engine.render.RenderConfig;
import com.oddlabs.tt.engine.render.Texture;
import com.oddlabs.util.DXTImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.EXTTextureCompressionS3TC;
import org.lwjgl.opengl.EXTTextureSRGB;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GL30;

/// Resource descriptor specifying how a texture asset is loaded and configured in OpenGL.
public final class TextureFile extends File<Texture> {
  private static final Logger logger = Logger.getLogger(TextureFile.class.getSimpleName());

  /// Logical pixel format for texture assets.
  public enum Format {
    RGB,
    RGBA,
    RED
  }

  private static final String[] EXTENSIONS = {".dds", ".image", ".png", ".jpg", ".jpeg"};

  private final Format format;
  private final int min_filter;
  private final int mag_filter;
  private final int wrap_s;
  private final int wrap_t;
  private final int base_fadeout_level;
  private final int max_mipmap_level;
  private final float fadeout_factor;
  private final boolean max_alpha;
  // Non-color data (e.g., normal or height maps) that must not be sRGB-decoded
  private final boolean is_data;
  // Color data in sRGB space decoded to linear on hardware fetch
  private final boolean is_srgb;
  private final boolean is_dxt;

  /// Creates a texture file for 2D UI elements, icon atlases, and bitmap fonts using un-mipmapped bilinear filtering
  /// with edge clamping to prevent atlas bleeding.
  ///
  /// @param location asset path without file extension
  public static TextureFile forUI(String location) {
    return new TextureFile(
        location,
        Format.RGBA,
        GL11.GL_LINEAR,
        GL11.GL_LINEAR,
        GL12.GL_CLAMP_TO_EDGE,
        GL12.GL_CLAMP_TO_EDGE,
        RenderConfig.NO_MIPMAP_CUTOFF,
        10000,
        1.0f,
        false,
        false,
        true);
  }

  /// Creates a texture file for UI panels, backgrounds, or repeating UI images using un-mipmapped bilinear filtering
  /// with repeating coordinates.
  ///
  /// @param location asset path without file extension
  /// @param isSrgb true if the image contains sRGB color data to decode to linear space
  public static TextureFile forUIImage(String location, boolean isSrgb) {
    return new TextureFile(
        location,
        Format.RGBA,
        GL11.GL_LINEAR,
        GL11.GL_LINEAR,
        GL11.GL_REPEAT,
        GL11.GL_REPEAT,
        RenderConfig.NO_MIPMAP_CUTOFF,
        10000,
        1.0f,
        false,
        false,
        isSrgb);
  }

  /// Creates a texture file for visual effect particles using trilinear mipmapping and edge clamping.
  ///
  /// @param location asset path without file extension
  public static TextureFile forEffect(String location) {
    return new TextureFile(
        location,
        Format.RGBA,
        GL11.GL_LINEAR_MIPMAP_LINEAR,
        GL11.GL_LINEAR,
        GL12.GL_CLAMP_TO_EDGE,
        GL12.GL_CLAMP_TO_EDGE,
        RenderConfig.NO_MIPMAP_CUTOFF,
        10000,
        1.0f,
        false,
        false,
        true);
  }

  /// Creates a texture file for 3D models, units, buildings, and world scenery sprites using trilinear mipmapping,
  /// distance fadeout, and automatic sRGB decoding for non-data textures.
  ///
  /// @param location asset path without file extension
  /// @param clampEdges true to clamp coordinates (e.g. foliage); false to repeat
  /// @param maxAlpha true to maximize alpha values during mipmap processing
  /// @param isData true for non-color data (normal, bump, team masks) that should skip sRGB decoding
  public static TextureFile forModel(
      String location, Format format, boolean clampEdges, int mipmapCutoff, boolean maxAlpha, boolean isData) {
    int wrapMode = clampEdges ? GL12.GL_CLAMP_TO_EDGE : GL11.GL_REPEAT;
    boolean isSrgb = !isData;
    return new TextureFile(
        location,
        format,
        GL11.GL_LINEAR_MIPMAP_LINEAR,
        GL11.GL_LINEAR,
        wrapMode,
        wrapMode,
        mipmapCutoff,
        100000,
        0.1f,
        maxAlpha,
        isData,
        isSrgb);
  }

  /// Creates a texture file for 3D models using `Format.RGBA` when `hasAlpha` is true or `Format.RGB` otherwise.
  public static TextureFile forModel(
      String location, boolean hasAlpha, boolean clampEdges, int mipmapCutoff, boolean maxAlpha, boolean isData) {
    return forModel(location, hasAlpha ? Format.RGBA : Format.RGB, clampEdges, mipmapCutoff, maxAlpha, isData);
  }

  /// Creates a texture descriptor with explicit OpenGL filtering, wrapping, mipmap, and color-space settings.
  ///
  /// @param location asset path without file extension
  public TextureFile(
      String location,
      Format format,
      int min_filter,
      int mag_filter,
      int wrap_s,
      int wrap_t,
      int max_mipmap_level,
      int base_fadeout_level,
      float fadeout_factor,
      boolean max_alpha,
      boolean is_data,
      boolean is_srgb) {
    super(locateTexture(location));
    this.is_dxt = getURL().toString().endsWith(".dds");
    this.format = format;
    this.min_filter = min_filter;
    this.mag_filter = mag_filter;
    this.wrap_s = wrap_s;
    this.wrap_t = wrap_t;
    this.base_fadeout_level = base_fadeout_level;
    this.max_mipmap_level = max_mipmap_level;
    this.fadeout_factor = fadeout_factor;
    this.max_alpha = max_alpha;
    this.is_data = is_data;
    this.is_srgb = is_srgb;
  }

  /// Checks whether a texture asset exists at `location` (without file extension) with any supported extension.
  public static boolean exists(String location) {
    return Arrays.stream(EXTENSIONS).map(ext -> locate(location + ext)).anyMatch(Optional::isPresent);
  }

  private static URI locateTexture(String location) {
    return Arrays.stream(EXTENSIONS)
        .map(ext -> locate(location + ext))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .findFirst()
        .orElseThrow(() -> {
          String msg =
              "Failed to locate texture: " + location + " (tried extensions: " + Arrays.toString(EXTENSIONS) + ")";
          logger.log(Level.SEVERE, msg);
          return new IllegalArgumentException(msg);
        });
  }

  public boolean isDXTImage() {
    return is_dxt;
  }

  public DXTImage getDXTImage() {
    try {
      if (logger.isLoggable(Level.FINE)) {
        logger.fine("Loading DXT image from: " + getURL());
      }
      return DXTImage.read(getURL());
    } catch (IOException e) {
      logger.log(Level.SEVERE, "Failed to read DXT image: " + getURL(), e);
      throw new UncheckedIOException(e);
    }
  }

  public GLImage getImage() {
    try {
      if (logger.isLoggable(Level.FINE)) {
        logger.fine("Loading image from: " + getURL());
      }
      return GLIntImage.loadImage(getURL());
    } catch (IOException e) {
      logger.log(Level.SEVERE, "Failed to load image: " + getURL(), e);
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public Texture get() {
    return new Texture(this);
  }

  @Override
  public boolean equals(@Nullable Object o) {
    return o instanceof TextureFile other
        && format == other.format
        && min_filter == other.min_filter
        && mag_filter == other.mag_filter
        && max_mipmap_level == other.max_mipmap_level
        && wrap_s == other.wrap_s
        && wrap_t == other.wrap_t
        && base_fadeout_level == other.base_fadeout_level
        && fadeout_factor == other.fadeout_factor
        && max_alpha == other.max_alpha
        && is_srgb == other.is_srgb
        && is_data == other.is_data
        && super.equals(o);
  }

  public int getInternalFormat() {
    if (is_dxt) {
      return switch (getDXTImage().getFourCC()) {
        case DXTImage.FOURCC_DXT1 ->
            (is_srgb && !is_data)
                ? EXTTextureSRGB.GL_COMPRESSED_SRGB_S3TC_DXT1_EXT
                : EXTTextureCompressionS3TC.GL_COMPRESSED_RGB_S3TC_DXT1_EXT;
        case DXTImage.FOURCC_DXT5 ->
            (is_srgb && !is_data)
                ? EXTTextureSRGB.GL_COMPRESSED_SRGB_ALPHA_S3TC_DXT5_EXT
                : EXTTextureCompressionS3TC.GL_COMPRESSED_RGBA_S3TC_DXT5_EXT;
        default -> {
          String msg =
              "Unsupported DXT format (FourCC): "
                  + Integer.toHexString(getDXTImage().getFourCC())
                  + " for texture: "
                  + getURL();
          logger.severe(msg);
          throw new IllegalArgumentException(msg);
        }
      };
    }

    boolean srgb = is_srgb && !is_data;
    return switch (format) {
      case RGB -> srgb ? GL21.GL_SRGB8 : GL11.GL_RGB8;
      case RGBA -> srgb ? GL21.GL_SRGB8_ALPHA8 : GL11.GL_RGBA8;
      case RED -> GL30.GL_R8;
    };
  }

  public Format getFormat() {
    return format;
  }

  public boolean isData() {
    return is_data;
  }

  public int getMinFilter() {
    return min_filter;
  }

  public int getMagFilter() {
    return mag_filter;
  }

  public int getWrapS() {
    return wrap_s;
  }

  public int getWrapT() {
    return wrap_t;
  }

  public int getBaseFadeoutLevel() {
    return base_fadeout_level;
  }

  public int getMaxMipmapLevel() {
    return max_mipmap_level;
  }

  public float getFadeoutFactor() {
    return fadeout_factor;
  }

  public boolean hasMaxAlpha() {
    return max_alpha;
  }
}
