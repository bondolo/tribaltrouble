package com.oddlabs.tt.engine.render;


import com.oddlabs.geometry.AnimationInfo;
import com.oddlabs.geometry.SpriteInfo;
import com.oddlabs.tt.base.geom.BoundingBox;
import com.oddlabs.tt.engine.resource.SpriteFile;
import com.oddlabs.tt.engine.vbo.FloatVBO;
import com.oddlabs.tt.engine.vbo.ShortVBO;
import com.oddlabs.tt.engine.vbo.VertexArray;
import com.oddlabs.util.Utils;
import org.jspecify.annotations.Nullable;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;
import java.util.stream.Stream;

/**
 * Manages a collection of 3D sprites and their associated shared OpenGL resources,
 * including index buffers, vertex attributes, and TBO textures.
 */
public final class SpriteList implements AutoCloseable {
    private static final SpriteList QUAD_INSTANCE = new SpriteList(new float[]{0, 0, 1, 0, 1, 1, 0, 1});

    private final BoundingBox[] bounds;
    private final Sprite[] sprites;
    private final AnimationInfo.AnimationType[] type_array;
    private final String[] animation_names;

    private final ShortVBO indices;
    private final FloatVBO vertices_and_normals;
    private final FloatVBO texcoords;
    private @Nullable VertexArray vao;
    private int tboTextureHandle;

    public static SpriteList getQuadInstance() {
        return QUAD_INSTANCE;
    }

    public static SpriteList createQuadInstance(float u1, float v1, float u2, float v2) {
        return new SpriteList(new float[]{u1, v1, u2, v1, u2, v2, u1, v2});
    }

    private SpriteList(float[] quad_texcoords) {
        // Private constructor for the quad instance
        this.bounds = new BoundingBox[]{new BoundingBox()};
        this.type_array = new AnimationInfo.AnimationType[]{AnimationInfo.AnimationType.LOOP};
        this.animation_names = new String[]{"default"};

        float[] quad_vertices = {
                -0.5f, -0.5f, 0f,
                0.5f, -0.5f, 0f,
                0.5f, 0.5f, 0f,
                -0.5f, 0.5f, 0f
        };
        float[] quad_normals = {0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1};
        short[] quad_indices = {0, 1, 2, 0, 2, 3};

        FloatBuffer vertAndNormBuf = BufferUtils.createFloatBuffer(quad_vertices.length + quad_normals.length);
        vertAndNormBuf.put(quad_vertices);
        vertAndNormBuf.put(quad_normals);
        vertAndNormBuf.flip();
        this.vertices_and_normals = new FloatVBO(GL15.GL_STATIC_DRAW, vertAndNormBuf);

        FloatBuffer texCoordBuf = BufferUtils.createFloatBuffer(quad_texcoords.length).put(quad_texcoords);
        texCoordBuf.flip();
        this.texcoords = new FloatVBO(GL15.GL_STATIC_DRAW, texCoordBuf);

        ShortBuffer indexBuf = BufferUtils.createShortBuffer(quad_indices.length).put(quad_indices);
        indexBuf.flip();
        this.indices = new ShortVBO(GL15.GL_STATIC_DRAW, indexBuf);

        this.sprites = new Sprite[]{new Sprite(4, 2, 0, true)};

        initTBO();
    }

