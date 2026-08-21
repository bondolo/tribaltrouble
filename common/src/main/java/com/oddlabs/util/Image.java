package com.oddlabs.util;


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

public final class Image implements Serializable {
    @Serial
    private static final long serialVersionUID = 1;

    private transient ByteBuffer data;

    private final int width;
    private final int height;

    public Image(int width, int height, ByteBuffer data) {
        assert width * height * Integer.BYTES == data.remaining() : "Image is incorrect size.";
        this.width = width;
        this.height = height;
        this.data = data;
    }

    public static Image read(URL url) {
        try (var source = url.openStream()) {
            return read(source);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static Image read(InputStream source) throws IOException {
        try (var input = new ObjectInputStream(new InflaterInputStream(new BufferedInputStream(source)))) {
            var object = input.readObject();
            if (object instanceof Image image) {
                return image;
            } else {
                throw new IOException("Null object returned from input stream.");
            }
        } catch (ClassNotFoundException e) {
            throw new IOException("Failed to read Image", e);
        }
    }

    public void write(String filename) {
        write(Path.of(filename + ".image"));
    }

    public void write(Path file) {
        data.rewind();
        //Utils.saveAsPNG(filename, data, width, height);
        try (var output = Files.newOutputStream(file)) {
            write(this, output);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void write(Image image, OutputStream os) throws IOException {
        try (var output = new ObjectOutputStream(new DeflaterOutputStream(new BufferedOutputStream(os)))) {
            output.writeObject(image);
        }
    }

    @Serial
    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.defaultWriteObject();

        data.rewind();
        ByteBuffer split = splitIntoPlanes();
        split.rewind();
        stream.write(split.array(), split.arrayOffset(), split.remaining());
    }

    @Serial
    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();

        int length = width * height * Integer.BYTES;

        ByteBuffer deltaPlanes = ByteBuffer.allocate(length);
        data = ByteBuffer.allocateDirect(length);
        byte[] buf = new byte[length];
        stream.readFully(buf);
        deltaPlanes.put(buf);
        mergePlanes(deltaPlanes);
    }

    private ByteBuffer splitIntoPlanes() {
        ByteBuffer buf = ByteBuffer.allocate(data.capacity());

        buf.position(buf.capacity() / 4);
        ByteBuffer buf1 = buf.slice();
        buf.position(2 * buf.capacity() / 4);
        ByteBuffer buf2 = buf.slice();
        buf.position(3 * buf.capacity() / 4);
        ByteBuffer buf3 = buf.slice();
        buf.position(0);
        IntBuffer data_ints = data.asIntBuffer();
        for (int y = 0; y < height; y++) {
            int o0 = 0, o1 = 0, o2 = 0, o3 = 0, n0 = 0, n1 = 0, n2 = 0, n3 = 0;
            for (int x = 0; x < width; x++) {
                int pixel = data_ints.get();
                /*
                				n0 = data.get();
                				n1 = data.get();
                				n2 = data.get();
                				n3 = data.get();
                */
                n0 = pixel >>> 24;
                n1 = (pixel >> 16) & 0xff;
                n2 = (pixel >> 8) & 0xff;
                n3 = pixel & 0xff;
                buf.put((byte) (n0 - o0));
                buf1.put((byte) (n1 - o1));
                buf2.put((byte) (n2 - o2));
                buf3.put((byte) (n3 - o3));
                o0 = n0;
                o1 = n1;
                o2 = n2;
                o3 = n3;
            }
        }
        data.rewind();

        return buf;
    }

    private void mergePlanes(ByteBuffer buf) {
        buf.flip();

        buf.position(buf.capacity() / Integer.BYTES);
        ByteBuffer buf0 = buf.slice();
        buf.position(2 * buf.capacity() / Integer.BYTES);
        ByteBuffer buf1 = buf.slice();
        buf.position(3 * buf.capacity() / Integer.BYTES);
        ByteBuffer buf2 = buf.slice();
        buf.position(0);
        IntBuffer data_ints = data.asIntBuffer();
        for (int y = 0; y < height; y++) {
            int o0 = 0, o1 = 0, o2 = 0, o3 = 0;
            for (int x = 0; x < width; x++) {
                o0 += buf.get();
                o1 += buf0.get();
                o2 += buf1.get();
                o3 += buf2.get();
                int pixel = (o0 & 0xff) << 24 | (o1 & 0xff) << 16 | (o2 & 0xff) << 8 | (o3 & 0xff);
                data_ints.put(pixel);
            }
        }
        data.clear();
    }

    public ByteBuffer getPixels() {
        return data;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    @Override
    public String toString() {
        return "Image: width = " + width + " | height = " + height;
    }
}
