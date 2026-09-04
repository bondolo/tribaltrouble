package com.oddlabs.util;

import com.oddlabs.procedural.Layer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.zip.CRC32;
import java.util.zip.InflaterInputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link PNGWriter}.
 */
final class PNGWriterTest {
    private static final byte[] EXPECTED_SIGNATURE = new byte[]{
            (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A
    };

    @Test
    void writesValidPngStructure() throws IOException {
        int width = 2;
        int height = 2;
        byte[] rgba = new byte[]{
                (byte) 255, 0, 0, (byte) 255,        // Red
                0, (byte) 255, 0, (byte) 255,        // Green
                0, 0, (byte) 255, (byte) 255,        // Blue
                (byte) 255, (byte) 255, 0, (byte) 255 // Yellow
        };

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PNGWriter.write(baos, width, height, rgba);

        byte[] pngBytes = baos.toByteArray();
        assertTrue(pngBytes.length > 8);

        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(pngBytes));

        // 1. Signature
        byte[] signature = new byte[8];
        dis.readFully(signature);
        assertArrayEquals(EXPECTED_SIGNATURE, signature);

        // 2. IHDR Chunk
        int ihdrLen = dis.readInt();
        assertEquals(13, ihdrLen);
        byte[] ihdrType = new byte[4];
        dis.readFully(ihdrType);
        assertArrayEquals(new byte[]{'I', 'H', 'D', 'R'}, ihdrType);

        byte[] ihdrData = new byte[13];
        dis.readFully(ihdrData);
        ByteBuffer ihdrBuf = ByteBuffer.wrap(ihdrData);
        assertEquals(width, ihdrBuf.getInt());
        assertEquals(height, ihdrBuf.getInt());
        assertEquals(8, ihdrBuf.get());  // bit depth
        assertEquals(6, ihdrBuf.get());  // color type RGBA
        assertEquals(0, ihdrBuf.get());  // compression
        assertEquals(0, ihdrBuf.get());  // filter
        assertEquals(0, ihdrBuf.get());  // interlace

        int ihdrCrc = dis.readInt();
        CRC32 crc = new CRC32();
        crc.update(ihdrType);
        crc.update(ihdrData);
        assertEquals((int) crc.getValue(), ihdrCrc);

        // 3. IDAT Chunk
        int idatLen = dis.readInt();
        byte[] idatType = new byte[4];
        dis.readFully(idatType);
        assertArrayEquals(new byte[]{'I', 'D', 'A', 'T'}, idatType);

        byte[] idatData = new byte[idatLen];
        dis.readFully(idatData);
        int idatCrc = dis.readInt();
        crc.reset();
        crc.update(idatType);
        crc.update(idatData);
        assertEquals((int) crc.getValue(), idatCrc);

        // Decompress IDAT and verify scanlines
        InflaterInputStream inflater = new InflaterInputStream(new ByteArrayInputStream(idatData));
        byte[] rawScanlines = inflater.readAllBytes();
        int expectedRawLen = height * (1 + width * 4); // 2 rows * (1 filter byte + 8 bytes RGBA)
        assertEquals(expectedRawLen, rawScanlines.length);

        // Verify row 0
        assertEquals(0, rawScanlines[0]); // filter byte
        assertArrayEquals(Arrays.copyOfRange(rgba, 0, 8), Arrays.copyOfRange(rawScanlines, 1, 9));

        // Verify row 1
        assertEquals(0, rawScanlines[9]); // filter byte
        assertArrayEquals(Arrays.copyOfRange(rgba, 8, 16), Arrays.copyOfRange(rawScanlines, 10, 18));

        // 4. IEND Chunk
        int iendLen = dis.readInt();
        assertEquals(0, iendLen);
        byte[] iendType = new byte[4];
        dis.readFully(iendType);
        assertArrayEquals(new byte[]{'I', 'E', 'N', 'D'}, iendType);
        int iendCrc = dis.readInt();
        crc.reset();
        crc.update(iendType);
        assertEquals((int) crc.getValue(), iendCrc);

        // 5. Verify ImageIO can read the PNG and verify pixel colors
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(pngBytes));
        assertEquals(width, image.getWidth());
        assertEquals(height, image.getHeight());
        assertEquals(0xFFFF0000, image.getRGB(0, 0)); // Red
        assertEquals(0xFF00FF00, image.getRGB(1, 0)); // Green
        assertEquals(0xFF0000FF, image.getRGB(0, 1)); // Blue
        assertEquals(0xFFFFFF00, image.getRGB(1, 1)); // Yellow
    }

    @Test
    void layerSaveAsPngProducesValidFile(@TempDir Path tempDir) throws IOException {
        Layer layer = new Layer(4, 4);
        Path outputFile = tempDir.resolve("test_layer.png");
        layer.saveAsPNG(outputFile);

        assertTrue(Files.exists(outputFile));
        byte[] bytes = Files.readAllBytes(outputFile);
        assertTrue(bytes.length > 8);

        byte[] sig = Arrays.copyOf(bytes, 8);
        assertArrayEquals(EXPECTED_SIGNATURE, sig);

        BufferedImage image = ImageIO.read(outputFile.toFile());
        assertEquals(4, image.getWidth());
        assertEquals(4, image.getHeight());
    }
}
