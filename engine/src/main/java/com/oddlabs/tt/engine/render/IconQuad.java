package com.oddlabs.tt.engine.render;

import com.oddlabs.util.Quad;
import java.util.Objects;

/// A quadrilateral region of a texture, typically defined by pixel coordinates. This is useful for defining icons from
/// a texture atlas.
public final class IconQuad extends Quad {
  private final Texture atlas;

  public IconQuad(float u1, float v1, float u2, float v2, int width, int height, Texture texture)
      throws IllegalArgumentException, NullPointerException {
    if (!Float.isFinite(u1) || !Float.isFinite(v1) || !Float.isFinite(u2) || !Float.isFinite(v2)) {
      throw new IllegalArgumentException("Texture coordinates must be finite numbers.");
    }
    if (u1 > u2 || v1 > v2) {
      throw new IllegalArgumentException(
          "u1/v1 must be less than or equal to u2/v2, but got: u1=" + u1 + ", v1=" + v1 + ", u2=" + u2 + ", v2=" + v2);
    }
    if (width < 0 || height < 0) {
      throw new IllegalArgumentException(
          "Width and height must be non-negative. Got: width=" + width + ", height=" + height);
    }
    super(u1, v1, u2, v2, width, height);
    this.atlas = texture;
  }

  public IconQuad(int x, int y, int width, int height, Texture texture)
      throws IndexOutOfBoundsException, NullPointerException {
    this(
        (float) x / getCheckedTexture(x, y, width, height, texture).getWidth(),
        (float) y / texture.getHeight(),
        (float) (x + width) / texture.getWidth(),
        (float) (y + height) / texture.getHeight(),
        width,
        height,
        texture);
  }

  private static Texture getCheckedTexture(int x, int y, int width, int height, Texture texture) {
    Objects.checkFromIndexSize(x, width, texture.getWidth());
    Objects.checkFromIndexSize(y, height, texture.getHeight());
    return texture;
  }

  public Texture getTexture() {
    return atlas;
  }
}
