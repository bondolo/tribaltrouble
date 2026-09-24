package com.oddlabs.tt.engine.render.shader;

/**
 * Common lighting model shader contracts and functions.
 */
interface LitShader extends Shader {
    String PERTURB_NORMAL_FUNC = """
            mat3 cotangent_frame(vec3 N, vec3 p, vec2 uv) {
                // get edge vectors of the pixel triangle
                vec3 dp1 = dFdx(p);
                vec3 dp2 = dFdy(p);
                vec2 duv1 = dFdx(uv);
                vec2 duv2 = dFdy(uv);

                // solve the linear system
                vec3 dp2perp = cross(dp2, N);
                vec3 dp1perp = cross(N, dp1);
                vec3 T = dp2perp * duv1.x + dp1perp * duv2.x;
                vec3 B = dp2perp * duv1.y + dp1perp * duv2.y;

                // construct a scale-invariant frame
                float invmax = inversesqrt(max(dot(T,T), dot(B,B)));
                return mat3(T * invmax, B * invmax, N);
            }

            vec3 perturbNormal(vec3 N, vec3 V, vec2 texcoord, vec3 map) {
                // assume N, the interpolated vertex normal and
                // V, the view vector (vertex to eye)
                map = map * 255./127. - 128./127.;
                mat3 TBN = cotangent_frame(N, -V, texcoord);
                return normalize(TBN * map);
            }
            """;

    /**
     * Simple vertex-based diffuse lighting (Legacy/FFP emulation).
     */
    String VERTEX_LIGHTING_FUNCTION = """
            vec4 calculateVertexLighting(
                vec3 normal,
                vec4 materialColor,
                mat4 modelViewMatrix
            ) {
                // Transform normal to view space
                vec3 transformedNormal = normalize((modelViewMatrix * vec4(normal, 0.0)).xyz);
                vec3 worldNormal = normalize((transpose(u_viewMatrix) * vec4(transformedNormal, 0.0)).xyz);
                vec3 lightDir = normalize((u_viewMatrix * vec4(u_lightDirection.xyz, 0.0)).xyz);

                // Standard Lambertian diffuse matching legacy
                float diff = max(dot(transformedNormal, lightDir), 0.0);

                // Ambient matching legacy
                vec3 ambient = u_globalAmbient.rgb;

                vec3 light = ambient + vec3(diff);

                // Apply lighting to material color
                return vec4(materialColor.rgb * clamp(light, 0.0, 1.0), materialColor.a);
            }
            """;

    /**
     * Advanced fragment-based lighting with specular support.
     */
    String FRAGMENT_LIGHT_DIR = "vec3 lightDir = normalize((u_viewMatrix * vec4(u_lightDirection.xyz, 0.0)).xyz);";

    String FRAGMENT_LIGHTING_FUNCTION = """
            vec3 calculateLighting(vec3 normal, vec3 worldNormal, vec3 viewPos, float specularStrength) {
                """ + FRAGMENT_LIGHT_DIR + """

                // Standard Lambertian diffuse matching legacy
                float diff = max(dot(normal, lightDir), 0.0);

                // Ambient matching legacy
                vec3 ambient = u_globalAmbient.rgb;

                // Specular (Blinn-Phong)
                vec3 viewDir = normalize(-viewPos);
                vec3 halfDir = normalize(lightDir + viewDir);
                float spec = pow(max(dot(normal, halfDir), 0.0), 32.0);
                vec3 specular = specularStrength * spec * vec3(1.0);

                return clamp(ambient + diff * vec3(1.0) + specular, 0.0, 1.0);
            }
            """;
}
