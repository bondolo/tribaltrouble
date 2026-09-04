package com.oddlabs.util;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

/**
 * Writes raw pixel data to PNG format.
 */
public final class PNGWriter {
    private static final byte[] PNG_SIGNATURE = new byte[]{
            (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A
    };
    private static final byte[] CHUNK_IHDR = new byte[]{'I', 'H', 'D', 'R'};
    private static final byte[] CHUNK_IDAT = new byte[]{'I', 'D', 'A', 'T'};
    private static final byte[] CHUNK_IEND = new byte[]{'I', 'E', 'N', 'D'};

    private PNGWriter() {
        // no instances
    }

    public static void write(Path file, int width, int height, byte[] rgbaPixels) throws IOException {
        try (OutputStream os = Files.newOutputStream(file)) {
            write(os, width, height, rgbaPixels);
        }
    }

    public static void write(OutputStream out, int width, int height, byte[] rgbaPixels) throws IOException {
        DataOutputStream dos = new DataOutputStream(out);
        dos.write(PNG_SIGNATURE);

        // IHDR chunk: 13 bytes
        byte[] ihdrData = new byte[13];
        ihdrData[0] = (byte) ((width >>> 24) & 0xFF);
        ihdrData[1] = (byte) ((width >>> 16) & 0xFF);
        ihdrData[2] = (byte) ((width >>> 8) & 0xFF);
        ihdrData[3] = (byte) (width & 0xFF);
        ihdrData[4] = (byte) ((height >>> 24) & 0xFF);
        ihdrData[5] = (byte) ((height >>> 16) & 0xFF);
        ihdrData[6] = (byte) ((height >>> 8) & 0xFF);
        ihdrData[7] = (byte) (height & 0xFF);
        ihdrData[8] = 8; // 8 bits per channel
        ihdrData[9] = 6; // Color type: 6 = RGBA
        ihdrData[10] = 0; // Compression method: 0 (deflate)
        ihdrData[11] = 0; // Filter method: 0 (standard)
        ihdrData[12] = 0; // Interlace method: 0 (no interlace)
        writeChunk(dos, CHUNK_IHDR, ihdrData);

        // IDAT chunk: compressed scanlines
        // Each scanline starts with filter byte 0 (None), followed by width * 4 RGBA bytes
        ByteArrayOutputStream compressedStream = new ByteArrayOutputStream();
        Deflater deflater = new Deflater(Deflater.DEFAULT_COMPRESSION);
        try (DeflaterOutputStream deflaterStream = new DeflaterOutputStream(compressedStream, deflater)) {
            int rowBytes = width * 4;
            for (int y = 0; y < height; y++) {
                deflaterStream.write(0); // Filter type: None
                deflaterStream.write(rgbaPixels, y * rowBytes, rowBytes);
            }
            deflaterStream.finish();
        } finally {
            deflater.end();
        }
        writeChunk(dos, CHUNK_IDAT, compressedStream.toByteArray());

        // IEND chunk: empty data
        writeChunk(dos, CHUNK_IEND, new byte[0]);
        dos.flush();
    }

    private static void writeChunk(DataOutputStream out, byte[] type, byte[] data) throws IOException {
        out.writeInt(data.length);
        out.write(type);
        out.write(data);

        CRC32 crc = new CRC32();
        crc.update(type);
        crc.update(data);
        out.writeInt((int) crc.getValue());
    }
}
