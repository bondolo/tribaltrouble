package com.oddlabs.tt.viewer;

import com.oddlabs.tt.audio.Assets;
import com.oddlabs.tt.audio.AudioImplementation;
import com.oddlabs.tt.audio.AudioManager;
import com.oddlabs.tt.audio.AudioParameters;
import com.oddlabs.tt.audio.AudioPlayer;
import com.oddlabs.tt.audio.openal.EFXManager;
import com.oddlabs.tt.audio.openal.OpenALManager;
import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.camera.GameCamera;
import com.oddlabs.tt.landscape.AbstractTreeGroup;
import com.oddlabs.tt.landscape.HeightMap;
import com.oddlabs.tt.landscape.TreeGroup;
import com.oddlabs.tt.landscape.TreeLeaf;
import com.oddlabs.tt.landscape.TreeSupply;
import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.render.Renderer;
import org.joml.Vector3f;
import org.jspecify.annotations.NonNull;

import java.util.logging.Logger;

/**
 * Coordinates the playback of ambient environmental sounds, providing
 * realistic reverb and attenuation based on camera proximity and terrain features.
 */
public final class AmbientAudio {
    private static final Logger logger = Logger.getLogger(AmbientAudio.class.getSimpleName());
    /** How many trees make a forest */
    private static final int TREES_FOREST_THRESHOLD = 10;
    private static final float CANYON_PROXIMITY_DISTANCE = 30f;
    private static final AudioParameters AMBIENT_FOREST = new AudioParameters(
            Assets.SFX_AMBIENT_FOREST, Assets.AUDIO_RANK_AMBIENT,
            Assets.AUDIO_DISTANCE_AMBIENT, Assets.AUDIO_GAIN_AMBIENT_FOREST, Assets.AUDIO_RADIUS_AMBIENT_FOREST,
            1f, true, true);
    private static final AudioParameters AMBIENT_BEACH = new AudioParameters(
            Assets.SFX_AMBIENT_BEACH, Assets.AUDIO_RANK_AMBIENT,
            Assets.AUDIO_DISTANCE_AMBIENT, Assets.AUDIO_GAIN_AMBIENT_BEACH, Assets.AUDIO_RADIUS_AMBIENT_BEACH,
            1f, true, true);
    private static final AudioParameters AMBIENT_WIND = new AudioParameters(
            Assets.SFX_AMBIENT_WIND, Assets.AUDIO_RANK_AMBIENT,
            Assets.AUDIO_DISTANCE_AMBIENT, Assets.AUDIO_GAIN_AMBIENT_WIND, Assets.AUDIO_RADIUS_AMBIENT_WIND,
            1f, true, true);

    private final @NonNull AudioPlayer ambient_forest;
    private final @NonNull AudioPlayer ambient_beach;
    private final @NonNull AudioPlayer ambient_wind;

    private final Vector3f f = new Vector3f();
    private final Vector3f s = new Vector3f();
    private final Vector3f u = new Vector3f();

    public AmbientAudio(@NonNull AudioImplementation audio_implementation) {
        ambient_forest = audio_implementation.newAudio(10000f, 10000f, 10000f, AMBIENT_FOREST).registerAmbient();
        ambient_beach = audio_implementation.newAudio(10000f, 10000f, 10000f, AMBIENT_BEACH).registerAmbient();
        ambient_wind = audio_implementation.newAudio(10000f, 10000f, 10000f, AMBIENT_WIND).registerAmbient();
    }

    private static int countTrees(@NonNull AbstractTreeGroup node, float x, float y, float radiusSq, int threshold,
            int currentCount) {
        if (currentCount >= threshold) return currentCount;

        if (intersects(node, x, y, radiusSq)) {
            switch (node) {
                case TreeGroup group -> {
                    for (AbstractTreeGroup child : group.children()) {
                        currentCount = countTrees(child, x, y, radiusSq, threshold, currentCount);
                        if (currentCount >= threshold) break;
                    }
                }
                case TreeLeaf leaf -> {
                    for (TreeSupply tree : leaf.getTrees()) {
                        if (currentCount >= threshold) break;
                        if (!tree.isHidden()) {
                            float ddx = tree.getCX() - x;
                            float ddy = tree.getCY() - y;
                            if (ddx * ddx + ddy * ddy < radiusSq) {
                                currentCount++;
                            }
                        }
                    }
                }
                case TreeSupply tree -> {
                    if (!tree.isHidden()) {
                        float ddx = tree.getCX() - x;
                        float ddy = tree.getCY() - y;
                        if (ddx * ddx + ddy * ddy < radiusSq) {
                            currentCount++;
                        }
                    }
                }
            }
        }
        return currentCount;
    }

    private static boolean intersects(com.oddlabs.tt.util.@NonNull BoundingBox box, float x, float y, float radiusSq) {
        float dx = x - Math.max(box.bmin_x, Math.min(x, box.bmax_x));
        float dy = y - Math.max(box.bmin_y, Math.min(y, box.bmax_y));
        return (dx * dx + dy * dy) < radiusSq;
    }

