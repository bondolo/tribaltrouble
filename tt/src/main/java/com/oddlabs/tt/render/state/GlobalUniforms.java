package com.oddlabs.tt.render.state;

import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.render.shader.FogShader;
import com.oddlabs.tt.resource.DistanceFogInfo;
import com.oddlabs.tt.resource.FogInfo;
import com.oddlabs.tt.resource.RadialFogInfo;
import com.oddlabs.util.Color;
import org.jspecify.annotations.NonNull;

import java.nio.ByteBuffer;

/**
 * Helper class to pack global uniform data into a ByteBuffer according to std140 layout.
 */
public final class GlobalUniforms {

    public void update(@NonNull CameraState camera, float time, @NonNull ByteBuffer buffer) {
        buffer.clear();

        // 0: mat4 projection (64)
        camera.getProjectionMatrix().get(0, buffer);

        // 64: mat4 view (64)
        camera.getModelView().get(64, buffer);

        // 128: vec4 fogColor (16)
        buffer.position(128);
        FogInfo fog = camera.getFog();
        Color color = fog.getColor();
        assert color instanceof Color.Linear : "Color must be linear, not " + color.getClass().getSimpleName();
        buffer.putFloat(color.r());
        buffer.putFloat(color.g());
        buffer.putFloat(color.b());
        buffer.putFloat(color.a());

        // 144: vec3 fogParams (16 aligned)
        // 156: float cameraHeight (4) -- Packed tightly after vec3
        // 160: float fogHeightFactor (4)
        // 164: float globalTime (4)
        // 168: int fogMode (4)

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
        // NO padding here; vec3 takes 12 bytes, next float starts at 12 bytes offset (align 4)

        buffer.putFloat(ch);
        buffer.putFloat(hf);
        buffer.putFloat(time);
        buffer.putInt(mode);

        buffer.position(176); // End of data (172 used, pad to 176 for 16-byte alignment)
    }
}
