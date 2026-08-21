package com.oddlabs.tt.audio;


/**
 * Interface for audio backend implementations to create audio players.
 */
@FunctionalInterface
public interface AudioImplementation {
    AudioPlayer newAudio(float x, float y, float z, AudioParameters params);
}
