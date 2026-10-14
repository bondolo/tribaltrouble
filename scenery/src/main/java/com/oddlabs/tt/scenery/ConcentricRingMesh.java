package com.oddlabs.tt.scenery;

import com.oddlabs.tt.engine.util.Stitcher;
import com.oddlabs.tt.engine.vbo.FloatVBO;
import com.oddlabs.tt.engine.vbo.ShortVBO;
import com.oddlabs.tt.simulation.landscape.HeightMap;
import com.oddlabs.tt.simulation.landscape.LandscapeEnvironment;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL15;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Concentric outer ring mesh connecting the island perimeter to the distant horizon.
 */
public final class ConcentricRingMesh implements AutoCloseable {
    private static final int NUM_WATER_RINGS = 6;
    private static final float START_ANGLE = (float) (Math.PI / 4.0);

    private final FloatVBO waterVertices;
    private final FloatVBO bottomVertices;
    private final ShortVBO indices;

    public ConcentricRingMesh(LandscapeEnvironment heightMap, float radius, int subdivAxis) {
        float innerRadius = (float) (heightMap.getMetersPerWorld() * Math.sqrt(2) / 2);
        float originX = heightMap.getMetersPerWorld() / 2f;
        float originY = heightMap.getMetersPerWorld() / 2f;

        List<RingStitchVertex[]> verticesStitchList = new ArrayList<>();
        List<ShortBuffer> stitchIndicesList = new ArrayList<>();
        int numVertices = 0;
        int numIndices = 0;

        RingStitchVertex[] previousVertices = makeLandscapeVertices(heightMap);
        verticesStitchList.add(previousVertices);
        numVertices += previousVertices.length;

        for (int i = 0; i < NUM_WATER_RINGS; i++) {
            float radiusFactor = (float) (i + 1) / NUM_WATER_RINGS;
            float ringRadius = innerRadius + (radius - innerRadius) * radiusFactor * radiusFactor;
            RingStitchVertex[] ringVertices = makeDomeVertices(heightMap, subdivAxis, i + 1,
                    numVertices, ringRadius, originX, originY);
            verticesStitchList.add(ringVertices);
            numVertices += ringVertices.length;

            RingStitchVertex[] stitchVertices = new RingStitchVertex[ringVertices.length + previousVertices.length];
            System.arraycopy(previousVertices, 0, stitchVertices, 0, previousVertices.length);
            System.arraycopy(ringVertices, 0, stitchVertices, previousVertices.length, ringVertices.length);

            ShortBuffer stitchIndices = Stitcher.stitch(stitchVertices);
            stitchIndicesList.add(stitchIndices);
            numIndices += stitchIndices.remaining();
            previousVertices = ringVertices;
        }

        RingStitchVertex[] allVertices = new RingStitchVertex[numVertices];
        int index = 0;
        for (RingStitchVertex[] vertices : verticesStitchList) {
            System.arraycopy(vertices, 0, allVertices, index, vertices.length);
            index += vertices.length;
        }

        ShortBuffer allIndices = BufferUtils.createShortBuffer(numIndices);
        for (ShortBuffer ind : stitchIndicesList) {
            allIndices.put(ind);
        }
        allIndices.flip();

        this.indices = new ShortVBO(GL15.GL_STATIC_DRAW, allIndices);
        this.waterVertices = toVBO(allVertices, heightMap.getSeaLevelMeters());
        this.bottomVertices = toVBO(allVertices, 0.0f);
    }

    private static FloatVBO toVBO(RingStitchVertex[] vertices, float height) {
        FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(vertices.length * 3);
        for (int i = 0; i < vertices.length; i++) {
            RingStitchVertex vertex = vertices[i];
            assert vertex.getIndex() == i : vertex.getIndex() + " " + i;
            float x = vertex.x;
            float y = vertex.y;
            float z = height;
            vertexBuffer.put(x).put(y).put(z);
        }
        vertexBuffer.flip();
        return new FloatVBO(GL15.GL_STATIC_DRAW, vertexBuffer);
    }

    private static RingStitchVertex[] makeDomeVertices(LandscapeEnvironment heightmap, int subdivAxis, int ringId,
            int indexOffset, float ringRadius, float originX, float originY) {
        float angleInc = (float) Math.PI * 2 / subdivAxis;
        return IntStream.range(0, subdivAxis)
                .mapToObj(i -> {
                    int index = i + indexOffset;
                    return new RingStitchVertex(heightmap, index, ringId,
                            (float) Math.cos(START_ANGLE + angleInc * i) * ringRadius + originX,
                            (float) Math.sin(START_ANGLE + angleInc * i) * ringRadius + originY);
                }).toArray(RingStitchVertex[]::new);
    }

    private static RingStitchVertex[] makeLandscapeVertices(LandscapeEnvironment heightmap) {
        int gridUnitsPerWorld = heightmap.getGridUnitsPerWorld();
        int size = 4 * gridUnitsPerWorld;
        RingStitchVertex[] result = new RingStitchVertex[size];

        int metersPerUnit = HeightMap.METERS_PER_UNIT_GRID;
        int metersPerWorld = heightmap.getMetersPerWorld();

        for (int i = 0; i < gridUnitsPerWorld; i++) {
            int index = i;
            result[index] = new RingStitchVertex(heightmap, index, 0, 0, i * metersPerUnit);

            index = i + gridUnitsPerWorld;
            result[index] = new RingStitchVertex(heightmap, index, 0, i * metersPerUnit, metersPerWorld);

            index = i + gridUnitsPerWorld * 2;
            result[index] = new RingStitchVertex(heightmap, index, 0, metersPerWorld, metersPerWorld - i
                    * metersPerUnit);

            index = i + gridUnitsPerWorld * 3;
            result[index] = new RingStitchVertex(heightmap, index, 0, metersPerWorld - i * metersPerUnit, 0);
        }
        return result;
    }

    public FloatVBO waterVertices() {
        return waterVertices;
    }

    public FloatVBO bottomVertices() {
        return bottomVertices;
    }

    public ShortVBO indices() {
        return indices;
    }

    @Override
    public void close() {
        waterVertices.close();
        bottomVertices.close();
        indices.close();
    }

    private static final class RingStitchVertex extends Stitcher.Vertex<RingStitchVertex> {
        private final float x;
        private final float y;
        private final float theta;

        private RingStitchVertex(LandscapeEnvironment heightmap, int index, int side, float x, float y) {
            super(index, side);
            this.x = x;
            this.y = y;
            float halfWorldSize = heightmap.getMetersPerWorld() * 0.5f;
            this.theta = (float) Math.atan2(y - halfWorldSize, x - halfWorldSize);
        }

        @Override
        public int compareTo(RingStitchVertex other) {
            if (other == this) {
                return 0;
            }
            int cmp = Float.compare(other.theta, theta);
            if (cmp != 0) {
                return cmp;
            }
            return Integer.compare(side, other.side);
        }
    }
}
