package com.oddlabs.tt.scenery;

import com.oddlabs.procedural.Channel;
import com.oddlabs.procedural.Layer;
import com.oddlabs.tt.engine.image.GLImage;
import com.oddlabs.tt.engine.image.GLIntImage;
import com.oddlabs.tt.engine.render.CameraState;
import com.oddlabs.tt.engine.render.DebugFlags;
import com.oddlabs.tt.engine.render.HeightMapVisual;
import com.oddlabs.tt.engine.render.MatrixStack;
import com.oddlabs.tt.engine.render.PatchMesh;
import com.oddlabs.tt.engine.render.Texture;
import com.oddlabs.tt.engine.render.state.BlendMode;
import com.oddlabs.tt.engine.render.state.CullMode;
import com.oddlabs.tt.engine.render.state.DepthMode;
import com.oddlabs.tt.engine.render.state.RenderContext;
import com.oddlabs.tt.engine.render.state.WaterUniformsProvider;
import com.oddlabs.tt.engine.resource.Resources;
import com.oddlabs.tt.engine.vbo.FloatVBO;
import com.oddlabs.tt.engine.vbo.VertexArray;
import com.oddlabs.tt.procedural.landscape.LandscapeConfig;
import com.oddlabs.tt.procedural.noise.Midpoint;
import com.oddlabs.tt.procedural.noise.Perlin;
import com.oddlabs.tt.procedural.noise.Voronoi;
import com.oddlabs.tt.simulation.landscape.HeightMap;
import com.oddlabs.tt.simulation.landscape.LandscapeLeaf;
import com.oddlabs.tt.simulation.model.Terrain;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL33;

import java.nio.FloatBuffer;
import java.util.ArrayDeque;
import java.util.BitSet;
import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/**
 * Water surface renderer for oceans and inland water.
 */
public final class Water implements WaterUniformsProvider, AutoCloseable {
    private static final int OCEAN_TEXTURE_SIZE = 512;

    public static final int WAVE_COUNT = 3;
    public static final float WAVE_AMPLITUDE_BASE = 0.15f;
    public static final float WAVE_STEEPNESS_BASE = 0.5f;

    public static final float WAVE_AMPLITUDE_SCALE_2 = 0.53f;
    public static final float WAVE_AMPLITUDE_SCALE_3 = 0.27f;

    public static final float VIKING_AMPLITUDE_MULTIPLIER = 1.5f;
    public static final float VIKING_STEEPNESS_MULTIPLIER = 1.2f;
    public static final float VIKING_WAVE_SPEED = 0.8f;

    public static final float NATIVE_WAVE_SPEED = 0.4f;

    public static final float WAVE_DIR_X_1 = 1.0f;
    public static final float WAVE_DIR_X_2 = 0.707f;
    public static final float WAVE_DIR_X_3 = -0.5f;
    public static final float WAVE_DIR_Y_1 = 0.0f;
    public static final float WAVE_DIR_Y_2 = 0.707f;
    public static final float WAVE_DIR_Y_3 = 0.866f;

    public static final float NATIVE_WAVE_LEN_1 = 60.0f;
    public static final float NATIVE_WAVE_LEN_2 = 35.0f;
    public static final float NATIVE_WAVE_LEN_3 = 18.0f;

    public static final float VIKING_WAVE_LEN_1 = 50.0f;
    public static final float VIKING_WAVE_LEN_2 = 28.0f;
    public static final float VIKING_WAVE_LEN_3 = 14.0f;

    private static final float[] WAVE_DIRS_X = new float[]{WAVE_DIR_X_1, WAVE_DIR_X_2, WAVE_DIR_X_3};
    private static final float[] WAVE_DIRS_Y = new float[]{WAVE_DIR_Y_1, WAVE_DIR_Y_2, WAVE_DIR_Y_3};

    private final Terrain terrain;
    private final Sky sky;
    private final MatrixStack modelViewStack;
    private final HeightMap heightMap;
    private final HeightMapVisual heightMapVisual;

    private final Texture[] ocean;

    private final WaterShader waterShader = new WaterShader();
    private final VertexArray skyWaterVao = new VertexArray();
    private final PatchMesh patchMesh = new PatchMesh();

