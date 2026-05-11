package com.oddlabs.tt.audio;

import com.oddlabs.tt.render.Renderer;
import com.oddlabs.tt.resource.File;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * A resource handle for an audio file.
 */
public final class AudioFile extends File<Audio> {
    private final boolean streaming;

    public AudioFile(@NonNull String location) {
        this(location, !location.contains("/sfx/"));
    }

    public AudioFile(@NonNull String location, boolean streaming) {
        super(location);
        this.streaming = streaming;
    }

    @Override
    public @NonNull Audio get() throws UncheckedIOException {
        try {
            return Renderer.getRenderer().getAudioManager().createAudio(getURL());
        } catch (IOException ex) {
            throw new UncheckedIOException("Could not load " + this.getURL(), ex);
        }
    }

    @Override
    public boolean equals(@Nullable Object o) {
        return o instanceof AudioFile audioFile && super.equals(o) && audioFile.streaming == streaming;
    }

    public boolean isStreaming() {
        return streaming;
    }
}
