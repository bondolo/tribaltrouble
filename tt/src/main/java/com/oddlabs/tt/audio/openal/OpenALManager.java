package com.oddlabs.tt.audio.openal;

import com.oddlabs.tt.render.Renderer;

import com.oddlabs.tt.audio.Audio;
import com.oddlabs.tt.audio.AudioManager;
import com.oddlabs.tt.audio.AudioParameters;
import com.oddlabs.tt.audio.AudioPlayer;
import com.oddlabs.tt.audio.AudioSource;
import com.oddlabs.tt.global.Settings;
import org.joml.Vector3fc;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static org.lwjgl.openal.ALC10.ALC_DEFAULT_DEVICE_SPECIFIER;
import static org.lwjgl.openal.ALC10.ALC_FALSE;
import static org.lwjgl.openal.ALC10.ALC_TRUE;
import static org.lwjgl.openal.ALC10.alcCloseDevice;
import static org.lwjgl.openal.ALC10.alcCreateContext;
import static org.lwjgl.openal.ALC10.alcDestroyContext;
import static org.lwjgl.openal.ALC10.alcGetString;
import static org.lwjgl.openal.ALC10.alcIsExtensionPresent;
import static org.lwjgl.openal.ALC10.alcMakeContextCurrent;
import static org.lwjgl.openal.ALC10.alcOpenDevice;
import static org.lwjgl.openal.SOFTHRTF.ALC_HRTF_SOFT;
import static org.lwjgl.openal.SOFTHRTF.alcResetDeviceSOFT;

/**
 * Audio Manager implementation using OpenAL
 */
public final class OpenALManager extends AudioManager {
    private static final boolean DEBUG = Boolean.getBoolean("com.oddlabs.tt.developer");
    private static final Logger logger = Logger.getLogger(OpenALManager.class.getName());
    private static final int MAX_NUM_SOURCES = 32;

    private final @NonNull ALData data;
    private final EFXManager efxManager = new EFXManager();

    private record ALData(long device, long context) implements AutoCloseable {
        @Override
        public void close() {
            alcDestroyContext(context);
            alcCloseDevice(device);
        }
    }

    public OpenALManager() {
        this(initAL());
    }

    private OpenALManager(@NonNull ALData data) {
        super(generateSources(MAX_NUM_SOURCES));
        this.data = data;
        this.efxManager.init(data.device);

        logger.info("OpenAL version: " + AL10.alGetString(AL10.AL_VERSION));
        logger.info("OpenAL vendor: " + AL10.alGetString(AL10.AL_VENDOR));
        logger.info("OpenAL renderer: " + AL10.alGetString(AL10.AL_RENDERER));
        AL10.alDistanceModel(AL11.AL_INVERSE_DISTANCE_CLAMPED);
        checkALError("alDistanceModel");
    }

    private static @NonNull ALData initAL() {
        String defaultDeviceName = alcGetString(0, ALC_DEFAULT_DEVICE_SPECIFIER);
        long device = alcOpenDevice(defaultDeviceName);
        if (device == 0) {
            throw new IllegalStateException("Failed to open default OpenAL device");
        }

        int[] attributes = {0};
        if (alcIsExtensionPresent(device, "ALC_SOFT_HRTF")) {
            attributes = new int[]{
                    ALC_HRTF_SOFT,
                    Renderer.getRenderer().getSettings().headphone_mode ? ALC_TRUE : ALC_FALSE,
                    0
            };
        }

        long context = alcCreateContext(device, attributes);
        if (context == 0) {
            alcCloseDevice(device);
            throw new IllegalStateException("Failed to create OpenAL context");
        }

        alcMakeContextCurrent(context);

        ALCCapabilities alcCapabilities = ALC.createCapabilities(device);
        AL.createCapabilities(alcCapabilities);

        return new ALData(device, context);
    }

    @Override
    public boolean isEFXSupported() {
        return efxManager.isSupported();
    }

    @Override
    public int getEFXEffectSlot() {
        return efxManager.getEffectSlot();
    }

    @Override
    public boolean isHRTFSupported() {
        return alcIsExtensionPresent(data.device, "ALC_SOFT_HRTF");
    }