    public SpriteList(SpriteFile sprite_file) {
        Object[] sprites_and_animations = Utils.loadObject(Object[].class, sprite_file.getURL());
        SpriteInfo[] sprite_infos = (SpriteInfo[]) sprites_and_animations[0];
        AnimationInfo[] animation_infos = (AnimationInfo[]) sprites_and_animations[1];
        bounds = Stream.generate(BoundingBox::new).limit(animation_infos.length).toArray(BoundingBox[]::new);

        int total_indices = 0;
        int total_vertices = 0;
        for (SpriteInfo sprite_info : sprite_infos) {
            total_indices += sprite_info.getIndices().length;
            total_vertices += sprite_info.getTexCoords().length / 2;
        }

        ShortBuffer all_indices = BufferUtils.createShortBuffer(total_indices);
        FloatBuffer all_texcoords = BufferUtils.createFloatBuffer(total_vertices * 2);

        int vert_and_normal_buffer_size = 0;
        for (SpriteInfo sprite_info : sprite_infos) {
            int num_vertices = sprite_info.getTexCoords().length / 2;
            int frame_size = num_vertices * 3 * 2; // pos(3) + norm(3)
            for (AnimationInfo animationInfo : animation_infos) {
                int num_frames = animationInfo.getFrames().length;
                vert_and_normal_buffer_size += num_frames * frame_size;
            }
        }

        FloatBuffer all_vertices_and_normals = BufferUtils.createFloatBuffer(
                vert_and_normal_buffer_size);

        float[] cpw_array = new float[animation_infos.length];
        type_array = new AnimationInfo.AnimationType[animation_infos.length];
        animation_names = new String[animation_infos.length];
        int[] animation_length_array = new int[animation_infos.length];
        for (int i = 0; i < animation_infos.length; i++) {
            cpw_array[i] = 1f / animation_infos[i].getWPC();
            type_array[i] = animation_infos[i].getType();
            animation_names[i] = animation_infos[i].getName();
            animation_length_array[i] = animation_infos[i].getFrames().length;
        }
        sprites = Arrays.stream(sprite_infos)
                .map(info -> new Sprite(info, animation_infos,
                        sprite_file.hasAlpha(), sprite_file.isLighted(), sprite_file.isCulled(),
                        sprite_file.hasModulateColor(), sprite_file.hasMaxAlpha(), sprite_file.getMipmapCutoff(),
                        bounds, cpw_array, type_array, animation_length_array,
                        all_indices, all_texcoords, all_vertices_and_normals)
                ).toArray(Sprite[]::new);

        all_indices.flip();
        indices = new ShortVBO(GL15.GL_STATIC_DRAW, all_indices.remaining());
        indices.put(all_indices);

        all_texcoords.flip();
        texcoords = new FloatVBO(GL15.GL_STATIC_DRAW, all_texcoords.remaining());
        texcoords.put(all_texcoords);

        all_vertices_and_normals.flip();
        vertices_and_normals = new FloatVBO(GL15.GL_STATIC_DRAW, all_vertices_and_normals.remaining());
        vertices_and_normals.put(all_vertices_and_normals);

        for (BoundingBox bound : bounds) {
            bound.maximizeXYPlane();
        }

        initTBO();
    }

    private void initTBO() {
        tboTextureHandle = org.lwjgl.opengl.GL11.glGenTextures();
        org.lwjgl.opengl.GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, tboTextureHandle);
        GL31.glTexBuffer(GL31.GL_TEXTURE_BUFFER, GL30.GL_RGB32F, vertices_and_normals.getHandle());
    }

    int getTBOTextureHandle() {
        return tboTextureHandle;
    }

    public float[] getClearColor() {
        return getSprite(0).getClearColor();
    }

    public BoundingBox[] getBounds() {
        return bounds;
    }

    public int getNumSprites() {
        return sprites.length;
    }

    public Sprite getSprite(int index) {
        return sprites[index];
    }

    public AnimationInfo.AnimationType[] getAnimationTypes() {
        return type_array;
    }

    public String[] getAnimationNames() {
        return animation_names;
    }

    public int getAnimationIndex(String name) {
        for (int i = 0; i < animation_names.length; i++) {
            if (animation_names[i].equals(name)) {
                return i;
            }
        }
        return -1;
    }

    public ShortVBO getIndices() {
        return indices;
    }

    public FloatVBO getVerticesAndNormals() {
        return vertices_and_normals;
    }

    public FloatVBO getTexcoords() {
        return texcoords;
    }

    @Override
    public void close() {
        if (tboTextureHandle != 0) {
            org.lwjgl.opengl.GL11.glDeleteTextures(tboTextureHandle);
            tboTextureHandle = 0;
        }
        if (vao != null) {
            vao.close();
            vao = null;
        }
        indices.close();
        vertices_and_normals.close();
        texcoords.close();
    }
}