    public void stop() {
        ambient_forest.stop().removeAmbient();
        ambient_beach.stop().removeAmbient();
        ambient_wind.stop().removeAmbient();
    }

    public void updateSoundListener(@NonNull CameraState camera, @NonNull HeightMap heightmap) {
        AudioManager audioManager = Renderer.getRenderer().getAudioManager();
        if (!audioManager.isSfxEnabled()) {
            return;
        }

        camera.updateDirectionAndNormal(f, u, s);
        audioManager
                .setListenerPosition(camera.getCurrentX(), camera.getCurrentY(), camera.getCurrentZ())
                .setListenerOrientation(f, u);

        int meters_per_world = heightmap.getMetersPerWorld();
        float dx = Math.abs(camera.getCurrentX() - meters_per_world / 2f);
        float dy = Math.abs(camera.getCurrentY() - meters_per_world / 2f);
        float dr = 2f * (float) Math.sqrt(dx * dx + dy * dy) / meters_per_world;

        // update placement and gain of ambient forest source
        ambient_forest.setPosition(0f, 0f, heightmap.getNearestHeight(camera.getCurrentX(), camera.getCurrentY())
                - camera.getCurrentZ() + 8f);
        ambient_forest.setGain(Assets.AUDIO_GAIN_AMBIENT_FOREST * Math.clamp(1f - dr + 0.5f, 0f, 1f));

        // update placement and gain of ambient beach source
        float factor = 1f;
        if (dr != 0)
            factor = 1f / dr - 1f;
        float beach_x = (camera.getCurrentX() - meters_per_world / 2f) * factor;
        float beach_y = (camera.getCurrentY() - meters_per_world / 2f) * factor;
        float beach_z = heightmap.getNearestHeight(camera.getCurrentX(), camera.getCurrentY()) - camera.getCurrentZ();
        float beach_gain = Assets.AUDIO_GAIN_AMBIENT_BEACH * Math.clamp(1f - Math.abs(4f * dr - 3.75f), 0f, 1f);
        ambient_beach.setPosition(beach_x, beach_y, beach_z);
        ambient_beach.setGain(beach_gain);

        // update placement of ambient wind source
        ambient_wind.setPosition(0f, 0f, Math.max(0f, 50f + GameCamera.MAX_Z - camera.getCurrentZ()));
        ambient_wind.setGain(Assets.AUDIO_GAIN_AMBIENT_WIND);

        if (Renderer.getRenderer().getAudioManager() instanceof OpenALManager alManager) {
            EFXManager efx = alManager.getEfxManager();
            if (efx.isSupported()) {
                float camZ = camera.getCurrentZ();
                float camX = camera.getCurrentX();
                float camY = camera.getCurrentY();
                float hCurrent = heightmap.getNearestHeight(camX, camY);

                if (camZ < heightmap.getSeaLevelMeters()) {
                    efx.setReverb(EFXManager.ReverbType.UNDERWATER);
                } else {
                    float heightAboveGround = camZ - hCurrent;

                    // Check for forest density
                    World world = heightmap.getWorld();
                    int treeCount = countTrees(world.getTreeRoot(), camX, camY, 25f * 25f, TREES_FOREST_THRESHOLD, 0);

                    if (treeCount >= TREES_FOREST_THRESHOLD) {
                        // Forest reverb: Blend from FOREST (fully active at 15m) to NONE (fully silent at 30m)
                        float blend = Math.clamp((30.0f - heightAboveGround) / 15.0f, 0f, 1f);
                        efx.setReverb(EFXManager.ReverbType.NONE, EFXManager.ReverbType.FOREST, blend);
                    } else {
                        // Check for valley/enclosure by sampling terrain height around camera
                        float hN = heightmap.getNearestHeight(camX, camY + CANYON_PROXIMITY_DISTANCE);
                        float hS = heightmap.getNearestHeight(camX, camY - CANYON_PROXIMITY_DISTANCE);
                        float hE = heightmap.getNearestHeight(camX + CANYON_PROXIMITY_DISTANCE, camY);
                        float hW = heightmap.getNearestHeight(camX - CANYON_PROXIMITY_DISTANCE, camY);

                        float avgSurround = (hN + hS + hE + hW) * 0.25f;
                        float valleyDepth = avgSurround - hCurrent;

                        if (valleyDepth > 8.0f) {
                            // Valley reverb: Blend from VALLEY (fully active at 8m) to NONE (fully silent at 16m)
                            float blend = Math.clamp((16.0f - heightAboveGround) / 8.0f, 0f, 1f);
                            efx.setReverb(EFXManager.ReverbType.NONE, EFXManager.ReverbType.VALLEY, blend);
                        } else {
                            // Open Plains (Generic) reverb: Blend from GENERIC (fully active at 10m) to NONE (fully silent at 20m)
                            float blend = Math.clamp((20.0f - heightAboveGround) / 10.0f, 0f, 1f);
                            efx.setReverb(EFXManager.ReverbType.NONE, EFXManager.ReverbType.GENERIC, blend);
                        }
                    }
                }
            }
        }
    }
}