    private final BitSet oceanPatches;
    private final Texture oceanMaskTexture;

    // Non-final to allow resizing
    private FloatVBO instanceVBO = new FloatVBO(GL15.GL_STREAM_DRAW, 1024 * 3);
    private FloatBuffer instanceBuffer = BufferUtils.createFloatBuffer(1024 * 3);

    private final float[] scrollOffset0 = new float[2];
    private final float[] scrollOffset1 = new float[2];
    private float flowDirection = (float) Math.toRadians(45f);
    private float flowSpeed = 0.001f;
    private float targetFlowDirection = flowDirection;
    private float targetFlowSpeed = flowSpeed;
    private float timeSinceChange = 0f;
    private float changeInterval = 20f;
    private float lastTime = 0f;

    private float waveTime = 0f;
    private final float[] waveAmplitudes;
    private final float[] waveSteepness;
    private final float[] waveLengths;
    private final float waveSpeed;

    public Water(HeightMapVisual heightMapVisual, HeightMap heightmap, Terrain terrain, Sky sky,
            MatrixStack modelViewStack) {
        this.heightMapVisual = heightMapVisual;
        this.heightMap = heightmap;
        this.terrain = terrain;
        waveAmplitudes = switch (terrain) {
            case VIKING -> new float[]{
                    WAVE_AMPLITUDE_BASE * VIKING_AMPLITUDE_MULTIPLIER,
                    WAVE_AMPLITUDE_BASE * VIKING_AMPLITUDE_MULTIPLIER * WAVE_AMPLITUDE_SCALE_2,
                    WAVE_AMPLITUDE_BASE * VIKING_AMPLITUDE_MULTIPLIER * WAVE_AMPLITUDE_SCALE_3
            };
            case NATIVE -> new float[]{
                    WAVE_AMPLITUDE_BASE,
                    WAVE_AMPLITUDE_BASE * WAVE_AMPLITUDE_SCALE_2,
                    WAVE_AMPLITUDE_BASE * WAVE_AMPLITUDE_SCALE_3
            };
        };
        waveSteepness = switch (terrain) {
            case VIKING -> new float[]{
                    WAVE_STEEPNESS_BASE * VIKING_STEEPNESS_MULTIPLIER,
                    WAVE_STEEPNESS_BASE * VIKING_STEEPNESS_MULTIPLIER,
                    WAVE_STEEPNESS_BASE * VIKING_STEEPNESS_MULTIPLIER
            };
            case NATIVE -> new float[]{
                    WAVE_STEEPNESS_BASE,
                    WAVE_STEEPNESS_BASE,
                    WAVE_STEEPNESS_BASE
            };
        };
        waveLengths = switch (terrain) {
            case VIKING -> new float[]{VIKING_WAVE_LEN_1, VIKING_WAVE_LEN_2, VIKING_WAVE_LEN_3};
            case NATIVE -> new float[]{NATIVE_WAVE_LEN_1, NATIVE_WAVE_LEN_2, NATIVE_WAVE_LEN_3};
        };
        waveSpeed = switch (terrain) {
            case VIKING -> VIKING_WAVE_SPEED;
            case NATIVE -> NATIVE_WAVE_SPEED;
        };
        this.ocean = Resources.findResource(new OceanTextures(terrain));

        this.sky = sky;
        this.modelViewStack = modelViewStack;

        skyWaterVao.bind();
        setupWaterAttributes(sky.getWaterVertices());
        skyWaterVao.unbind();

        int gridUnits = heightmap.getGridUnitsPerWorld();
        float seaLevel = heightmap.getSeaLevelMeters();
        BitSet oceanGrid = new BitSet(gridUnits * gridUnits);
        Queue<int[]> queue = new ArrayDeque<>();

        for (int x = 0; x < gridUnits; x++) {
            if (heightmap.getWrappedHeight(x, 0) < seaLevel) {
                oceanGrid.set(x);
                queue.add(new int[]{x, 0});
            }
            if (heightmap.getWrappedHeight(x, gridUnits - 1) < seaLevel) {
                int index = (gridUnits - 1) * gridUnits + x;
                oceanGrid.set(index);
                queue.add(new int[]{x, gridUnits - 1});
            }
        }
        for (int y = 1; y < gridUnits - 1; y++) {
            if (heightmap.getWrappedHeight(0, y) < seaLevel) {
                int index = y * gridUnits;
                oceanGrid.set(index);
                queue.add(new int[]{0, y});
            }
            if (heightmap.getWrappedHeight(gridUnits - 1, y) < seaLevel) {
                int index = y * gridUnits + (gridUnits - 1);
                oceanGrid.set(index);
                queue.add(new int[]{gridUnits - 1, y});
            }
        }

        int[][] orthogonalNeighbors = new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!queue.isEmpty()) {
            int[] current = queue.poll();
            int currX = current[0];
            int currY = current[1];

            for (int[] offset : orthogonalNeighbors) {
                int nx = currX + offset[0];
                int ny = currY + offset[1];
                if (nx >= 0 && nx < gridUnits && ny >= 0 && ny < gridUnits) {
                    int index = ny * gridUnits + nx;
                    if (!oceanGrid.get(index) && heightmap.getWrappedHeight(nx, ny) < seaLevel) {
                        oceanGrid.set(index);
                        queue.add(new int[]{nx, ny});
                    }
                }
            }
        }

