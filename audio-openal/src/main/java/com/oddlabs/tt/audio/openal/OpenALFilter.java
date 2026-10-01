package com.oddlabs.tt.audio.openal;

import com.oddlabs.tt.base.resource.NativeResource;
import org.lwjgl.openal.ALC10;

import java.util.function.Consumer;

import static org.lwjgl.openal.EXTEfx.AL_FILTER_LOWPASS;
import static org.lwjgl.openal.EXTEfx.AL_FILTER_TYPE;
import static org.lwjgl.openal.EXTEfx.AL_LOWPASS_GAIN;
import static org.lwjgl.openal.EXTEfx.AL_LOWPASS_GAINHF;
import static org.lwjgl.openal.EXTEfx.alDeleteFilters;
import static org.lwjgl.openal.EXTEfx.alFilterf;
import static org.lwjgl.openal.EXTEfx.alFilteri;
import static org.lwjgl.openal.EXTEfx.alGenFilters;

/**
 * Manages a native OpenAL filter for environmental audio effects.
 */
final class OpenALFilter extends NativeResource<OpenALFilter.FilterState> {

    static final class FilterState extends NativeResource.NativeState {
        final int filterId;

        FilterState() {
            filterId = alGenFilters();
            alFilteri(filterId, AL_FILTER_TYPE, AL_FILTER_LOWPASS);
        }

        @Override
        public void close() {
            if (ALC10.alcGetCurrentContext() != 0) {
                alDeleteFilters(filterId);
            }
        }
    }

    OpenALFilter(Consumer<Runnable> cleanupStrategy) {
        super(new FilterState(), cleanupStrategy);
    }

    void setLowPassGain(float gain) {
        alFilterf(state.filterId, AL_LOWPASS_GAIN, gain);
    }

    void setLowPassGainHF(float gainHF) {
        alFilterf(state.filterId, AL_LOWPASS_GAINHF, gainHF);
    }

    int getFilterId() {
        return state.filterId;
    }
}