    public void setHeadphoneMode(boolean enabled) {
        if (isHRTFSupported()) {
            int[] attrs = {ALC_HRTF_SOFT, enabled ? ALC_TRUE : ALC_FALSE, 0};
            if (!alcResetDeviceSOFT(data.device, attrs)) {
                logger.warning("Failed to reset device for HRTF change: " + errorToString(AL10.alGetError()));
            }
        } else {
            logger.warning("ALC_SOFT_HRTF not supported");
        }
    }

    private static @NonNull OpenALAudioSource @NonNull [] generateSources(int max) {
        return Stream.generate(() -> {
                    try {
                        return new OpenALAudioSource();
                    } catch (Exception _) {
                        // If source generation fails, stop trying to create more
                        return null;
                    }
                }).takeWhile(Objects::nonNull)
                .limit(max)
                .toArray(OpenALAudioSource[]::new);
    }

    @Override
    public @NonNull AudioManager setMasterGain(float gain) {
        super.setMasterGain(gain);
        AL10.alListenerf(AL10.AL_GAIN, gain);
        checkALError("alListenerf AL_GAIN");
        return this;
    }

    @Override
    public @NonNull AudioManager setListenerOrientation(@NonNull Vector3fc forward, @NonNull Vector3fc up) {
        super.setListenerOrientation(forward, up);
        try (var stack = MemoryStack.stackPush()) {
            var fb = stack.mallocFloat(6);
            fb.put(forward.x()).put(forward.y()).put(forward.z());
            fb.put(up.x()).put(up.y()).put(up.z());
            fb.flip();
            AL10.alListenerfv(AL10.AL_ORIENTATION, fb);
            checkALError("alListenerfv AL_ORIENTATION");
        }
        return this;
    }

    @Override
    public @NonNull AudioManager setListenerPosition(float x, float y, float z) {
        super.setListenerPosition(x, y, z);
        AL10.alListener3f(AL10.AL_POSITION, x, y, z);
        checkALError("alListener3f AL_POSITION");
        return this;
    }

    @Override
    protected @NonNull AudioPlayer createPlayer(@Nullable AudioSource source, float x, float y, float z, @NonNull AudioParameters<?> params) {
        return createPlayer((OpenALAudioSource) source, x, y, z, params);
    }

    @SuppressWarnings("unchecked")
    private @NonNull AudioPlayer createPlayer(@Nullable OpenALAudioSource source, float x, float y, float z, @NonNull AudioParameters<?> params)  {
        if (params.sound() instanceof Audio) {
            return new OpenALAudioPlayer(source, x, y, z, (AudioParameters<Audio>) params);
        } else if (params.sound() instanceof String) {
            return new OpenALQueuedAudioPlayer(source, x, y, z, (AudioParameters<String>) params);
        } else {
            throw new IllegalArgumentException("Unrecognized audio parameters : " + params.sound().getClass().getSimpleName());
        }
    }

    @Override
    public @NonNull Audio createAudio(@NonNull URL file) throws IOException {
        return new OpenALAudio(file);
    }

    @Override
    public void close() {
        try {
            efxManager.cleanup();
            super.close();
        } finally {
            data.close();
        }
    }

    public @NonNull EFXManager getEfxManager() {
        return efxManager;
    }

    /**
     * Checks for OpenAL errors and logs them
     *
     * @param message A descriptive message for the context of the OpenAL call.
     */
    public static void checkALError(@NonNull String message) {
        if (DEBUG) {
            int error = AL10.alGetError();
            if (error != AL10.AL_NO_ERROR) {
                logger.log(Level.WARNING, "OpenAL Error (" + message + "): " + errorToString(error), new Throwable("stacktrace"));
            }
        }
    }

    private static @NonNull String errorToString(int error) {
        return switch (error) {
            case AL10.AL_NO_ERROR -> "AL_NO_ERROR";
            case AL10.AL_INVALID_NAME -> "AL_INVALID_NAME";
            case AL10.AL_INVALID_ENUM -> "AL_INVALID_ENUM";
            case AL10.AL_INVALID_VALUE -> "AL_INVALID_VALUE";
            case AL10.AL_INVALID_OPERATION -> "AL_INVALID_OPERATION";
            case AL10.AL_OUT_OF_MEMORY -> "AL_OUT_OF_MEMORY";
            default -> "Unknown OpenAL Error: " + error;
        };
    }
}
