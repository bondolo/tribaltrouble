package com.oddlabs.tt.engine.render;


import com.oddlabs.geometry.AnimationInfo;
import com.oddlabs.geometry.BoundsData;
import com.oddlabs.geometry.SkeletonData;
import com.oddlabs.geometry.SpriteInfo;
import com.oddlabs.tt.base.geom.BoundingBox;
import com.oddlabs.tt.engine.resource.SpriteFile;
import com.oddlabs.tt.engine.vbo.FloatVBO;
import com.oddlabs.tt.engine.vbo.ShortVBO;
import com.oddlabs.tt.engine.vbo.VertexArray;
import com.oddlabs.util.Utils;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;

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
    private final AnimationInfo @Nullable [] animation_infos;
    private final @Nullable SkeletonData skeleton_data;
    private final Matrix4f @Nullable [] initial_pose_matrices;
    private final float @Nullable [] cpw_array;
    private final int @Nullable [] animation_length_array;

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
        this.animation_infos = null;
        this.skeleton_data = null;
        this.initial_pose_matrices = null;
        this.cpw_array = null;
        this.animation_length_array = null;

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
        this.animation_infos = animation_infos;
        this.skeleton_data = sprites_and_animations.length > 2 && sprites_and_animations[2] instanceof SkeletonData sd
                ? sd : null;
        if (this.skeleton_data != null) {
            String[] boneNames = this.skeleton_data.boneNames();
            this.initial_pose_matrices = new Matrix4f[boneNames.length];
            for (int i = 0; i < boneNames.length; i++) {
                float[] ipd = this.skeleton_data.getInitialPose(i);
                if (ipd != null) {
                    this.initial_pose_matrices[i] = new Matrix4f().set(ipd);
                }
            }
        } else {
            this.initial_pose_matrices = null;
        }
        BoundsData[] bounds_data = (BoundsData[]) sprites_and_animations[3];
        this.bounds = new BoundingBox[bounds_data.length];
        for (int i = 0; i < bounds_data.length; i++) {
            BoundsData bd = bounds_data[i];
            this.bounds[i] = new BoundingBox(bd.minX(), bd.maxX(), bd.minY(), bd.maxY(), bd.minZ(), bd.maxZ());
        }

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

        this.cpw_array = new float[animation_infos.length];
        type_array = new AnimationInfo.AnimationType[animation_infos.length];
        animation_names = new String[animation_infos.length];
        this.animation_length_array = new int[animation_infos.length];
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

    /**
     * Checks if this sprite list has a skeleton with the specified bone or socket name.
     *
     * @param socketName the socket or bone name
     * @return true if the socket is present
     */
    public boolean hasSocket(String socketName) {
        return skeleton_data != null && skeleton_data.hasBone(socketName);
    }

    /**
     * Resolves the bone index for a named socket.
     *
     * @param socketName the socket or bone name
     * @return the bone index, or -1 if not found
     */
    public int getSocketIndex(String socketName) {
        return skeleton_data != null ? skeleton_data.findBoneIndex(socketName) : -1;
    }

    /**
     * Resolves the animated transform for a bone socket in sprite model space by socket name.
     *
     * @param socketName the socket or bone name
     * @param animationIndex the animation index
     * @param animTicks the animation ticks
     * @param dest the matrix to receive the result
     * @return true if successfully resolved, false if socket or animation not found
     */
    public boolean getSocketTransform(String socketName, int animationIndex, float animTicks, Matrix4f dest) {
        int boneIndex = getSocketIndex(socketName);
        return getSocketTransform(boneIndex, animationIndex, animTicks, dest);
    }

    /**
     * Resolves the animated transform for a bone socket in sprite model space by bone index.
     *
     * @param boneIndex the bone index
     * @param animationIndex the animation index
     * @param animTicks the animation ticks
     * @param dest the matrix to receive the result
     * @return true if successfully resolved, false if socket or animation not found
     */
    public boolean getSocketTransform(int boneIndex, int animationIndex, float animTicks, Matrix4f dest) {
        if (initial_pose_matrices == null || animation_infos == null || cpw_array == null
                || animation_length_array == null) {
            return false;
        }
        if (boneIndex < 0 || boneIndex >= initial_pose_matrices.length || animationIndex < 0
                || animationIndex >= animation_infos.length) {
            return false;
        }
        Matrix4f initPose = initial_pose_matrices[boneIndex];
        if (initPose == null) {
            return false;
        }

        AnimationInfo animInfo = animation_infos[animationIndex];
        float anim_position = animTicks * cpw_array[animationIndex];
        int len = animation_length_array[animationIndex];
        float exactFrame = anim_position * len;

        int frame1 = (int) exactFrame;
        int frame2 = frame1 + 1;
        float tween = exactFrame - frame1;

        if (type_array[animationIndex] == AnimationInfo.AnimationType.LOOP) {
            frame1 %= len;
            frame2 %= len;
        } else {
            frame1 = Math.min(frame1, len - 1);
            frame2 = Math.min(frame2, len - 1);
        }

        float[] frame1Data = animInfo.getFrames()[frame1];
        float[] frame2Data = animInfo.getFrames()[frame2];
        int offset = boneIndex * 12;

        float t0 = 1.0f - tween;
        dest.set(
                frame1Data[offset + 0] * t0 + frame2Data[offset + 0] * tween,
                frame1Data[offset + 4] * t0 + frame2Data[offset + 4] * tween,
                frame1Data[offset + 8] * t0 + frame2Data[offset + 8] * tween,
                0.0f,
                frame1Data[offset + 1] * t0 + frame2Data[offset + 1] * tween,
                frame1Data[offset + 5] * t0 + frame2Data[offset + 5] * tween,
                frame1Data[offset + 9] * t0 + frame2Data[offset + 9] * tween,
                0.0f,
                frame1Data[offset + 2] * t0 + frame2Data[offset + 2] * tween,
                frame1Data[offset + 6] * t0 + frame2Data[offset + 6] * tween,
                frame1Data[offset + 10] * t0 + frame2Data[offset + 10] * tween,
                0.0f,
                frame1Data[offset + 3] * t0 + frame2Data[offset + 3] * tween,
                frame1Data[offset + 7] * t0 + frame2Data[offset + 7] * tween,
                frame1Data[offset + 11] * t0 + frame2Data[offset + 11] * tween,
                1.0f
        );

        dest.mul(initPose);
        return true;
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
