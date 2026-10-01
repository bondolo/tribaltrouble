package com.oddlabs.tt.audio.openal;

import com.oddlabs.tt.audio.Audio;
import com.oddlabs.tt.audio.AudioParameters;
import com.oddlabs.tt.audio.AudioPlayer;
import com.oddlabs.tt.audio.AudioSource;
import com.oddlabs.tt.base.resource.NativeResource;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;
import org.lwjgl.openal.EXTEfx;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.openal.EXTEfx.AL_AUXILIARY_SEND_FILTER;

/**
 * OpenAL implementation of {@link AudioSource} managing a native OpenAL source.
 */
final class OpenALAudioSource extends NativeResource<OpenALAudioSource.Source> implements AudioSource {

    static final class Source extends NativeResource.NativeState {

        final int sourceId;

        Source() {
            sourceId = AL10.alGenSources();
        }

        @Override
        public int hashCode() {
            return sourceId;
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            return obj instanceof Source source && sourceId == source.sourceId;
        }

        @Override
        public void close() {
            // Stop the source before deleting it, to be safe
            AL10.alSourceStop(sourceId);

            // Explicitly unqueue and detach any buffers (static or queued) from the source.
            detachBuffers(sourceId);

            // Reset any auxiliary sends to free up effect slots
            AL11.alSource3i(sourceId, AL_AUXILIARY_SEND_FILTER, 0, 0, 0);

            // Detach the direct filter to free up the filter object
            AL10.alSourcei(sourceId, EXTEfx.AL_DIRECT_FILTER, EXTEfx.AL_FILTER_NULL);

            AL10.alDeleteSources(sourceId);
        }
    }

    private final OpenALManager manager;
    private @Nullable AudioPlayer audio_player;
    private @Nullable OpenALFilter directFilter;
    private float rolloff;
    private float reference_distance;

    OpenALAudioSource(OpenALManager manager) {
        super(new Source(), manager::enqueueCleanup);
        this.manager = manager;
    }

    @Override
    public int hashCode() {
        return state.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return obj instanceof OpenALAudioSource source && state.equals(source.state);
    }

    @Override
    public void close() {
        try {
            stop();
            if (directFilter != null) {
                AL10.alSourcei(getSource(), EXTEfx.AL_DIRECT_FILTER, EXTEfx.AL_FILTER_NULL);
            }
            super.close();
        } finally {
            if (directFilter != null) {
                directFilter.close();
                directFilter = null;
            }
        }
    }

    @Override
    public float getRolloff() {
        return rolloff;
    }

    @Override
    public float getDistance() {
        return reference_distance;
    }

    @Override
    public void setDirectFilterGainHF(float gainHF) {
        try {
            if (directFilter == null) {
                directFilter = new OpenALFilter(manager::enqueueCleanup);
            }
            directFilter.setLowPassGainHF(gainHF);
            int sourceId = getSource();
            AL10.alSourcei(sourceId, EXTEfx.AL_DIRECT_FILTER, directFilter.getFilterId());
        } catch (Exception e) {
            // EFX not supported or an error creating the filter, ignore
        }
    }

    @Override
    public State getState() {
        return switch (getSourceState()) {
            case AL10.AL_INITIAL -> State.INITIAL;
            case AL10.AL_PLAYING -> State.PLAYING;
            case AL10.AL_PAUSED -> State.PAUSED;
            case AL10.AL_STOPPED -> State.STOPPED;
            default -> throw new IllegalStateException("Unknown state");
        };
    }

    @Override
    public void setAudio(Audio audio) {
        if (audio instanceof OpenALAudio alAudio) {
            setAudio(alAudio);
        } else {
            throw new IllegalArgumentException("Unsupported audio type: " + audio.getClass().getName());
        }
    }

    void setAudio(OpenALAudio audio) {
        int buffer = audio.getBuffer();
        assert buffer != AL10.AL_NONE;
        int sourceId = getSource();
        if (AL10.alGetSourcei(sourceId, AL11.AL_SOURCE_TYPE) == AL11.AL_STREAMING) {
            detachBuffers(sourceId);
        }
        AL10.alSourcei(sourceId, AL10.AL_BUFFER, audio.getBuffer());
    }

    void queue(IntBuffer al_buffers) {
        int sourceId = getSource();
        assert al_buffers.remaining() > 0 : "al_buffers is empty";
        if (AL10.alGetSourcei(sourceId, AL11.AL_SOURCE_TYPE) != AL11.AL_STREAMING) {
            AL10.alSourceStop(sourceId);
            AL10.alSourcei(sourceId, AL10.AL_BUFFER, AL10.AL_NONE);
        }
        AL10.alSourceQueueBuffers(sourceId, al_buffers);
    }

