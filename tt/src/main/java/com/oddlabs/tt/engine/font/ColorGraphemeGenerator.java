package com.oddlabs.tt.engine.font;

import com.oddlabs.tt.procedural.TextureGenerator;
import com.oddlabs.tt.render.Texture;
import com.oddlabs.tt.resource.GLIntImage;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.awt.*;

import java.awt.image.BufferedImage;
import java.text.BreakIterator;
import java.util.List;
import java.util.logging.Logger;

/**
 * Generates an OpenGL texture for a single color grapheme cluster string using platform color fonts.
 * Uses AWT rendering on a background virtual thread.
 */
public final class ColorGraphemeGenerator extends TextureGenerator {
    private static final Logger logger = Logger.getLogger(ColorGraphemeGenerator.class.getName());
    private static final int TEXT_SIZE = 48;
    private static final int TEXTURE_SIZE = 64;
    private static final List<String> EMOJI_FONT_NAMES = List.of("Apple Color Emoji", "Segoe UI Emoji",
            "Noto Color Emoji", "Google Sans", "Arial");
    private static final List<java.awt.@NonNull Font> EMOJI_FONTS = EMOJI_FONT_NAMES.stream()
            .map(name -> new java.awt.Font(name, java.awt.Font.PLAIN, TEXT_SIZE))
            .toList();
    private static final java.awt.Font FALLBACK_FONT = new java.awt.Font(
            java.awt.Font.SANS_SERIF, java.awt.Font.PLAIN, TEXT_SIZE);

    private final @NonNull String grapheme;
    private final Thread worker;
    private final @NonNull GLIntImage glIntImage;

    public ColorGraphemeGenerator(@NonNull String grapheme) {
        BreakIterator iterator = BreakIterator.getCharacterInstance();
        iterator.setText(grapheme);
        int count = 0;
        while (iterator.next() != BreakIterator.DONE) {
            count++;
        }
        assert count == 1 : "grapheme must contain exactly one cluster";
        this.grapheme = grapheme;
        this.glIntImage = new GLIntImage(TEXTURE_SIZE, TEXTURE_SIZE, GL11.GL_RGBA);
        this.worker = Thread.startVirtualThread(() -> renderColorGrapheme(grapheme, glIntImage));
    }

    public ColorGraphemeGenerator(int codepoint) {
        this(Character.toString(codepoint));
    }

    private static void renderColorGrapheme(@NonNull String grapheme, @NonNull GLIntImage glIntImage) {
        BufferedImage bufferedImage = new BufferedImage(TEXTURE_SIZE, TEXTURE_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = bufferedImage.createGraphics();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

            java.awt.Font selectedFont = selectEmojiFont(grapheme);

            g2d.setFont(selectedFont);
            java.awt.FontMetrics metrics = g2d.getFontMetrics(selectedFont);
            int x = (TEXTURE_SIZE - metrics.stringWidth(grapheme)) / 2;
            int y = ((TEXTURE_SIZE - metrics.getHeight()) / 2) + metrics.getAscent();

            // Draw a soft black border for contrast
            g2d.setColor(new java.awt.Color(0, 0, 0, 160));
            g2d.drawString(grapheme, x + 1, y + 1);
            g2d.drawString(grapheme, x - 1, y - 1);
            g2d.drawString(grapheme, x + 1, y - 1);
            g2d.drawString(grapheme, x - 1, y + 1);

            g2d.setColor(java.awt.Color.WHITE);
            g2d.drawString(grapheme, x, y);
        } finally {
            g2d.dispose();
        }

        for (int py = 0; py < TEXTURE_SIZE; py++) {
            for (int px = 0; px < TEXTURE_SIZE; px++) {
                // flip y-axis
                int argb = bufferedImage.getRGB(px, TEXTURE_SIZE - 1 - py);
                glIntImage.putPixel(px, py, argb);
            }
        }
    }

    private static java.awt.@NonNull Font selectEmojiFont(@NonNull String grapheme) {
        for (java.awt.Font font : EMOJI_FONTS) {
            if (font.canDisplayUpTo(grapheme) == -1) {
                return font;
            }
        }
        return FALLBACK_FONT;
    }

    @Override
    protected @NonNull Texture @NonNull [] generate() {
        try {
            worker.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        // Use uncompressed RGBA8 to avoid driver issues with small dynamic textures
        Texture texture = new Texture(glIntImage, GL11.GL_RGBA8,
                GL11.GL_LINEAR, GL11.GL_LINEAR,
                GL12.GL_CLAMP_TO_EDGE, GL12.GL_CLAMP_TO_EDGE);
        return new Texture[]{texture};
    }

    @Override
    public int hashCode() {
        return grapheme.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        return this == o || (o instanceof ColorGraphemeGenerator cgg && grapheme.equals(cgg.grapheme));
    }
}
