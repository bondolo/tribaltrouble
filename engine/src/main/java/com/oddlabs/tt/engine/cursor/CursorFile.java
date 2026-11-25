package com.oddlabs.tt.engine.cursor;

import com.oddlabs.procedural.Layer;
import com.oddlabs.tt.base.resource.File;
import com.oddlabs.tt.engine.image.GLIntImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import org.jspecify.annotations.Nullable;

/// A resource file for loading and scaling hardware cursors.
public final class CursorFile extends File<Cursor> {

  private final int xHot, yHot;
  private final float scale;

  public CursorFile(String source, int xHot, int yHot) {
    this(source, xHot, yHot, 1.0f);
  }

  public CursorFile(String source, int xHot, int yHot, float scale) {
    super(source);
    this.xHot = xHot;
    this.yHot = yHot;
    this.scale = scale;
  }

  @Override
  public boolean equals(@Nullable Object o) {
    return o instanceof CursorFile other
        && super.equals(o)
        && xHot == other.xHot
        && yHot == other.yHot
        && Float.compare(scale, other.scale) == 0;
  }

  @Override
  public Cursor get() {
    try {
      GLIntImage image = GLIntImage.loadImage(getURL());
      if (scale != 1.0f) {
        Layer layer = image.toLayer();
        int newWidth = Math.max(1, Math.round(image.getWidth() * scale));
        int newHeight = Math.max(1, Math.round(image.getHeight() * scale));
        layer.scale(newWidth, newHeight);
        image = new GLIntImage(layer);
      }
      return new Cursor(image, Math.round(xHot * scale), Math.round(yHot * scale));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
