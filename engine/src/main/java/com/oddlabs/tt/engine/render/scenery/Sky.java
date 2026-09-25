package com.oddlabs.tt.engine.render.scenery;

import com.oddlabs.procedural.Channel;
import com.oddlabs.tt.engine.image.GLByteImage;
import com.oddlabs.tt.engine.render.CameraState;
import com.oddlabs.tt.engine.render.DebugFlags;
import com.oddlabs.tt.engine.render.MatrixStack;
import com.oddlabs.tt.engine.render.SceneRenderer;
import com.oddlabs.tt.engine.render.Texture;
import com.oddlabs.tt.engine.render.shader.SeaBottomShader;
import com.oddlabs.tt.engine.render.shader.SkyShader;
import com.oddlabs.tt.engine.render.state.BlendMode;
import com.oddlabs.tt.engine.render.state.CullMode;
import com.oddlabs.tt.engine.render.state.DepthMode;
import com.oddlabs.tt.engine.render.state.RenderContext;
import com.oddlabs.tt.engine.resource.Resources;
import com.oddlabs.tt.engine.util.Stitcher;
import com.oddlabs.tt.engine.vbo.FloatVBO;
import com.oddlabs.tt.engine.vbo.ShortVBO;
import com.oddlabs.tt.engine.vbo.VBO;
import com.oddlabs.tt.engine.vbo.VertexArray;
import com.oddlabs.tt.procedural.landscape.LandscapeConfig;
import com.oddlabs.tt.procedural.noise.Midpoint;
import com.oddlabs.tt.simulation.landscape.HeightMap;
import com.oddlabs.tt.simulation.landscape.LandscapeEnvironment;
import com.oddlabs.tt.simulation.model.Terrain;
import com.oddlabs.util.Color;
import org.jspecify.annotations.NonNull;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.IntStream;

/**
 * Sky dome, clouds, and background water scenery renderer.
 */
public final class Sky implements SceneRenderer, AutoCloseable {
    private record WindSpeeds(float[] outer, float[] inner) {
        private static WindSpeeds fromPolar(float outerAngleDeg, float outerSpeed, float innerAngleDeg, float innerSpeed) {
            float outerRad = (float) Math.toRadians(outerAngleDeg);
            float innerRad = (float) Math.toRadians(innerAngleDeg);
            return new WindSpeeds(
                    new float[]{(float) Math.cos(outerRad) * outerSpeed, (float) Math.sin(outerRad) * outerSpeed},
                    new float[]{(float) Math.cos(innerRad) * innerSpeed, (float) Math.sin(innerRad) * innerSpeed}
            );
        }
    }

    private static final Map<Terrain, WindSpeeds> CLOUD_WINDS = Map.of(
            Terrain.NATIVE, WindSpeeds.fromPolar(230.0f, 0.20f, 245.0f, 0.38f),
            Terrain.VIKING, WindSpeeds.fromPolar(185.0f, 0.20f, 200.0f, 0.38f)
    );
    private static final float SKYDOME_HEIGHT = 0f;
    private static final int SKYDOME_DEFAULT_COLOR = 8;
    private static final int FLOATS_PER_VERTEX = 11;
    private static final int CLOUD_TEXTURE_SIZE = 512;
    private static final int CLOUD_INNER = 0;
    private static final int CLOUD_OUTER = 1;

    private static final Map<Terrain, float[]> SKYDOME_INITCOLOR = Map.of(
            Terrain.NATIVE, new float[]{0.90f, 0.95f, 1.0f},
            Terrain.VIKING, new float[]{1.50f, 0.90f, 0.65f}
    );

    private static final Map<Terrain, float[]> SKYDOME_GRADIENT = Map.of(
            Terrain.NATIVE, new float[]{0.75f, 0.825f, 0.95f},
            Terrain.VIKING, new float[]{0.60f, 0.60f, 0.85f}
    );