    int processed() {
        return AL10.alGetSourcei(getSource(), AL10.AL_BUFFERS_PROCESSED);
    }

    void unqueued(IntBuffer al_buffers) {
        AL10.alSourceUnqueueBuffers(getSource(), al_buffers);
    }

    @Override
    public void setPitch(float pitch) {
        AL10.alSourcef(getSource(), AL10.AL_PITCH, pitch);
    }

    @Override
    public void setGain(float gain) {
        AL10.alSourcef(getSource(), AL10.AL_GAIN, gain);
    }

    @Override
    public void setMinGain(float gain) {
        AL10.alSourcef(getSource(), AL10.AL_MIN_GAIN, gain);
    }

    @Override
    public void setMaxGain(float gain) {
        AL10.alSourcef(getSource(), AL10.AL_MAX_GAIN, gain);
    }

    @Override
    public void setRolloff(float rolloff) {
        this.rolloff = rolloff;
        AL10.alSourcef(getSource(), AL10.AL_ROLLOFF_FACTOR, rolloff);
    }

    @Override
    public void setDistance(float distance) {
        this.reference_distance = distance;
        AL10.alSourcef(getSource(), AL10.AL_REFERENCE_DISTANCE, distance);
    }

    @Override
    public void setPosition(float x, float y, float z) {
        AL10.alSource3f(getSource(), AL10.AL_POSITION, x, y, z);
    }

    @Override
    public void setRelative(boolean relative) {
        AL10.alSourcei(getSource(), AL10.AL_SOURCE_RELATIVE, relative ? AL10.AL_TRUE : AL10.AL_FALSE);
    }

    @Override
    public void setLooping(boolean looping) {
        AL10.alSourcei(getSource(), AL10.AL_LOOPING, looping ? AL10.AL_TRUE : AL10.AL_FALSE);
    }

    @Override
    public void stop() {
        int sourceId = getSource();
        if (AL10.alGetSourcei(sourceId, AL10.AL_SOURCE_STATE) == AL10.AL_PLAYING) {
            AL10.alSourcef(sourceId, AL10.AL_GAIN, 0f);
        }
        AL10.alSourcei(sourceId, AL10.AL_LOOPING, AL10.AL_FALSE);
        AL10.alSourceStop(sourceId);
        AL10.alSourceRewind(sourceId);
    }

    static void detachBuffers(int sourceId) {
        AL10.alSourceStop(sourceId);
        int processed = AL10.alGetSourcei(sourceId, AL10.AL_BUFFERS_PROCESSED);
        if (processed > 0) {
            try (var stack = MemoryStack.stackPush()) {
                IntBuffer unqueueBuf = stack.mallocInt(processed);
                AL10.alSourceUnqueueBuffers(sourceId, unqueueBuf);
            }
        }
        AL10.alSourcei(sourceId, AL10.AL_BUFFER, AL10.AL_NONE);
    }

    @Override
    public void pause() {
        AL10.alSourcePause(getSource());
    }

    @Override
    public void play() {
        // Only play if not already playing to avoid OpenAL source stealing/restarting
        if (getState() != State.PLAYING) {
            AL10.alSourcePlay(getSource());
        }
    }

    @Override
    public void setBuffer(int bufferId) {
        AL10.alSourcei(getSource(), AL10.AL_BUFFER, bufferId);
    }

    @Override
    public void rewind() {
        AL10.alSourceRewind(getSource());
    }

    int getSourceState() {
        return AL10.alGetSourcei(getSource(), AL10.AL_SOURCE_STATE);
    }

    @Override
    public Vector3f getPosition() {
        try (var stack = MemoryStack.stackPush()) {
            FloatBuffer positionBuffer = stack.mallocFloat(3);
            AL10.alGetSourcefv(getSource(), AL10.AL_POSITION, positionBuffer);
            return new Vector3f(positionBuffer);
        }
    }

    int getSource() {
        return state.sourceId;
    }

    @Override
    public int getRank() {
        return audio_player != null ? audio_player.getParameters().rank() : AudioParameters.RANK_NOT_INITIALIZED;
    }

    @Override
    public @Nullable AudioPlayer getAudioPlayer() {
        return audio_player;
    }

    @Override
    public void setAudioPlayer(@Nullable AudioPlayer audio_player) {
        if (this.audio_player != null && this.audio_player != audio_player && this.audio_player.isPlaying())
            this.audio_player.stop();
        this.audio_player = audio_player;
    }

    @Override
    public void setAuxiliarySend(int slotId, int filterId) {
        AL11.alSource3i(getSource(), AL_AUXILIARY_SEND_FILTER, slotId, 0, filterId);
    }
}
