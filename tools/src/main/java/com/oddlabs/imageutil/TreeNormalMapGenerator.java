package com.oddlabs.imageutil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Generates tangent-space normal maps with wood material specular metadata for tree textures.
 */
public final class TreeNormalMapGenerator {

    private TreeNormalMapGenerator() {
    }

    /**
     * Generates a normal map from an input diffuse texture and writes it as an RGBA PNG.
     *
     * @param inputPath path to the input diffuse image
     * @param outputPath path for the output normal map image
     * @param strength bump depth scale for normal map calculation
     * @param isTropical true if processing tropical trees (smooth jungle and extra-smooth palm)
     * @throws IOException if reading or writing images fails
     */
    public static void generateNormalMap(Path inputPath, Path outputPath, float strength, boolean isTropical)
            throws IOException {
        BufferedImage diffuse = ImageIO.read(inputPath.toFile());
        if (diffuse == null) {
            throw new IOException("Failed to load image from " + inputPath);
        }

        int width = diffuse.getWidth();
        int height = diffuse.getHeight();

        float[][] luminance = new float[width][height];
        int[][] alphaValues = new int[width][height];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = diffuse.getRGB(x, y);
                int a = (argb >>> 24) & 0xFF;
                int r = (argb >>> 16) & 0xFF;
                int g = (argb >>> 8) & 0xFF;
                int b = argb & 0xFF;

                alphaValues[x][y] = a;
                // Standard relative luminance
                luminance[x][y] = (0.299f * r + 0.587f * g + 0.114f * b) / 255.0f;
            }
        }

        BufferedImage normalImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < height; y++) {
            int yPrev = (y - 1 + height) % height;
            int yNext = (y + 1) % height;
            float v = (float) y / height;

            // Palm stem (V > 0.75 in tropical trees) is calibrated even smoother
            float localStrength = strength;
            if (isTropical && v > 0.75f) {
                localStrength = strength * 0.7f;
            }

            for (int x = 0; x < width; x++) {
                int a = alphaValues[x][y];
                if (a < 128) {
                    // Transparent foliage background: flat neutral normal (0, 0, 1), zero specular
                    normalImage.setRGB(x, y, (0 << 24) | (128 << 16) | (128 << 8) | 255);
                    continue;
                }

                int xPrev = (x - 1 + width) % width;
                int xNext = (x + 1) % width;

                float tl = luminance[xPrev][yPrev];
                float t = luminance[x][yPrev];
                float tr = luminance[xNext][yPrev];
                float l = luminance[xPrev][y];
                float r = luminance[xNext][y];
                float bl = luminance[xPrev][yNext];
                float b = luminance[x][yNext];
                float br = luminance[xNext][yNext];

                // Sobel operator
                float dX = (tr + 2.0f * r + br) - (tl + 2.0f * l + bl);
                float dY = (bl + 2.0f * b + br) - (tl + 2.0f * t + tr);

                // Normal vector tilts into depressions (negative gradient)
                float nx = -dX * localStrength;
                float ny = -dY * localStrength;
                float nz = 1.0f;

                float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
                nx /= len;
                ny /= len;
                nz /= len;

                int outR = Math.clamp(Math.round((nx * 0.5f + 0.5f) * 255.0f), 0, 255);
                int outG = Math.clamp(Math.round((ny * 0.5f + 0.5f) * 255.0f), 0, 255);
                int outB = Math.clamp(Math.round((nz * 0.5f + 0.5f) * 255.0f), 0, 255);

                // Wood specular material: subtle sheen on exposed bark ridges, zero in crevices
                float lum = luminance[x][y];
                float spec = Math.clamp((lum - 0.20f) * 0.10f, 0.0f, 0.08f);
                int outA = Math.clamp(Math.round(spec * 255.0f), 0, 255);

                normalImage.setRGB(x, y, (outA << 24) | (outR << 16) | (outG << 8) | outB);
            }
        }

        Files.createDirectories(outputPath.getParent());
        ImageIO.write(normalImage, "PNG", outputPath.toFile());
    }

    public static void main(String... args) throws IOException {
        if (args.length < 3 || args.length % 3 != 0) {
            System.err.println(
                    "Usage: TreeNormalMapGenerator <input1.png> <output1.png> <strength1> [<input2.png> <output2.png> <strength2> ...]");
            System.exit(1);
        }

        for (int i = 0; i < args.length; i += 3) {
            Path input = Path.of(args[i]);
            Path output = Path.of(args[i + 1]);
            float strength = Float.parseFloat(args[i + 2]);
            boolean isTropical = input.getFileName().toString().contains("trees")
                    && !input.getFileName().toString().contains("viking");
            generateNormalMap(input, output, strength, isTropical);
        }
    }
}
