package com.oddlabs.tt.audio.openal;

import com.oddlabs.tt.audio.AudioManager;
import com.oddlabs.tt.render.Renderer;
import com.oddlabs.tt.resource.NativeResource;
import org.lwjgl.openal.ALC10;

import static com.oddlabs.tt.audio.openal.OpenALManager.checkALError;
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
public final class OpenALFilter extends NativeResource<OpenALFilter.FilterState> {

    static final class FilterState extends NativeResource.NativeState {
        final int filterId;

        FilterState() {
            filterId = alGenFilters();
            checkALError("alGenFilters");
            alFilteri(filterId, AL_FILTER_TYPE, AL_FILTER_LOWPASS);
            checkALError("alFilteri AL_FILTER_TYPE LOWPASS");
        }

        @Override
        public void close() {
            if (ALC10.alcGetCurrentContext() != 0) {
                alDeleteFilters(filterId);
                checkALError("alDeleteFilters");
            }
        }
    }

    public OpenALFilter() {
        super(new FilterState(), task -> Renderer.getRenderer().getAudioManager().enqueueCleanup(task));
    }

    public void setLowPassGain(float gain) {
        alFilterf(state.filterId, AL_LOWPASS_GAIN, gain);
        checkALError("alFilterf AL_LOWPASS_GAIN");
    }

    public void setLowPassGainHF(float gainHF) {
        alFilterf(state.filterId, AL_LOWPASS_GAINHF, gainHF);
        checkALError("alFilterf AL_LOWPASS_GAINHF");
    }

    public int getFilterId() {
        return state.filterId;
    }
}
