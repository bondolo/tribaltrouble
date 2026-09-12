package com.oddlabs.graphservlet;

import jakarta.inject.Singleton;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * Generates PNG telemetry charts from match progression data.
 */
@Singleton
final class GraphRenderer {
    static final int IMAGE_WIDTH = 532;
    static final int IMAGE_HEIGHT = 200;

    private static final int BACKGROUND_COLOR = 0xFFFFFF;

    private static final Color[] TEAM_COLORS = new Color[]{
            new Color(1f, 0.75f, 0f),
            new Color(0f, 0.5f, 1f),
            new Color(1f, 0f, 0.25f),
            new Color(0f, 1f, 0.75f),
            new Color(0.75f, 0f, 1f),
            new Color(0.75f, 1f, 0f)
    };

    byte[] renderPng(int[][] data) {
        BufferedImage img = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setColor(new Color(BACKGROUND_COLOR));
            g.fillRect(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);
            g.setColor(Color.BLACK);
            g.drawLine(0, IMAGE_HEIGHT - 1, IMAGE_WIDTH - 1, IMAGE_HEIGHT - 1);
            g.drawLine(0, 0, 0, IMAGE_HEIGHT - 1);

            if (data.length > 0) {
                int maxX = 0;
                int maxY = 0;
                for (int[] row : data) {
                    if (row[0] > maxX) {
                        maxX = row[0];
                    }
                    for (int j = 1; j < row.length; j++) {
                        if (row[j] > maxY) {
                            maxY = row[j];
                        }
                    }
                }
                if (maxX == 0) {
                    maxX = 1;
                }
                if (maxY == 0) {
                    maxY = 1;
                }

                for (int i = 0; i < data.length; i++) {
                    int a = i + 1;
                    int x = data[i][0] * IMAGE_WIDTH / maxX;
                    int y = IMAGE_HEIGHT - 1;
                    if ((a % 30) == 0) {
                        g.drawLine(x, y, x, y - 10);
                    } else if ((a % 15) == 0) {
                        g.drawLine(x, y, x, y - 7);
                    } else if ((a % 3) == 0) {
                        g.drawLine(x, y, x, y - 3);
                    }
                }

                for (int j = 1; j < data[0].length && j - 1 < TEAM_COLORS.length; j++) {
                    g.setColor(TEAM_COLORS[j - 1]);
                    int lastX = 0;
                    int lastY = IMAGE_HEIGHT;
                    for (int[] row : data) {
                        int x = row[0] * IMAGE_WIDTH / maxX;
                        int y = IMAGE_HEIGHT - row[j] * IMAGE_HEIGHT / maxY;
                        g.drawLine(lastX, lastY, x, y);
                        lastX = x;
                        lastY = y;
                    }
                }
            }
        } finally {
            g.dispose();
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ImageIO.write(img, "png", out);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to encode telemetry chart PNG", e);
        }
        return out.toByteArray();
    }
}
