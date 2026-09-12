package com.oddlabs.tt.engine.render;


import com.oddlabs.geometry.AnimationInfo;
import com.oddlabs.geometry.BoundsData;
import com.oddlabs.geometry.SkeletonData;
import com.oddlabs.geometry.SpriteInfo;
import com.oddlabs.tt.base.geom.BoundingBox;
import com.oddlabs.tt.engine.resource.SpriteFile;
import com.oddlabs.tt.engine.vbo.ByteVBO;
import com.oddlabs.tt.engine.vbo.FloatVBO;
import com.oddlabs.tt.engine.vbo.ShortVBO;
import com.oddlabs.tt.engine.vbo.VertexArray;
import com.oddlabs.util.Utils;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL15;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * Manages a collection of 3D sprites and their associated shared OpenGL resources,
 * including index buffers, vertex attributes, and skeletal VBOs.
 */
public final class SpriteList implements AutoCloseable {
    private static final SpriteList QUAD_INSTANCE = new SpriteList(new float[]{0, 0, 1, 0, 1, 1, 0, 1});

    private final BoundingBox[] bounds;
    private final Sprite[] sprites;
    private final AnimationInfo.AnimationType[] type_array;
    private final String[] animation_names;
    private final AnimationInfo @Nullable [] animation_infos;
    private final @Nullable SkeletonData skeleton_data;
    private final Matrix4fc @Nullable [] initial_pose_matrices;
    private final float @Nullable [] cpw_array;
    private final int @Nullable [] animation_length_array;

    private final ShortVBO indices;
    private final FloatVBO positions;
    private final FloatVBO normals;
    private final FloatVBO texcoords;
    private final @Nullable ByteVBO bone_indices;
    private final @Nullable FloatVBO bone_weights;
    private @Nullable VertexArray vao;

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

        FloatBuffer posBuf = BufferUtils.createFloatBuffer(quad_vertices.length).put(quad_vertices);
        posBuf.flip();
        this.positions = new FloatVBO(GL15.GL_STATIC_DRAW, posBuf);

        FloatBuffer normBuf = BufferUtils.createFloatBuffer(quad_normals.length).put(quad_normals);
        normBuf.flip();
        this.normals = new FloatVBO(GL15.GL_STATIC_DRAW, normBuf);

        FloatBuffer texCoordBuf = BufferUtils.createFloatBuffer(quad_texcoords.length).put(quad_texcoords);
        texCoordBuf.flip();
        this.texcoords = new FloatVBO(GL15.GL_STATIC_DRAW, texCoordBuf);

        ShortBuffer indexBuf = BufferUtils.createShortBuffer(quad_indices.length).put(quad_indices);
        indexBuf.flip();
        this.indices = new ShortVBO(GL15.GL_STATIC_DRAW, indexBuf);

        this.bone_indices = null;
        this.bone_weights = null;

        this.sprites = new Sprite[]{new Sprite(4, 2, 0, 0, 0, 0, 0, 0, true)};
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
            this.initial_pose_matrices = new Matrix4fc[boneNames.length];
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
        FloatBuffer all_positions = BufferUtils.createFloatBuffer(total_vertices * 3);
        FloatBuffer all_normals = BufferUtils.createFloatBuffer(total_vertices * 3);
        FloatBuffer all_texcoords = BufferUtils.createFloatBuffer(total_vertices * 2);
        ByteBuffer all_bone_indices = this.skeleton_data != null
                ? BufferUtils.createByteBuffer(total_vertices * 4) : null;
        FloatBuffer all_bone_weights = this.skeleton_data != null
                ? BufferUtils.createFloatBuffer(total_vertices * 4) : null;

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

        sprites = new Sprite[sprite_infos.length];
        for (int s = 0; s < sprite_infos.length; s++) {
            SpriteInfo info = sprite_infos[s];
            int idxOffset = all_indices.position();
            int vertOffset = all_positions.position();
            int normOffset = all_normals.position();
            int texOffset = all_texcoords.position();
            int boneIdxOffset = all_bone_indices != null ? all_bone_indices.position() : 0;
            int boneWeightOffset = all_bone_weights != null ? all_bone_weights.position() : 0;

            all_indices.put(info.getIndices());
            all_positions.put(info.getVertices());
            all_normals.put(info.getNormals());
            all_texcoords.put(info.getTexCoords());

            if (all_bone_indices != null && all_bone_weights != null) {
                byte[][] skinNames = info.getSkinNames();
                float[][] skinWeights = info.getSkinWeights();
                int vCount = info.getTexCoords().length / 2;
                for (int v = 0; v < vCount; v++) {
                    byte[] bones = (skinNames != null && v < skinNames.length) ? skinNames[v] : null;
                    float[] weights = (skinWeights != null && v < skinWeights.length) ? skinWeights[v] : null;
                    int bLen = bones != null ? bones.length : 0;
                    for (int i = 0; i < 4; i++) {
                        if (i < bLen) {
                            all_bone_indices.put(bones[i]);
                            all_bone_weights.put(weights[i]);
                        } else {
                            all_bone_indices.put((byte) 0);
                            all_bone_weights.put(0.0f);
                        }
                    }
                }
            }

            sprites[s] = new Sprite(info, sprite_file.hasAlpha(), sprite_file.isLighted(),
                    sprite_file.isCulled(), sprite_file.hasModulateColor(), sprite_file.hasMaxAlpha(),
                    sprite_file.getMipmapCutoff(), idxOffset, texOffset, vertOffset, normOffset,
                    boneIdxOffset, boneWeightOffset);
        }

