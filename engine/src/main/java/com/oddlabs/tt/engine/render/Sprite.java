package com.oddlabs.tt.engine.render;


import com.oddlabs.geometry.SpriteInfo;
import com.oddlabs.tt.engine.procedural.GeneratorRespond;
import com.oddlabs.tt.engine.resource.Resources;
import com.oddlabs.tt.engine.resource.TextureFile;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.util.function.Supplier;

/**
 * Represents a single 3D animated sprite, including its textures, vertex data, and animation offsets.
 */
public final class Sprite {
    public static final int TEXTURE_NORMAL = 0;
    public static final int TEXTURE_TEAM = 1;
    public static final int TEXTURE_BUMP = 2;
    private static final String GENERATOR_STRING = "Generator:";

    public final Texture[][] textures;
    private final int num_triangles;
    private final int num_vertices;
    private final float @Nullable [] clear_color;
    public final boolean alpha;
    public final boolean lighted;
    public final boolean culled;
    public final boolean modulate_color;
    public final @Nullable Texture respond_texture;
    public final int indices_offset;
    public final int texcoords_offset;
    public final int vertices_offset;
    public final int normals_offset;
    public final int bone_indices_offset;
    public final int bone_weights_offset;

    /**
     * Dummy constructor for creating a simple quad sprite.
     */
    Sprite(int num_vertices, int num_triangles, int indices_offset, int texcoords_offset,
            int vertices_offset, int normals_offset, int bone_indices_offset, int bone_weights_offset,
            boolean modulate_color) {
        this.textures = new Texture[0][0];
        this.num_vertices = num_vertices;
        this.num_triangles = num_triangles;
        this.indices_offset = indices_offset;
        this.texcoords_offset = texcoords_offset;
        this.vertices_offset = vertices_offset;
        this.normals_offset = normals_offset;
        this.bone_indices_offset = bone_indices_offset;
        this.bone_weights_offset = bone_weights_offset;
        this.clear_color = new float[]{1f, 1f, 1f, 1f};
        this.alpha = true;
        this.lighted = false;
        this.culled = false;
        this.modulate_color = modulate_color;
        this.respond_texture = null;
    }

    public Sprite(SpriteInfo sprite_info, boolean alpha, boolean lighted, boolean culled,
            boolean modulate_color, boolean max_alpha, int mipmap_cutoff, int indices_offset,
            int texcoords_offset, int vertices_offset, int normals_offset, int bone_indices_offset,
            int bone_weights_offset) {
        this.culled = culled;
        this.alpha = alpha;
        this.lighted = lighted;
        this.modulate_color = modulate_color;
        this.indices_offset = indices_offset;
        this.texcoords_offset = texcoords_offset;
        this.vertices_offset = vertices_offset;
        this.normals_offset = normals_offset;
        this.bone_indices_offset = bone_indices_offset;
        this.bone_weights_offset = bone_weights_offset;

        short[] tmp_indices = sprite_info.getIndices();
        float[] tmp_texcoords = sprite_info.getTexCoords();
        num_vertices = tmp_texcoords.length / 2;
        num_triangles = tmp_indices.length / 3;
        clear_color = sprite_info.getClearColor();

        TextureFile.Format color_format = alpha ? TextureFile.Format.RGBA : TextureFile.Format.RGB;

        String[][] texture_names = sprite_info.getTextures();
        textures = new Texture[texture_names.length][3];
        for (int i = 0; i < texture_names.length; i++) {
            Texture[] diffuseAndBump = getTextureForName(texture_names[i][0], color_format, mipmap_cutoff, max_alpha);
            textures[i][TEXTURE_NORMAL] = diffuseAndBump[0];
            if (diffuseAndBump.length > 1) {
                textures[i][TEXTURE_BUMP] = diffuseAndBump[1];
            }

            textures[i][TEXTURE_TEAM] = texture_names[i][TEXTURE_TEAM] != null
                    ? getTextureForName(texture_names[i][1], TextureFile.Format.RGB, mipmap_cutoff,
                            max_alpha)[0]
                    : null;
        }
        this.respond_texture = Resources.findResource(new GeneratorRespond())[0];
    }

    public boolean modulateColor() {
        return modulate_color;
    }

    public int getTriangleCount() {
        return num_triangles;
    }

    private static Texture[] getTextureForName(String texture_name, TextureFile.Format color_format,
            int mipmap_cutoff, boolean max_alpha) {
        if (texture_name.startsWith(GENERATOR_STRING)) {
            String generator_class_name = texture_name.substring(GENERATOR_STRING.length());
            try {
                Class<?> generator_class;
                try {
                    generator_class = Class.forName(generator_class_name);
                } catch (ClassNotFoundException e) {
                    if (generator_class_name.startsWith("com.oddlabs.tt.procedural.")) {
                        String fallback_class_name = "com.oddlabs.tt.engine.procedural."
                                + generator_class_name.substring("com.oddlabs.tt.procedural.".length());
                        generator_class = Class.forName(fallback_class_name);
                    } else {
                        throw e;
                    }
                }
                @SuppressWarnings("unchecked") Supplier<Texture[]> descriptor = (Supplier<Texture[]>) generator_class
                        .getDeclaredConstructor().newInstance();
                return Resources.findResource(descriptor);
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | NoSuchMethodException
                     | InvocationTargetException e) {
                throw new IllegalStateException("Failed to instantiate texture generator: " + generator_class_name, e);
            }
        } else {
            String lowerName = texture_name.toLowerCase();
            boolean clampEdges = lowerName.contains("leaf") || lowerName.contains("plant") || lowerName.contains(
                    "crown")
                    || lowerName.contains("branch") || lowerName.contains("foliage") || lowerName.contains("bush");
            boolean isData = lowerName.contains("normal") || lowerName.contains("bump") || lowerName.contains("mica")
                    || lowerName.contains("team");
            return new Texture[]{Resources.findResource(TextureFile.forModel("/textures/models/" + texture_name,
                    color_format, clampEdges, mipmap_cutoff, max_alpha, isData))};
        }
    }

    public boolean hasTeamDecal() {
        return textures.length > 0 && textures[0].length > TEXTURE_TEAM && textures[0][TEXTURE_TEAM] != null;
    }

    public boolean hasBumpMap(int tex_index) {
        return textures.length > tex_index && textures[tex_index].length > TEXTURE_BUMP
                && textures[tex_index][TEXTURE_BUMP] != null;
    }

    public int getNumTextures() {
        return textures.length;
    }

    public int getNumVertices() {
        return num_vertices;
    }

    public float[] getClearColor() {
        return clear_color;
    }
}