    private static final Map<Terrain, Color.Linear> TEX_ENV_COLOR = Map.of(
            Terrain.NATIVE, new Color.Linear(Color.toLinear(0.95f), Color.toLinear(0.975f), Color.toLinear(1.0f), 1.0f),
            Terrain.VIKING, new Color.Linear(Color.toLinear(1.0f), Color.toLinear(0.95f), Color.toLinear(0.8f), 1.0f)
    );

    public static final Map<Terrain, Color.Linear> SEA_BOTTOM_COLOR = Map.of(
            Terrain.NATIVE, new Color.Standard(0xFF_73_40_99).linear(),
            Terrain.VIKING, Color.Linear.BLACK
    );

    private static final Map<Terrain, Color.Linear> CLOUD_SHADOW = Map.of(
            Terrain.NATIVE, new Color.Linear(0.92f, 0.96f, 1.0f, 0.08f),
            Terrain.VIKING, new Color.Linear(0.68f, 0.72f, 0.84f, 0.18f)
    );

    private static final Map<Terrain, Float> HORIZON_CLOUD_FADE = Map.of(
            Terrain.NATIVE, 0.09f,
            Terrain.VIKING, 0.045f
    );

    private static final float SKYDOME_OUTER_UTILING = 8f;
    private static final float SKYDOME_OUTER_VTILING = 8f;
    private static final float SKYDOME_INNER_UTILING = 8f;
    private static final float SKYDOME_INNER_VTILING = 8f;

    private static final int NUM_WATER_RINGS = 6;

    private static final float START_ANGLE = -(float) Math.PI / 4f;

    private final Color.Linear skyColor;
    private final Color.Linear seaBottomColor;
    private final Color.@NonNull Linear cloudShadow;
    private final float horizonCloudFade;
    private final float originX;
    private final float originY;
    private final float originZ;
    private final ShortVBO[] strip_indices;
    private final ShortVBO fan_indices;
    private final FloatVBO water_vertices;
    private final FloatVBO bottom_vertices;
    private final ShortVBO water_indices;
    private final FloatVBO sky_vbo;

    private final Texture[] clouds;
    private final int subdiv_axis;
    private final int subdiv_height;
    private final Terrain terrain;

    private final SkyShader skyShader = new SkyShader();
    private final SeaBottomShader seaBottomShader = new SeaBottomShader();
    private final Texture detail;
    private final Texture detailNormal;
    private final VertexArray skyVAO;
    private final VertexArray seaBottomVAO;

    // Cloud animation state
    private final float[] innerOffset = new float[2];
    private final float[] outerOffset = new float[2];
    private final float[] speedOuter;
    private final float[] speedInner;

    public Sky(HeightMap heightMap, Terrain terrain, Texture detail,
            Texture detailNormal) {
        this(heightMap, terrain, (float) (heightMap.getMetersPerWorld() * Math.sqrt(2) / 2), 6000f, 32, 32,
                SKYDOME_OUTER_UTILING, SKYDOME_OUTER_VTILING, SKYDOME_INNER_UTILING, SKYDOME_INNER_VTILING, heightMap
                        .getMetersPerWorld() / 2f,
                heightMap.getMetersPerWorld() / 2f, SKYDOME_HEIGHT, detail, detailNormal);
    }

