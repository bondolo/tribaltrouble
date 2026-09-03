package com.oddlabs.tt.engine.render.state;

import com.oddlabs.tt.engine.render.CameraState;
import com.oddlabs.tt.engine.render.scenery.Water;
import com.oddlabs.tt.engine.render.shader.FogShader;
import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;

/**
 * Helper class to pack global uniform data into a ByteBuffer according to std140 layout.
 */
public final class GlobalUniforms {

    public void update(CameraState camera, float time, float seaLevel, @Nullable Water water,
            ByteBuffer buffer) {
        buffer.clear();

        // 0: mat4 projection (64)
        camera.getProjectionMatrix().get(0, buffer);

        // 64: mat4 view (64)
        camera.getModelView().get(64, buffer);

        // 128: vec4 fogColor (16)
        buffer.position(128);
        FogInfo fog = camera.getFog();
        var linearColor = fog.getColor();
        buffer.putFloat(linearColor.r());
        buffer.putFloat(linearColor.g());
        buffer.putFloat(linearColor.b());
        buffer.putFloat(linearColor.a());

        // 144: vec4 fogParams (16)
        // 160: float cameraHeight (4) -- Must be 16-aligned after vec3 in std140
        // 164: float fogHeightFactor (4)
        // 168: float globalTime (4)
        // 172: int fogMode (4)

        int mode = -1;
        float hf = 0f;
        float ch = camera.getCurrentZ();
        float p1 = 0, p2 = 0, p3 = 0;

        if (fog.isEnabled()) {
            if (fog instanceof DistanceFogInfo df) {
                mode = switch (df.getMode()) {
                    case EXP -> FogShader.FOG_MODE_EXP;
                    case EXP2 -> FogShader.FOG_MODE_EXP2;
                    default -> FogShader.FOG_MODE_LINEAR;
                };
                p1 = df.getDensity();
                p2 = df.getStart();
                p3 = df.getEnd();
                hf = df.getHeightFactor();
            } else if (fog instanceof RadialFogInfo rf) {
                mode = FogShader.FOG_MODE_RADIAL;
                p1 = (float) camera.getWidth();
                p2 = (float) camera.getHeight();
                p3 = rf.getDensity();
                hf = rf.getRadiusScale();
            }
        }

        buffer.putFloat(p1);
        buffer.putFloat(p2);
        buffer.putFloat(p3);
        buffer.putFloat(seaLevel);

        buffer.putFloat(ch);
        buffer.putFloat(hf);
        buffer.putFloat(time);
        buffer.putInt(mode);

        buffer.position(176); // Ensure position before lighting params

        // 176: vec4 u_lightDirection (16)
        buffer.putFloat(-0.70710677f);
        buffer.putFloat(0.0f);
        buffer.putFloat(0.70710677f);
        buffer.putFloat(1.0f);

        // 192: vec4 u_globalAmbient (16) - Linearized (0.4, 0.4, 0.45)
        buffer.putFloat(0.132866f);
        buffer.putFloat(0.132866f);
        buffer.putFloat(0.170656f);
        buffer.putFloat(1.0f);

        // 208: vec4 u_groundAmbient (16) - Linearized (0.15, 0.12, 0.1)
        buffer.putFloat(0.019472f);
        buffer.putFloat(0.012726f);
        buffer.putFloat(0.008518f);
        buffer.putFloat(1.0f);

        // 224: vec4 u_sunColor (16)
        buffer.putFloat(1.0f);
        buffer.putFloat(1.0f);
        buffer.putFloat(1.0f);
        buffer.putFloat(1.0f);

        buffer.position(240); // Ensure position before water params

        if (water != null) {
            water.putGlobalUniforms(buffer, !camera.inNoDetailMode());
        } else {
            // Pad 128 bytes (Water params size) if not present
            buffer.position(240 + 128);
        }
    }
}
