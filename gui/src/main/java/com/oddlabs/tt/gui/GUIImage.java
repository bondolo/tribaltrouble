package com.oddlabs.tt.gui;

import com.oddlabs.tt.engine.render.IconQuad;
import com.oddlabs.tt.engine.render.Texture;
import com.oddlabs.tt.engine.resource.Resources;
import com.oddlabs.tt.engine.resource.TextureFile;

/// Displays a texture or a region of a texture.
public final class GUIImage extends GUIIcon {

  /// Displays a texture file region; negative `width` or `height` uses the texture's dimensions.
  public GUIImage(int width, int height, float u1, float v1, float u2, float v2, String texture_name) {
    this(width, height, u1, v1, u2, v2, texture_name, true);
  }

  public GUIImage(int width, int height, float u1, float v1, float u2, float v2, String texture_name, boolean isSrgb) {
    this(width, height, u1, v1, u2, v2, Resources.findResource(TextureFile.forUIImage(texture_name, isSrgb)));
  }

  private GUIImage(int width, int height, float u1, float v1, float u2, float v2, Texture texture) {
    boolean useTexture = width < 0 || height < 0;
    int useHeight = useTexture ? texture.getHeight() : height;
    int useWidth = useTexture ? texture.getWidth() : width;
    super(new IconQuad(u1, v1, u2, v2, useWidth, useHeight, texture));
  }
}
