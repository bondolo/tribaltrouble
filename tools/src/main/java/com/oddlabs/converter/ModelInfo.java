package com.oddlabs.converter;

import org.jspecify.annotations.Nullable;

public record ModelInfo(short[] indices, float[] vertices, float[] normals,
                        float[] colors, float[] texcoords, float @Nullable [] texcoords2,
                        byte[][] skin_names, float[][] skin_weights) {
    //	public final String tex_name;

    public ModelInfo(/*String tex_name, */short[] indices, float[] vertices,
            float[] normals, float[] colors, float[] texcoords,
            byte[][] skin_names, float[][] skin_weights) {
        this(indices, vertices, normals, colors, texcoords, null, skin_names, skin_weights);
    }

    /*String tex_name, */
    //		this.tex_name = tex_name;
}
