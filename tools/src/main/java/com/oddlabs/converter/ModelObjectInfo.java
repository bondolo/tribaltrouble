package com.oddlabs.converter;


import java.nio.file.Path;

public final class ModelObjectInfo extends ObjectInfo {
    private final float[] clear_color;
    private final String[][] textures;

    public ModelObjectInfo(Path file, String[][] textures,
            float[] clear_color) {
        super(file);
        this.textures = textures;
        this.clear_color = clear_color;
    }

    public String[][] getTextures() {
        return textures;
    }

    public float[] getClearColor() {
        return clear_color;
    }
}
