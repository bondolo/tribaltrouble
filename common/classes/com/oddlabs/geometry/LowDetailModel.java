package com.oddlabs.geometry;

import java.io.*;

public final strictfp class LowDetailModel implements Serializable {
	private final static long serialVersionUID = 1;

	private final float[] vertices;
	private final float[] tex_coords;
	private final short[] indices;
	private final int poly_count;

	public LowDetailModel(short[] indices, float[] vertices, float[] tex_coords) {
		this.indices = indices;
		this.vertices = vertices;
		this.tex_coords = tex_coords;
		this.poly_count = indices.length/3;
	}

    @SuppressWarnings("ReturnOfCollectionOrArrayField")
	public short[] getIndices() {
		return indices;
	}

    @SuppressWarnings("ReturnOfCollectionOrArrayField")
	public float[] getVertices() {
		return vertices;
	}

    @SuppressWarnings("ReturnOfCollectionOrArrayField")
	public float[] getTexCoords() {
		return tex_coords;
	}

	public int getPolyCount() {
		return poly_count;
	}
}