        all_indices.flip();
        indices = new ShortVBO(GL15.GL_STATIC_DRAW, all_indices);

        all_positions.flip();
        positions = new FloatVBO(GL15.GL_STATIC_DRAW, all_positions);

        all_normals.flip();
        normals = new FloatVBO(GL15.GL_STATIC_DRAW, all_normals);

        all_texcoords.flip();
        texcoords = new FloatVBO(GL15.GL_STATIC_DRAW, all_texcoords);

        if (all_bone_indices != null && all_bone_weights != null) {
            all_bone_indices.flip();
            bone_indices = new ByteVBO(GL15.GL_STATIC_DRAW, all_bone_indices);

            all_bone_weights.flip();
            bone_weights = new FloatVBO(GL15.GL_STATIC_DRAW, all_bone_weights);
        } else {
            bone_indices = null;
            bone_weights = null;
        }

        for (BoundingBox bound : bounds) {
            bound.maximizeXYPlane();
        }
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

    public FloatVBO getPositions() {
        return positions;
    }

    public FloatVBO getNormals() {
        return normals;
    }

    public FloatVBO getTexcoords() {
        return texcoords;
    }

    public @Nullable ByteVBO getBoneIndices() {
        return bone_indices;
    }

    public @Nullable FloatVBO getBoneWeights() {
        return bone_weights;
    }

    public boolean isSkeletal() {
        return skeleton_data != null && getBoneCount() > 0;
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
     * Returns the number of bones in this sprite list's skeleton, or 0 if non-skeletal.
     *
     * @return the number of bones
     */
    public int getBoneCount() {
        return skeleton_data != null ? skeleton_data.boneNames().length : 0;
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
     * Evaluates all bone skinning matrices for the given animation and frame ticks into the destination array.
     *
     * @param animationIndex the animation index
     * @param animTicks the animation ticks
     * @param dest the array of matrices to receive evaluated bone transforms
     * @return true if successfully evaluated, false if animation or skeleton data is missing
     */
    public boolean evaluateSkeleton(int animationIndex, float animTicks, Matrix4f[] dest) {
        if (initial_pose_matrices == null || animation_infos == null || cpw_array == null
                || animation_length_array == null) {
            return false;
        }
        if (animationIndex < 0 || animationIndex >= animation_infos.length) {
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
        float t0 = 1.0f - tween;
        int numBones = Math.min(dest.length, initial_pose_matrices.length);

        for (int bone = 0; bone < numBones; bone++) {
            int offset = bone * 12;
            dest[bone].set(
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
        }
        return true;
    }

    /**
     * Resolves the animated transform for a bone socket in sprite model space from pre-evaluated bone matrices.
     *
     * @param boneIndex the bone index
     * @param evaluatedBones the pre-evaluated bone skinning matrices
     * @param dest the matrix to receive the result
     * @return true if successfully resolved, false if socket bone not found
     */
    public boolean getSocketTransform(int boneIndex, Matrix4fc[] evaluatedBones, Matrix4f dest) {
        if (initial_pose_matrices == null || boneIndex < 0 || boneIndex >= initial_pose_matrices.length
                || boneIndex >= evaluatedBones.length) {
            return false;
        }
        Matrix4fc initPose = initial_pose_matrices[boneIndex];
        if (initPose == null) {
            return false;
        }
        dest.set(evaluatedBones[boneIndex]).mul(initPose);
        return true;
    }

    /**
     * Resolves the animated transform for a bone socket in sprite model space from pre-evaluated bone matrices by
     * socket name.
     *
     * @param socketName the socket or bone name
     * @param evaluatedBones the pre-evaluated bone skinning matrices
     * @param dest the matrix to receive the result
     * @return true if successfully resolved, false if socket not found
     */
    public boolean getSocketTransform(String socketName, Matrix4fc[] evaluatedBones, Matrix4f dest) {
        int boneIndex = getSocketIndex(socketName);
        return getSocketTransform(boneIndex, evaluatedBones, dest);
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
        Matrix4fc initPose = initial_pose_matrices[boneIndex];
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
        if (vao != null) {
            vao.close();
            vao = null;
        }
        indices.close();
        positions.close();
        normals.close();
        texcoords.close();
        if (bone_indices != null) {
            bone_indices.close();
        }
        if (bone_weights != null) {
            bone_weights.close();
        }
    }
}
