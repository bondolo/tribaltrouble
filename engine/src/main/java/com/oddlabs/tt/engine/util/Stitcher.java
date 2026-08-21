package com.oddlabs.tt.engine.util;

import org.lwjgl.BufferUtils;

import java.nio.ShortBuffer;
import java.util.Arrays;

public final class Stitcher {
    public static <V extends Vertex<V>> ShortBuffer stitch(V[] vertices)
            throws IllegalArgumentException {
        ShortBuffer indices = BufferUtils.createShortBuffer(vertices.length * 3);
        vertices = vertices.clone();
        Arrays.sort(vertices);
        int start_index = getStartIndex(vertices);
        V right_vertex = vertices[start_index];
        V left_vertex = vertices[(start_index + 1) % vertices.length];
        for (int i = 2; i < vertices.length + 2; i++) {
            V next_vertex = vertices[(i + start_index) % vertices.length];
            indices.put(right_vertex.index).put(left_vertex.index).put(next_vertex.index);
            if (next_vertex.side == right_vertex.side)
                right_vertex = next_vertex;
            else {
                assert next_vertex.side == left_vertex.side;
                left_vertex = next_vertex;
            }
        }
        assert !indices.hasRemaining();
        indices.flip();
        return indices;
    }

    private static <V extends Vertex<V>> int getStartIndex(V[] vertices)
            throws IllegalArgumentException {
        int vertex_index;
        for (vertex_index = 0; vertex_index < vertices.length; vertex_index++) {
            if (vertices[vertex_index % vertices.length].side >
                    vertices[(vertex_index + 1) % vertices.length].side)
                break;
        }
        if (vertex_index >= vertices.length)
            throw new IllegalArgumentException("All vertices are on one side");
        return vertex_index;
    }

    public abstract static class Vertex<V extends Vertex<V>> implements Comparable<V> {
        protected final int side;
        protected final short index;

        public Vertex(int index, int side) {
            this.index = (short) index;
            this.side = side;
        }

        public final short getIndex() {
            return index;
        }

        public final int getSide() {
            return side;
        }
    }

    private Stitcher() {
        // no instances
    }
}
