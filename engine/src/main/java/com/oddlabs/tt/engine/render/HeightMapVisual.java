package com.oddlabs.tt.engine.render;

import com.oddlabs.tt.simulation.landscape.HeightMap;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

/// Client-side visual representation of the height map. Manages the OpenGL Texture state and propagates updates to the
/// GPU.
public final class HeightMapVisual {
  private final Texture heightTexture;

  public HeightMapVisual(HeightMap heightMap) {
    this.heightTexture =
        new Texture(
            heightMap.getHeightData(),
            heightMap.getGridUnitsPerWorld(),
            heightMap.getGridUnitsPerWorld(),
            GL30.GL_R32F,
            GL11.GL_LINEAR,
            GL11.GL_LINEAR,
            GL11.GL_REPEAT);
  }

  public Texture getHeightTexture() {
    return heightTexture;
  }

  public void editHeight(int x, int y, float height) {
    heightTexture.update(x, y, 1, 1, height);
  }
}
