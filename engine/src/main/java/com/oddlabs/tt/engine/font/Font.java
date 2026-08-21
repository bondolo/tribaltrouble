package com.oddlabs.tt.engine.font;

import com.oddlabs.tt.engine.render.Texture;
import com.oddlabs.tt.engine.resource.Resources;
import com.oddlabs.tt.engine.resource.TextureFile;
import com.oddlabs.util.FontInfo;
import com.oddlabs.util.Quad;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.text.BreakIterator;
import java.util.Map;

/**
 * Represents a pre-rendered font asset at runtime, providing glyph metadata and layout metrics.
 */
public final class Font {
    private final Map<String, Quad> key_map;
    private final Texture texture;
    private final int x_border;
    private final int y_border;
    private final int height;
    private final int max_ascension;
    private final int max_descension;

    public Font(FontInfo font_info) {
        this.key_map = font_info.getKeyMap();
        TextureFile file = new TextureFile(font_info.getTextureName(),
                GL11.GL_RGBA,
                GL11.GL_LINEAR,
                GL11.GL_LINEAR,
                GL12.GL_CLAMP_TO_EDGE,
                GL12.GL_CLAMP_TO_EDGE);
        this.texture = Resources.findResource(file);
        this.x_border = font_info.getBorderX();
        this.y_border = font_info.getBorderY();
        this.height = font_info.getHeight();
        this.max_ascension = font_info.getMaxAscension();
        this.max_descension = font_info.getMaxDescension();
    }

    public @Nullable Quad getQuad(String grapheme) {
        return key_map.get(grapheme);
    }

    public @Nullable Quad getQuad(int codepoint) {
        return getQuad(Character.toString(codepoint));
    }

    public int getXBorder() {
        return x_border;
    }

    public int getYBorder() {
        return y_border;
    }

    public int getHeight() {
        return height;
    }

    public int getMaxAscension() {
        return max_ascension;
    }

    public int getMaxDescension() {
        return max_descension;
    }

    public Texture getTexture() {
        return texture;
    }

    public int getWidestCodepoint(CharSequence text) {
        assert !text.isEmpty() : "Empty CharSequence";

        return text.codePoints().reduce(Character.codePointAt(text, 0), (current, codePoint) -> {
            var widest = getQuad(current);
            var quad = getQuad(codePoint);

            return null != quad && null != widest && quad.getWidth() > widest.getWidth() ? codePoint : current;
        });
    }

    public int getWidth(CharSequence text) {
        if (text.isEmpty())
            return 0;

        int width = 0;
        BreakIterator iterator = BreakIterator.getCharacterInstance();
        iterator.setText(text.toString());
        int start = iterator.first();
        for (int end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator.next()) {
            String grapheme = text.subSequence(start, end).toString();
            Quad quad = getQuad(grapheme);
            width += (null != quad ? quad.getWidth() - x_border : 0);
        }
        return width + x_border;
    }
}
