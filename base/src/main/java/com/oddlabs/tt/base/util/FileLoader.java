package com.oddlabs.tt.base.util;

import com.oddlabs.event.Deterministic;
import org.jspecify.annotations.NonNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.Objects;

public final class FileLoader implements FileLoaderInterface {
    private final FileLoaderListener listener;
    private final ReadableByteChannel file_channel;
    private final @NonNull ByteBuffer buffer;
    private final @NonNull Deterministic deterministic;

    public FileLoader(@NonNull File file, FileLoaderListener listener, int num_bytes,
            @NonNull Deterministic deterministic) {
        this.deterministic = Objects.requireNonNull(deterministic);
        this.buffer = ByteBuffer.allocate(num_bytes);
        this.listener = listener;
        IOException exception;
        ReadableByteChannel tmp_channel;
        try {
            tmp_channel = new FileInputStream(file).getChannel();
            exception = null;
        } catch (FileNotFoundException e) {
            tmp_channel = null;
            exception = e;
        }
        this.file_channel = tmp_channel;
        if (deterministic.log(exception != null))
            error(deterministic.log(exception));
        else
            newFile(file, deterministic.log(file.length()));
    }

    @Override
    public void newFile(File file, long length) {
        listener.newFile(file, length);
    }

    public void load() {
        if (deterministic.log(file_channel == null || !file_channel
                .isOpen()))
            return;
        buffer.clear();
        IOException exception;
        boolean eof;
        try {
            int num_bytes_read;
            do {
                num_bytes_read = file_channel.read(buffer);
            } while (num_bytes_read != -1 && buffer.hasRemaining());
            eof = num_bytes_read == -1;
            if (eof)
                file_channel.close();
            exception = null;
        } catch (IOException e) {
            exception = e;
            eof = true;
        }
        if (deterministic.log(exception != null))
            error(deterministic.log(exception));
        else
            data(deterministic.log(buffer.array()),
                    deterministic.log(buffer.position()),
                    deterministic.log(eof));
    }

    @Override
    public void data(byte[] data, int num_bytes_read, boolean eof) {
        listener.data(data, num_bytes_read, eof);
    }

    @Override
    public void error(IOException e) {
        close();
        listener.error(e);
    }

    public void close() {
        if (file_channel != null) {
            try {
                file_channel.close();
            } catch (IOException _) {
                //ignore
            }
        }
    }
}
