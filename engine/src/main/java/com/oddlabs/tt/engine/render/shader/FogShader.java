package com.oddlabs.tt.engine.render.shader;

/**
 * Interface for shaders that support standard fog.
 */
public interface FogShader extends Shader {
    int FOG_MODE_LINEAR = 0;
    int FOG_MODE_EXP = 1;
    int FOG_MODE_EXP2 = 2;
    int FOG_MODE_RADIAL = 3;

    String FOG_FUNCTION
            = """
                    float calculateFogFactor(
                        float dist,
                        float worldZ,
                        vec2 fragCoord
                    ) {
                        if (u_fogMode == 3) { // Radial fog for map view
                            vec2 resolution = u_fogParams.xy;
                            float density = u_fogParams.z;
                            float radius = (max(resolution.x, resolution.y) / 2.0) * u_fogHeightFactor;

                            // Center coordinates and correct for aspect ratio
                            vec2 centeredCoords = fragCoord - resolution / 2.0;

                            float distance = length(centeredCoords);
                            return 1.0 - smoothstep(radius - (radius * density), radius, distance);
                        }

                        if (u_fogMode < 0) {
                            return 1.0;
                        }

                        float seaLevel = u_fogParams.w;
                        float c_z = max(u_cameraHeight - seaLevel, 0.0);
                        float p_z = max(worldZ - seaLevel, 0.0);
                        float deltaZ = p_z - c_z;

                        float scaleHeight = u_fogParams.y > 0.0 ? u_fogParams.y : 50.0;
                        float lambda = 1.0 / scaleHeight;
                        float rho0 = u_fogParams.x * 0.80; // Scaled base density for natural aerial perspective

                        float opticalDepth;
                        float absDeltaZ = abs(deltaZ);
                        if (absDeltaZ > 0.001) {
                            opticalDepth = dist * rho0 * exp(-lambda * c_z) * ((1.0 - exp(-lambda * deltaZ)) / (lambda * deltaZ));
                        } else {
                            opticalDepth = dist * rho0 * exp(-lambda * c_z);
                        }

                        // Minimum horizon haze floor on distant ocean to dissolve outer mesh rim at high camera altitudes
                        float distFactor = smoothstep(2000.0, 6000.0, dist);
                        float minHorizonDepth = distFactor * 3.5;
                        opticalDepth = max(opticalDepth, minHorizonDepth);

                        return clamp(exp(-opticalDepth), 0.0, 1.0);
                    }

                    float calculateFogFactor(
                        float dist,
                        vec2 fragCoord
                    ) {
                        return calculateFogFactor(dist, u_fogParams.w, fragCoord);
                    }

                    vec3 applyFogWithColor(vec3 color, vec3 fogColor, float dist, float worldZ, vec2 fragCoord) {
                        float fogFactor = calculateFogFactor(dist, worldZ, fragCoord);
                        return mix(fogColor, color, fogFactor);
                    }

                    vec3 applyFog(vec3 color, float dist, float worldZ, vec2 fragCoord) {
                        return applyFogWithColor(color, u_fogColor.rgb, dist, worldZ, fragCoord);
                    }

                    vec3 applyFog(vec3 color, float dist, vec2 fragCoord) {
                        return applyFog(color, dist, u_fogParams.w, fragCoord);
                    }
                    """;
}
