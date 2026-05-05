package com.oddlabs.tt.audio.openal;

import com.oddlabs.tt.audio.Audio;
import com.oddlabs.tt.audio.AudioManager;
import com.oddlabs.tt.audio.AudioParameters;
import com.oddlabs.tt.audio.AudioPlayer;
import org.joml.Vector3fc;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * OpenAL implementation of {@link AudioPlayer} for buffered audio.
 */
final class OpenALAudioPlayer extends AudioPlayer {

    public OpenALAudioPlayer(@Nullable OpenALAudioSource source, @NonNull AudioParameters<Audio> params) {
        super(source, params);
        if (this.source == null) {
            return;
        }

        if (AudioManager.getManager() instanceof OpenALManager alManager) {
            EFXManager efx = alManager.getEfxManager();
            if (efx.isSupported()) {
                boolean useReverb = params.rank != AUDIO_RANK_MUSIC && params.rank != AUDIO_RANK_NOTIFICATION;
                ((OpenALAudioSource) source).setAuxiliarySend(useReverb ? efx.getEffectSlot() : 0, 0);

                if (useReverb) {
                    Vector3fc listener = AudioManager.getManager().getListenerPosition();
                    float dx = params.x - listener.x();
                    float dy = params.y - listener.y();
                    float dz = params.z - listener.z();
                    float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

                    // Simple air absorption: brighter up close, muffled far away
                    // Clamp to [0.1, 1.0] to avoid total silence in HF
                    float maxDist = params.distance != Float.MAX_VALUE ? params.distance : 1000f;
                    float gainHF = Math.clamp(1.0f - (dist / maxDist), 0.1f, 1.0f);

                    ((OpenALAudioSource) source).setDirectFilterGainHF(gainHF);
                } else {
                    ((OpenALAudioSource) source).setDirectFilterGainHF(1.0f); // Reset to full brightness
                }
            }
        }

        if (params.music || AudioManager.getManager().startPlaying()) {
            source.play();
        }
    }
}