        int patchesPerWorld = heightmap.getPatchesPerWorld();
        int unitsPerPatch = heightmap.getGridUnitsPerPatch();
        this.oceanPatches = new BitSet(patchesPerWorld * patchesPerWorld);
        for (int py = 0; py < patchesPerWorld; py++) {
            for (int px = 0; px < patchesPerWorld; px++) {
                if (heightmap.isBelowSeaLevel(px, py)) {
                    boolean connectedToOcean = false;
                    for (int y = 0; y <= unitsPerPatch; y++) {
                        for (int x = 0; x <= unitsPerPatch; x++) {
                            int gx = px * unitsPerPatch + x;
                            int gy = py * unitsPerPatch + y;
                            if (oceanGrid.get(gy * gridUnits + gx)) {
                                connectedToOcean = true;
                                break;
                            }
                        }
                        if (connectedToOcean) break;
                    }
                    if (connectedToOcean) {
                        oceanPatches.set(py * patchesPerWorld + px);
                    }
                }
            }
        }

        float[] oceanData = new float[gridUnits * gridUnits];
        for (int i = 0; i < oceanData.length; i++) {
            if (oceanGrid.get(i)) {
                oceanData[i] = 1.0f;
            }
        }
        this.oceanMaskTexture = new Texture(oceanData, gridUnits, gridUnits, GL30.GL_R8,
                GL11.GL_LINEAR, GL11.GL_LINEAR, GL12.GL_CLAMP_TO_EDGE);
    }


    private void setupWaterAttributes(FloatVBO vbo) {
        vbo.bind();
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 0, 0L);
    }


    public void render(RenderContext context, CameraState state, Collection<
            LandscapeLeaf> visiblePatches, float currentTime) {
        updateAnimation(currentTime);

        try (var _ = waterShader.use(); var _ = context.withBlendMode(BlendMode.ALPHA); var _ = context.withDepthMode(
                DepthMode.READ_WRITE); var _ = context.withCullMode(CullMode.NONE)) {

            context.setBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

            waterShader.setUniform(waterShader.locModelViewMatrix, modelViewStack.current());

            waterShader.setUniform(waterShader.locCameraPos, state.getCurrentX(), state.getCurrentY(), state
                    .getCurrentZ());

            context.setTexture(0, ocean[0]);
            waterShader.setUniform(waterShader.locTexture0, 0);

            if (DebugFlags.draw_detail) {
                context.setTexture(1, ocean[1]);
                waterShader.setUniform(waterShader.locTexture1, 1);
                waterShader.setUniform(waterShader.locEnableDetail, true);
            } else {
                waterShader.setUniform(waterShader.locEnableDetail, false);
            }

            context.setTexture(2, heightMapVisual.getHeightTexture());
            waterShader.setUniform(waterShader.locHeightMap, 2);
            waterShader.setUniform(waterShader.locWorldSize, (float) heightMap.getMetersPerWorld());

            waterShader.setUniformColor3(waterShader.locSkyColor, sky.getSkyColor());

            // Upload cloud parameters and textures for sky reflection
            waterShader.setUniform(waterShader.locInnerOffset, sky.getInnerOffset()[0], sky.getInnerOffset()[1]);
            waterShader.setUniform(waterShader.locOuterOffset, sky.getOuterOffset()[0], sky.getOuterOffset()[1]);

            context.setTexture(3, sky.getClouds()[0]);
            waterShader.setUniform(waterShader.locCloudTexture0, 3);
            context.setTexture(4, sky.getClouds()[1]);
            waterShader.setUniform(waterShader.locCloudTexture1, 4);

            context.setTexture(5, oceanMaskTexture);
            waterShader.setUniform(waterShader.locOceanMask, 5);

            // Render Sky Water (Infinite Plane)
            waterShader.setUniform(waterShader.locWaterHeight, 0.0f);
            GL20.glVertexAttrib3f(4, 0.0f, 0.0f, 1.0f);
            skyWaterVao.bind();
            sky.getWaterIndices().drawElements(GL11.GL_TRIANGLES, sky.getWaterIndices().capacity(), 0);
            skyWaterVao.unbind();

            // Render Instanced Water Patches. u_waterHeight = seaLevel.
            if (!visiblePatches.isEmpty()) {
                waterShader.setUniform(waterShader.locWaterHeight, heightMap.getSeaLevelMeters());
                instanceBuffer.clear();
                int count = 0;
                float patchSize = heightMap.getMetersPerPatch();
                int patchesPerWorld = heightMap.getPatchesPerWorld();

                for (LandscapeLeaf leaf : visiblePatches) {
                    int px = leaf.getPatchX();
                    int py = leaf.getPatchY();
                    if (heightMap.isBelowSeaLevel(px, py)) {
                        float worldX = px * patchSize;
                        float worldY = py * patchSize;
                        instanceBuffer = addInstance(instanceBuffer, worldX, worldY, 0.0f);
                        count++;
                    }
                }

                if (count > 0) {
                    instanceVBO = uploadAndDraw(context, count, instanceBuffer, instanceVBO);
                }
            }

            context.setActiveTexture(0);
        }
    }

    public void render(RenderContext context, CameraState state, Collection<
            LandscapeLeaf> visiblePatches) {
        render(context, state, visiblePatches, lastTime);
    }

    private void updateAnimation(float currentTime) {
        float dt = currentTime - lastTime;
        if (dt < 0 || dt > 1.0f) dt = 0.016f;
        lastTime = currentTime;
        waveTime += dt;

        var random = ThreadLocalRandom.current();
        timeSinceChange += dt;
        if (timeSinceChange > changeInterval) {
            timeSinceChange = 0f;

            float mean = 17.5f;
            float stdDev = 5.0f;
            float gaussianValue = (float) random.nextGaussian();
            changeInterval = mean + gaussianValue * stdDev;

            float dirChangeDegrees = (float) random.nextGaussian() * 7.5f;
            targetFlowDirection += (float) Math.toRadians(dirChangeDegrees);

            float speedChange = flowSpeed * (float) random.nextGaussian() * 0.05f;
            targetFlowSpeed = Math.clamp(targetFlowSpeed + speedChange, 0.0005f, 0.002f);
        }

        flowDirection += (targetFlowDirection - flowDirection) * dt * 0.5f;
        flowSpeed += (targetFlowSpeed - flowSpeed) * dt * 0.5f;

        float dx = (float) Math.cos(flowDirection) * flowSpeed * dt;
        float dy = (float) Math.sin(flowDirection) * flowSpeed * dt;

        scrollOffset0[0] += dx;
        scrollOffset0[1] += dy;

        // Move the second layer in a different direction (e.g., 90 degrees offset)
        // and slightly slower to create interference patterns.
        float flowDirection2 = flowDirection + (float) Math.toRadians(90f);
        float dx2 = (float) Math.cos(flowDirection2) * flowSpeed * 0.7f * dt;
        float dy2 = (float) Math.sin(flowDirection2) * flowSpeed * 0.7f * dt;

        scrollOffset1[0] += dx2;
        scrollOffset1[1] += dy2;
    }

    /**
     * Appends instance offsets to the buffer, resizing the buffer if necessary.
     */
    private FloatBuffer addInstance(FloatBuffer buffer, float x, float y, float z) {
        if (buffer.remaining() < 3) {
            int newCapacity = buffer.capacity() * 2;
            FloatBuffer newBuffer = BufferUtils.createFloatBuffer(newCapacity);
            buffer.flip();
            newBuffer.put(buffer);
            buffer = newBuffer;
        }
        buffer.put(x);
        buffer.put(y);
        buffer.put(z);
        return buffer;
    }

    /**
     * Uploads instance offset data and renders the instanced patches.
     */
    private FloatVBO uploadAndDraw(RenderContext context, int count, FloatBuffer buffer,
            FloatVBO vbo) {
        buffer.flip();

        int requiredFloats = count * 3;
        if (vbo.capacity() < requiredFloats) {
            vbo.close();
            //noinspection resource
            vbo = new FloatVBO(GL15.GL_STREAM_DRAW, Math.max(vbo.capacity() * 2, requiredFloats));
        }

        vbo.bind();
        GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, buffer);

        patchMesh.bind();

        // Setup instance attribute (Location 4: in_InstanceOffset)
        int offsetLoc = 4;
        GL20.glEnableVertexAttribArray(offsetLoc);
        GL20.glVertexAttribPointer(offsetLoc, 3, GL11.GL_FLOAT, false, 0, 0);
        GL33.glVertexAttribDivisor(offsetLoc, 1);

        patchMesh.drawInstanced(count);

        // Cleanup
        GL33.glVertexAttribDivisor(offsetLoc, 0);
        GL20.glDisableVertexAttribArray(offsetLoc);

        patchMesh.unbind();
        context.bindBuffer(GL15.GL_ARRAY_BUFFER, 0);

        return vbo;
    }

    public BitSet getOceanPatches() {
        return oceanPatches;
    }

    public Texture getOceanMaskTexture() {
        return oceanMaskTexture;
    }

    @Override
    public void putGlobalUniforms(java.nio.ByteBuffer buffer, boolean enableWaves) {
        // u_waveDirLength[3] (each element is a vec4 aligned to 16 bytes)
        for (int i = 0; i < WAVE_COUNT; i++) {
            buffer.putFloat(WAVE_DIRS_X[i]);
            buffer.putFloat(WAVE_DIRS_Y[i]);
            buffer.putFloat(waveLengths[i]);
            buffer.putFloat(0f); // pad
        }

        // u_waveAmpSteep[3] (each element is a vec4 aligned to 16 bytes)
        for (int i = 0; i < WAVE_COUNT; i++) {
            buffer.putFloat(enableWaves ? waveAmplitudes[i] : 0.0f);
            buffer.putFloat(waveSteepness[i]);
            buffer.putFloat(0f); // pad
            buffer.putFloat(0f); // pad
        }

        // u_scrollOffsets (vec4)
        buffer.putFloat(scrollOffset0[0]);
        buffer.putFloat(scrollOffset0[1]);
        buffer.putFloat(scrollOffset1[0]);
        buffer.putFloat(scrollOffset1[1]);

        // u_waveTime, u_waterRepeatRate, u_waterDetailRepeatRate, _pad2 (4 floats)
        buffer.putFloat(waveTime * waveSpeed);
        buffer.putFloat(LandscapeConfig.WATER_REPEAT_RATE);
        buffer.putFloat(LandscapeConfig.WATER_DETAIL_REPEAT_RATE);
        buffer.putFloat(0f); // _pad2
    }

    private record OceanTextures(Terrain terrain) implements Supplier<Texture[]> {
        @Override
        public Texture[] get() {
            int seed = LandscapeConfig.LANDSCAPE_SEED + 1;

            // water1
            Channel perlin2 = new Perlin(OCEAN_TEXTURE_SIZE, OCEAN_TEXTURE_SIZE, 4, 4, 0.5f, 1, seed,
                    Perlin.Interpolation.CUBIC, Perlin.Summation.NORMAL).toChannel();
            Channel perlin4 = new Perlin(OCEAN_TEXTURE_SIZE, OCEAN_TEXTURE_SIZE, 4, 4, 0.5f, 3, seed,
                    Perlin.Interpolation.CUBIC, Perlin.Summation.NORMAL).toChannel();
            Channel perlin32 = new Perlin(OCEAN_TEXTURE_SIZE, OCEAN_TEXTURE_SIZE, 32, 32, 0.5f, 2, seed,
                    Perlin.Interpolation.CUBIC, Perlin.Summation.NORMAL).toChannel();
            Channel perlin64 = new Perlin(OCEAN_TEXTURE_SIZE, OCEAN_TEXTURE_SIZE, 64, 64, 0.5f, 2, seed,
                    Perlin.Interpolation.CUBIC, Perlin.Summation.NORMAL).toChannel();
            Channel noise32 = perlin32.copy().abs().dynamicRange().gamma8().gamma2().invert().perturb(
                    perlin4.copy().rotate(90), 0.05f).perturb(perlin2, 0.05f);
            Channel noise64 = perlin64.copy().abs().dynamicRange().gamma2().invert().channelMultiply(
                    perlin32.copy().rotate(90).contrast(2f)).perturb(perlin4.copy().rotate(180), 0.05f).perturb(
                            perlin2.copy().rotate(90), 0.05f);
            Channel highlight = noise32.channelBrightest(noise64);
            Layer water1 = new Layer(new Channel(OCEAN_TEXTURE_SIZE, OCEAN_TEXTURE_SIZE),
                    perlin4.copy().dynamicRange(0.5f, 0.8f).rotate(180),
                    perlin4.copy().rotate(270).dynamicRange(0.6f, 0.9f));
            water1.layerAdd(highlight.multiply(0.2f).toLayer());
            water1.addAlpha();
            water1.a.fill(0.5f);
            if (terrain == Terrain.VIKING) {
                water1.multiply(0.4f);
                water1.a.addClip(0.1f);
            }

            // water2
            Channel voronoi12 = new Voronoi(OCEAN_TEXTURE_SIZE, 12, 12, 1, 1f, seed).getDistance(1f, 0f, 0f);
            Channel voronoi24 = new Voronoi(OCEAN_TEXTURE_SIZE, 24, 24, 1, 1f, seed).getDistance(1f, 0f, 0f);
            Channel perturb = new Midpoint(OCEAN_TEXTURE_SIZE, 4, 0.4f, 42).toChannel();
            voronoi12.channelAverage(voronoi24);
            voronoi12.perturb(perturb, 0.025f);
            Layer water2 = new Layer(voronoi12.copy(), voronoi12.copy(), voronoi12.copy(),
                    voronoi12.copy().gamma(1.5f));
            water2.bump(voronoi12, 3.5f, 0f, 0.5f, 0.5f, 0.8f, 1f, 0f, 0f, 0f);

            switch (terrain) {
                case NATIVE -> {
                    water2.r.dynamicRange(0f, 0.4f);
                    water2.g.dynamicRange(0.6f, 1f);
                    water2.b.dynamicRange(0.9f, 1f);
                }
                case VIKING -> {
                    water2.r.dynamicRange(0.5f, 1f);
                    water2.g.dynamicRange(0.7f, 1f);
                    water2.b.dynamicRange(0.8f, 1f);
                    water2.a.gamma(0.5f).dynamicRange(0f, 0.2f);
                }
                default -> throw new IllegalArgumentException("Illegal terrain: " + terrain);
            }

            Texture[] textures = new Texture[2];
            textures[0] = new Texture(new GLImage[]{new GLIntImage(water1)}, GL11.GL_RGBA8,
                    GL11.GL_LINEAR_MIPMAP_LINEAR, GL11.GL_LINEAR, GL11.GL_REPEAT, GL11.GL_REPEAT);
            textures[1] = new Texture(new GLImage[]{new GLIntImage(water2)}, GL11.GL_RGBA8,
                    GL11.GL_LINEAR_MIPMAP_LINEAR, GL11.GL_LINEAR, GL11.GL_REPEAT, GL11.GL_REPEAT);
            return textures;
        }
    }

    @Override
    public void close() {
        oceanMaskTexture.close();
        skyWaterVao.close();
        patchMesh.delete();
        instanceVBO.close();
        waterShader.close();
    }
}
