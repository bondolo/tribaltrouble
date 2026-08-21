package com.oddlabs.tt.audio;

import com.oddlabs.util.Utils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.ref.SoftReference;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

/**
 * A resource handle for an audio file.
 */
public final class AudioFile {
    private final @NonNull URI uri;
    private final boolean streaming;

    private @Nullable SoftReference<Audio> audio;

    public AudioFile(@NonNull URI uri) {
        this(uri, uri.toString().contains("/music/"));
    }

    public AudioFile(@NonNull URI uri, boolean streaming) {
        this.uri = uri;
        this.streaming = streaming;
    }

    public AudioFile(@NonNull String location) {
        this(location, location.contains("/music/"));
    }

    public AudioFile(@NonNull String location, boolean streaming) {
        this(Utils.makeURI(location), streaming);
    }

    public @NonNull URL getURL() {
        try {
            return uri.toURL();
        } catch (MalformedURLException e) {
            throw new UncheckedIOException(new IOException("bad location: " + uri, e));
        }
    }

    public synchronized @NonNull Audio get(@NonNull AudioManager manager) throws UncheckedIOException {
        Audio audio = null == this.audio ? null : this.audio.get();
        if (null == audio) {
            try {
                audio = manager.createAudio(getURL());
                this.audio = new SoftReference<>(audio);
            } catch (IOException ex) {
                throw new UncheckedIOException("Could not load " + this.getURL(), ex);
            }
        }

        return audio;
    }

    @Override
    public int hashCode() {
        return uri.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        return o instanceof AudioFile audioFile && uri.equals(audioFile.uri) && audioFile.streaming == streaming;
    }

    @Override
    public @NonNull String toString() {
        return "AudioFile{uri=" + uri.toASCIIString() + ", streaming=" + streaming + '}';
    }

    public boolean isStreaming() {
        return streaming;
    }
}
