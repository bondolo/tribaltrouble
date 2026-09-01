package com.oddlabs.tt.base.geom;

import com.oddlabs.geometry.AnimationInfo;
import com.oddlabs.geometry.SpriteInfo;
import com.oddlabs.util.Utils;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.net.URL;
import java.util.Objects;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Pure CPU geometry representation loaded from binary sprite files.
 */
public record SpriteGeometry(
                             BoundingBox[] bounds,
                             AnimationInfo.AnimationType[] animTypes
) implements BoundsProvider {
    public static SpriteGeometry load(String location) {
        URL url = Objects.requireNonNull(
                SpriteGeometry.class.getResource(location),
                () -> "Geometry resource not found: " + location
        );
        return load(url);
    }

    public static SpriteGeometry load(URL url) {
        Object[] spritesAndAnimations = Utils.loadObject(Object[].class, url);
        SpriteInfo[] spriteInfos = (SpriteInfo[]) spritesAndAnimations[0];
        AnimationInfo[] animationInfos = (AnimationInfo[]) spritesAndAnimations[1];

        BoundingBox[] bounds = Stream.generate(BoundingBox::new)
                .limit(animationInfos.length)
                .toArray(BoundingBox[]::new);

        AnimationInfo.AnimationType[] typeArray = IntStream.range(0, animationInfos.length)
                .mapToObj(i -> animationInfos[i].getType())
                .toArray(AnimationInfo.AnimationType[]::new);

        for (SpriteInfo spriteInfo : spriteInfos) {
            calculateBounds(spriteInfo, animationInfos, bounds);
        }

        for (BoundingBox bound : bounds) {
            bound.maximizeXYPlane();
        }

        return new SpriteGeometry(bounds, typeArray);
    }

    private static void calculateBounds(
            SpriteInfo spriteInfo,
            AnimationInfo[] animations,
            BoundingBox[] boundingBoxes
    ) {
        float[] initialPoseVertices = spriteInfo.getVertices();
        byte[][] skinNames = spriteInfo.getSkinNames();
        float[][] skinWeights = spriteInfo.getSkinWeights();
        int numVertices = initialPoseVertices.length / 3;

        int numBones = animations[0].getFrames()[0].length / 12;
        Matrix4f[] frameBones = new Matrix4f[numBones];
        for (int bone = 0; bone < frameBones.length; bone++) {
            frameBones[bone] = new Matrix4f();
        }
        Vector4f v = new Vector4f();
        Vector4f temp = new Vector4f();

        for (int anim = 0; anim < animations.length; anim++) {
            BoundingBox boundingBox = boundingBoxes[anim];
            int numFrames = animations[anim].getFrames().length;
            for (int frame = 0; frame < numFrames; frame++) {
                float[] frameAnimation = animations[anim].getFrames()[frame];
                for (int bone = 0; bone < numBones; bone++) {
                    int offset = bone * 12;
                    frameBones[bone].set(
                            frameAnimation[offset + 0], frameAnimation[offset + 4], frameAnimation[offset + 8], 0.0f,
                            frameAnimation[offset + 1], frameAnimation[offset + 5], frameAnimation[offset + 9], 0.0f,
                            frameAnimation[offset + 2], frameAnimation[offset + 6], frameAnimation[offset + 10], 0.0f,
                            frameAnimation[offset + 3], frameAnimation[offset + 7], frameAnimation[offset + 11], 1.0f
                    );
                }
                for (int vertex = 0; vertex < numVertices; vertex++) {
                    float x = initialPoseVertices[vertex * 3 + 0];
                    float y = initialPoseVertices[vertex * 3 + 1];
                    float z = initialPoseVertices[vertex * 3 + 2];
                    float resultX = 0f;
                    float resultY = 0f;
                    float resultZ = 0f;
                    v.set(x, y, z, 1f);
                    byte[] vertexSkinNames = skinNames[vertex];
                    float[] vertexSkinWeights = skinWeights[vertex];
                    for (int bone = 0; bone < vertexSkinNames.length; bone++) {
                        float weight = vertexSkinWeights[bone];
                        Matrix4f boneMatrix = frameBones[vertexSkinNames[bone]];
                        boneMatrix.transform(v, temp);
                        resultX += temp.x * weight;
                        resultY += temp.y * weight;
                        resultZ += temp.z * weight;
                    }
                    boundingBox.checkBounds(resultX, resultX, resultY, resultY, resultZ, resultZ);
                }
            }
        }
    }
}