    private Sky(HeightMap heightMap, Terrain terrain,
            float inner_radius, float radius, int subdiv_axis, int subdiv_height, float outer_utile, float outer_vtile,
            float inner_utile, float inner_vtile, float origin_x, float origin_y, float origin_z,
            Texture detail, Texture detailNormal) {
        this.terrain = terrain;
        WindSpeeds wind = CLOUD_WINDS.get(terrain);
        this.speedOuter = wind.outer();
        this.speedInner = wind.inner();
        this.detail = detail;
        this.detailNormal = detailNormal;
        this.subdiv_axis = subdiv_axis;
        this.subdiv_height = subdiv_height;
        this.skyColor = TEX_ENV_COLOR.get(terrain);
        this.seaBottomColor = SEA_BOTTOM_COLOR.get(terrain);
        this.cloudShadow = CLOUD_SHADOW.get(terrain);
        this.horizonCloudFade = HORIZON_CLOUD_FADE.get(terrain);
        this.originX = origin_x;
        this.originY = origin_y;
        this.originZ = origin_z;
        this.clouds = Resources.findResource(new CloudTextures(terrain));

        // Create interleaved VBO for the sky
        int num_vertices_sky = subdiv_axis * (subdiv_height - 1) + 1;
        int stride = FLOATS_PER_VERTEX * Float.BYTES; // pos, norm, uv0, uv1, color
        try (var stack = MemoryStack.stackPush()) {
            FloatBuffer skyBuffer = stack.mallocFloat(num_vertices_sky * (stride / Float.BYTES));
            makeSkyVertices(radius, outer_utile, outer_vtile, inner_utile, inner_vtile, origin_x, origin_y, origin_z,
                    skyBuffer);
            skyBuffer.flip();
            sky_vbo = new FloatVBO(GL15.GL_STATIC_DRAW, skyBuffer);
        }

        strip_indices = makeSkyStripIndices();
        fan_indices = makeSkyFanIndices();

        // --- Sea bottom and water stitching logic ---
        List<SkyStitchVertex[]> vertices_stitch_list = new ArrayList<>();
        List<ShortBuffer> stitch_indices_list = new ArrayList<>();
        int num_vertices_water = 0;
        int num_indices = 0;
        SkyStitchVertex[] previous_vertices = makeLandscapeVertices(heightMap);
        vertices_stitch_list.add(previous_vertices);
        num_vertices_water += previous_vertices.length;
        for (int i = 0; i < NUM_WATER_RINGS; i++) {
            float radius_factor = (float) (i + 1) / NUM_WATER_RINGS;
            float ring_radius = inner_radius + (float) Math.pow(radius - inner_radius, radius_factor);
            SkyStitchVertex[] ring_vertices = makeDomeVertices(heightMap, i + 1,
                    num_vertices_water, ring_radius, origin_x, origin_y);
            vertices_stitch_list.add(ring_vertices);
            num_vertices_water += ring_vertices.length;
            SkyStitchVertex[] stitch_vertices = new SkyStitchVertex[ring_vertices.length + previous_vertices.length];
            System.arraycopy(previous_vertices, 0, stitch_vertices, 0, previous_vertices.length);
            System.arraycopy(ring_vertices, 0, stitch_vertices, previous_vertices.length, ring_vertices.length);
            ShortBuffer stitch_indices = Stitcher.stitch(stitch_vertices);
            stitch_indices_list.add(stitch_indices);
            num_indices += stitch_indices.remaining();
            previous_vertices = ring_vertices;
        }
        SkyStitchVertex[] all_vertices = new SkyStitchVertex[num_vertices_water];
        int index = 0;
        for (SkyStitchVertex[] vertices : vertices_stitch_list) {
            System.arraycopy(vertices, 0, all_vertices, index, vertices.length);
            index += vertices.length;
        }
        ShortBuffer all_indices = BufferUtils.createShortBuffer(num_indices);
        for (ShortBuffer indices : stitch_indices_list) {
            all_indices.put(indices);
        }
        all_indices.flip();
        water_indices = new ShortVBO(GL15.GL_STATIC_DRAW, all_indices);
        water_vertices = toVBO(all_vertices, heightMap.getSeaLevelMeters());
        bottom_vertices = toVBO(all_vertices, 0);

        this.skyVAO = new VertexArray();
        skyVAO.bind();
        sky_vbo.bind();
        GL20.glEnableVertexAttribArray(0); // Position
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, stride, 0L);
        GL20.glEnableVertexAttribArray(1); // TexCoord0
        GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, stride, 3L * Float.BYTES);
        GL20.glEnableVertexAttribArray(2); // TexCoord1
        GL20.glVertexAttribPointer(2, 2, GL11.GL_FLOAT, false, stride, 5L * Float.BYTES);
        GL20.glEnableVertexAttribArray(3); // Color (rgb) & Elevation (a)
        GL20.glVertexAttribPointer(3, 4, GL11.GL_FLOAT, false, stride, 7L * Float.BYTES);
        skyVAO.unbind();

        this.seaBottomVAO = new VertexArray();
        seaBottomVAO.bind();
        bottom_vertices.bind();
        GL20.glEnableVertexAttribArray(0); // Position
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 0, 0);
        seaBottomVAO.unbind();
    }

    public void render(RenderContext context, CameraState state, MatrixStack modelView,
            MatrixStack projection, float currentTime) {
        try (var _ = skyShader.use(); var _ = context.withBlendMode(BlendMode.NONE); var _ = context.withDepthMode(
                DepthMode.READ_WRITE); var _ = context.withCullMode(CullMode.BACK)) {

            skyShader.setUniform(skyShader.locModelViewMatrix, modelView.current());
            skyShader.setUniform(skyShader.locSkyColor, skyColor);
            skyShader.setUniform(skyShader.locDomeCenter, originX, originY, originZ);
            skyShader.setUniform(skyShader.locCloudShadow, cloudShadow);
            skyShader.setUniform(skyShader.locHorizonCloudFade, horizonCloudFade);

            context.setTexture(0, clouds[CLOUD_INNER]);
            skyShader.setUniform(skyShader.locTexture0, 0);

            context.setTexture(1, clouds[CLOUD_OUTER]);
            skyShader.setUniform(skyShader.locTexture1, 1);

            float speedScale = 0.01f;
            outerOffset[0] = speedOuter[0] * currentTime * speedScale;
            outerOffset[1] = speedOuter[1] * currentTime * speedScale;
            innerOffset[0] = speedInner[0] * currentTime * speedScale;
            innerOffset[1] = speedInner[1] * currentTime * speedScale;

            skyShader.setUniform(skyShader.locOuterOffset, outerOffset[0], outerOffset[1]);
            skyShader.setUniform(skyShader.locInnerOffset, innerOffset[0], innerOffset[1]);

            skyVAO.bind();

            for (ShortVBO strip_index : strip_indices) {
                strip_index.drawElements(GL11.GL_TRIANGLE_STRIP, subdiv_axis * 2 + 2, 0);
            }
            fan_indices.drawElements(GL11.GL_TRIANGLE_FAN, subdiv_axis + 2, 0);

            skyVAO.unbind();
        } finally {
            VBO.releaseIndexVBO();
        }
    }

    @Override
    public void render(RenderContext context, CameraState state, MatrixStack modelView,
            MatrixStack projection) {
        render(context, state, modelView, projection, 0.0f);
    }


    public void renderSeaBottom(RenderContext context, CameraState state,
            MatrixStack modelView, MatrixStack projection) {
        try (var _ = seaBottomShader.use(); var _ = context.withBlendMode(BlendMode.NONE); var _ = context
                .withDepthMode(DepthMode.READ_WRITE); var _ = context.withCullMode(CullMode.BACK)) {

            seaBottomShader.setUniform(seaBottomShader.locModelViewMatrix, modelView.current());

            seaBottomShader.setUniform(seaBottomShader.locBaseColor, seaBottomColor);

            if (DebugFlags.draw_detail) {
                context.setTexture(1, detail);
                seaBottomShader.setUniform(seaBottomShader.locTexture1, 1);
                context.setTexture(2, detailNormal);
                seaBottomShader.setUniform(seaBottomShader.locTextureNormal, 2);
                seaBottomShader.setUniform(seaBottomShader.locDetailScale,
                        LandscapeConfig.LANDSCAPE_DETAIL_REPEAT_RATE);
            } else {
                seaBottomShader.setUniform(seaBottomShader.locDetailScale, 0f);
            }

            seaBottomVAO.bind();
            water_indices.drawElements(GL11.GL_TRIANGLES, water_indices.capacity(), 0);
            seaBottomVAO.unbind();

            context.setActiveTexture(0);
        } finally {
            VBO.releaseIndexVBO();
        }
    }

    private static FloatVBO toVBO(SkyStitchVertex[] vertices, float height) {
        FloatBuffer vertex_buffer = BufferUtils.createFloatBuffer(vertices.length * 3);
        for (SkyStitchVertex vertex : vertices) {
            float x = vertex.x;
            float y = vertex.y;
            float z = (height * (NUM_WATER_RINGS - vertex.getSide())) / NUM_WATER_RINGS;
            vertex_buffer.put(x).put(y).put(z);
        }
        vertex_buffer.flip();
        return new FloatVBO(GL15.GL_STATIC_DRAW, vertex_buffer);
    }

    public FloatVBO getWaterVertices() {
        return water_vertices;
    }

    public Color.Linear getSkyColor() {
        return skyColor;
    }

    public ShortVBO getWaterIndices() {
        return water_indices;
    }

    public float[] getInnerOffset() {
        return innerOffset;
    }

    public float[] getOuterOffset() {
        return outerOffset;
    }

    public float getInnerCloudDensity() {
        return 0f;
    }

    public float getOuterCloudDensity() {
        return 0f;
    }

    public Texture[] getClouds() {
        return clouds;
    }

    private void makeSkyVertices(float radius, float outer_utile, float outer_vtile, float inner_utile,
            float inner_vtile, float origin_x, float origin_y, float origin_z, FloatBuffer buffer) {
        float r;
        float x, y, z;
        float height_coeff;
        float dome_height = radius;
        float h_angle_inc = ((float) Math.PI / 2) / (subdiv_height - 1);
        float a_angle_inc = (float) Math.PI * 2 / subdiv_axis;
        float offset_angle = a_angle_inc / 2f;

        float[] skydome_gradient_const = SKYDOME_GRADIENT.get(terrain);
        float[] skydome_init_color = SKYDOME_INITCOLOR.get(terrain);

        float[] skydome_default_color = new float[]{
                (float) Math.pow(skydome_gradient_const[0], SKYDOME_DEFAULT_COLOR),
                (float) Math.pow(skydome_gradient_const[1], SKYDOME_DEFAULT_COLOR),
                (float) Math.pow(skydome_gradient_const[2], SKYDOME_DEFAULT_COLOR)
        };

        float[][] skydome_gradient = new float[subdiv_height][3];
        skydome_gradient[0] = skydome_init_color.clone();

        for (int i = 1; i < subdiv_height; i++) {
            float alpha = (float) i / (subdiv_height - 1);
            skydome_gradient[i] = new float[]{
                    alpha * skydome_default_color[0] + (1f - alpha) * skydome_gradient[i - 1][0] * skydome_gradient_const[0],
                    alpha * skydome_default_color[1] + (1f - alpha) * skydome_gradient[i - 1][1] * skydome_gradient_const[1],
                    alpha * skydome_default_color[2] + (1f - alpha) * skydome_gradient[i - 1][2] * skydome_gradient_const[2]
            };
        }

        // Convert the computed sRGB/display gradient colors to linear HDR scene colors
        Color.Linear[] skydome_gradient_linear = new Color.Linear[subdiv_height];
        for (int i = 0; i < subdiv_height; i++) {
            skydome_gradient_linear[i] = new Color.Linear(
                    Color.toLinear(skydome_gradient[i][0]),
                    Color.toLinear(skydome_gradient[i][1]),
                    Color.toLinear(skydome_gradient[i][2]),
                    1.0f
            );
        }
        Color.Linear skydome_default_linear = new Color.Linear(
                Color.toLinear(skydome_default_color[0]),
                Color.toLinear(skydome_default_color[1]),
                Color.toLinear(skydome_default_color[2]),
                1.0f
        );

        for (int i = 0; i < subdiv_height - 1; i++) {
            z = (float) Math.sin(h_angle_inc * i) * radius;
            r = (float) Math.cos(h_angle_inc * i) * radius;
            height_coeff = Math.abs(z) < 250f ? dome_height / 250f : dome_height / z;
            float elevation = (float) i / (subdiv_height - 1);

            for (int j = 0; j < subdiv_axis; j++) {
                x = (float) Math.cos(START_ANGLE + a_angle_inc * j + offset_angle * i) * r;
                y = (float) Math.sin(START_ANGLE + a_angle_inc * j + offset_angle * i) * r;

                buffer.put(x + origin_x).put(y + origin_y).put(z + origin_z); // Position
                buffer.put(x * height_coeff / (radius * outer_utile) + 0.5f).put(y * height_coeff / (radius
                        * outer_vtile) + 0.5f); // TexCoord0
                buffer.put(x * height_coeff / (radius * inner_utile) + 0.5f).put(y * height_coeff / (radius
                        * inner_vtile) + 0.5f); // TexCoord1
                Color.Linear colorVal = skydome_gradient_linear[i];
                buffer.put(colorVal.r()).put(colorVal.g()).put(colorVal.b()).put(elevation); // Color (rgb) & Elevation (a)
            }
        }
        buffer.put(origin_x).put(origin_y).put(radius + origin_z); // Position
        buffer.put(0.5f).put(0.5f); // TexCoord0
        buffer.put(0.5f).put(0.5f); // TexCoord1
        buffer.put(skydome_default_linear.r()).put(skydome_default_linear.g()).put(skydome_default_linear.b()).put(1.0f); // Color & Elevation
    }


    private ShortVBO[] makeSkyStripIndices() {
        ShortVBO[] strip_indices = new ShortVBO[subdiv_height - 2];
        try (var stack = MemoryStack.stackPush()) {
            for (int i = 0; i < strip_indices.length; i++) {
                int size = subdiv_axis * 2 + 2;
                ShortBuffer temp = stack.mallocShort(size);
                for (int j = 0; j < subdiv_axis; j++) {
                    temp.put(j * 2, (short) (i * subdiv_axis + j));
                    temp.put(j * 2 + 1, (short) ((i + 1) * subdiv_axis + j));
                }
                temp.put(subdiv_axis * 2, (short) (i * subdiv_axis));
                temp.put(subdiv_axis * 2 + 1, (short) ((i + 1) * subdiv_axis));
                strip_indices[i] = new ShortVBO(GL15.GL_STATIC_DRAW, size);
                temp.rewind();
                strip_indices[i].put(temp);
            }
        }
        return strip_indices;
    }

    private ShortVBO makeSkyFanIndices() {
        int size = subdiv_axis + 2;
        try (var stack = MemoryStack.stackPush()) {
            ShortBuffer temp = stack.mallocShort(size);
            temp.put(0, (short) (sky_vbo.capacity() / FLOATS_PER_VERTEX - 1));
            for (int i = 0; i < subdiv_axis; i++) {
                temp.put(i + 1, (short) ((subdiv_height - 1) * subdiv_axis - i - 1));
            }
            temp.put(subdiv_axis + 1, (short) ((subdiv_height - 1) * subdiv_axis - 1));

            ShortVBO fan_indices = new ShortVBO(GL15.GL_STATIC_DRAW, size);
            temp.rewind();
            fan_indices.put(temp);
            return fan_indices;
        }
    }

    private SkyStitchVertex[] makeDomeVertices(LandscapeEnvironment heightmap, int ring_id,
            int index_offset, float radius, float origin_x, float origin_y) {
        float a_angle_inc = (float) Math.PI * 2 / subdiv_axis;
        return IntStream.range(0, subdiv_axis)
                .mapToObj(i -> {
                    int index = i + index_offset;
                    return new SkyStitchVertex(heightmap, index, ring_id,
                            (float) Math.cos(START_ANGLE + a_angle_inc * i) * radius + origin_x,
                            (float) Math.sin(START_ANGLE + a_angle_inc * i) * radius + origin_y);
                }).toArray(SkyStitchVertex[]::new);
    }

    private SkyStitchVertex[] makeLandscapeVertices(LandscapeEnvironment heightmap) {
        int gridUnitsPerWorld = heightmap.getGridUnitsPerWorld();
        int size = 4 * gridUnitsPerWorld;
        SkyStitchVertex[] result = new SkyStitchVertex[size];

        int metersPerUnit = HeightMap.METERS_PER_UNIT_GRID;
        int metersPerWorld = heightmap.getMetersPerWorld();

        for (int i = 0; i < gridUnitsPerWorld; i++) {
            int index = i;
            result[index] = new SkyStitchVertex(heightmap, index, 0, 0, i * metersPerUnit);

            index = i + gridUnitsPerWorld;
            result[index] = new SkyStitchVertex(heightmap, index, 0, i * metersPerUnit, metersPerWorld);

            index = i + gridUnitsPerWorld * 2;
            result[index] = new SkyStitchVertex(heightmap, index, 0, metersPerWorld, metersPerWorld - i
                    * metersPerUnit);

            index = i + gridUnitsPerWorld * 3;
            result[index] = new SkyStitchVertex(heightmap, index, 0, metersPerWorld - i * metersPerUnit, 0);
        }
        return result;
    }

    private static class SkyStitchVertex extends Stitcher.Vertex<SkyStitchVertex> {
        private final float x;
        private final float y;
        private final float theta;
        private final LandscapeEnvironment heightmap;

        private SkyStitchVertex(LandscapeEnvironment heightmap, int index, int side, float x, float y) {
            super(index, side);
            this.heightmap = heightmap;
            this.x = x;
            this.y = y;
            float half_world_size = heightmap.getMetersPerWorld() * .5f;
            this.theta = (float) Math.atan2(y - half_world_size, x - half_world_size);
        }

        @Override
        public final int compareTo(SkyStitchVertex o) {
            return -Float.compare(theta, o.theta);
        }
    }

    private record CloudTextures(Terrain terrain) implements Supplier<Texture[]> {
        @Override
        public Texture[] get() {
            int seed = LandscapeConfig.LANDSCAPE_SEED;
            Channel clouds1 = new Midpoint(CLOUD_TEXTURE_SIZE, 3, 0.55f, seed).toChannel();
            Channel clouds2 = new Midpoint(CLOUD_TEXTURE_SIZE, 2, 0.4f, seed).toChannel();

            List<Channel> channels = switch (terrain) {
                case NATIVE -> List.of(
                        clouds1.dynamicRange(0.5f, 1f, 0f, 1f).gamma(0.75f).brightness(0.5f),
                        clouds2.dynamicRange(0.25f, 1f, 0f, 1f).gamma(0.5f).brightness(0.33f));
                case VIKING -> List.of(
                        clouds1.dynamicRange(0.5f, 1f, 0f, 0.75f),
                        clouds2.dynamicRange(0.5f, 1f, 0f, 0.75f));
            };

            return channels.stream()
                    .map(cloud -> new GLByteImage(cloud, GL11.GL_RED))
                    .map(image -> new Texture(image, GL30.GL_R8, GL11.GL_LINEAR_MIPMAP_LINEAR,
                            GL11.GL_LINEAR, GL11.GL_REPEAT, GL11.GL_REPEAT))
                    .toArray(Texture[]::new);
        }
    }

    @Override
    public void close() {
        skyVAO.close();
        seaBottomVAO.close();
        skyShader.close();
        seaBottomShader.close();
        sky_vbo.close();
        water_vertices.close();
        bottom_vertices.close();
        water_indices.close();
        fan_indices.close();
        for (ShortVBO vbo : strip_indices) {
            vbo.close();
        }
    }
}
