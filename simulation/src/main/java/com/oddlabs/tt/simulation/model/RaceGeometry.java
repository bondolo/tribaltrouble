package com.oddlabs.tt.simulation.model;

import com.oddlabs.geometry.AnimationInfo.AnimationType;
import com.oddlabs.tt.base.geom.BoundingBox;
import org.jspecify.annotations.NonNull;

/**
 * Predefined bounding boxes and animation types for race buildings and units.
 */
final class RaceGeometry {

    record BuildingBounds(@NonNull BoundingBox[] built, @NonNull BoundingBox[] halfbuilt,
                          @NonNull BoundingBox[] start) {
    }

    record UnitBounds(@NonNull BoundingBox[] bounds, AnimationType @NonNull [] animTypes) {
    }

    private static final AnimationType[] STANDARD_UNIT_ANIM_TYPES = {
            AnimationType.LOOP,
            AnimationType.LOOP,
            AnimationType.PLAIN,
            AnimationType.PLAIN
    };

    private static final AnimationType[] VIKING_CHIEFTAIN_ANIM_TYPES = {
            AnimationType.LOOP,
            AnimationType.LOOP,
            AnimationType.PLAIN,
            AnimationType.PLAIN,
            AnimationType.PLAIN,
            AnimationType.LOOP
    };

    private static final AnimationType[] NATIVE_CHIEFTAIN_ANIM_TYPES = {
            AnimationType.LOOP,
            AnimationType.LOOP,
            AnimationType.PLAIN,
            AnimationType.PLAIN,
            AnimationType.PLAIN
    };

    private static BoundingBox box(float radius, float minZ, float maxZ) {
        return new BoundingBox(-radius, radius, -radius, radius, minZ, maxZ);
    }

    private static BoundingBox[] single(BoundingBox b) {
        return new BoundingBox[]{b};
    }

    static final BuildingBounds VIKING_QUARTERS = new BuildingBounds(
            single(box(9.3193f, -0.0146f, 8.9678f)),
            single(box(11.1502f, -0.6629f, 9.5274f)),
            single(box(10.1260f, -0.0732f, 1.7590f))
    );

    static final BuildingBounds VIKING_ARMORY = new BuildingBounds(
            single(box(7.2741f, -0.9595f, 14.1583f)),
            single(box(9.8210f, -0.5338f, 11.9930f)),
            single(box(7.5091f, -0.0588f, 0.9514f))
    );

    static final BuildingBounds VIKING_TOWER = new BuildingBounds(
            single(box(3.7565f, -0.0125f, 11.2445f)),
            single(box(4.3294f, -0.3273f, 9.1499f)),
            single(box(3.9765f, -0.0132f, 0.8151f))
    );

    static final BuildingBounds NATIVE_QUARTERS = new BuildingBounds(
            single(box(7.6865f, -2.8651f, 11.1876f)),
            single(box(6.8610f, -0.1519f, 11.8963f)),
            single(box(7.7669f, -0.2312f, 0.5357f))
    );

    static final BuildingBounds NATIVE_ARMORY = new BuildingBounds(
            single(box(6.4127f, -2.3975f, 12.3240f)),
            single(box(7.4290f, -1.4744f, 11.7685f)),
            single(box(6.2053f, -0.2312f, 0.4965f))
    );

    static final BuildingBounds NATIVE_TOWER = new BuildingBounds(
            single(box(3.3199f, -2.8891f, 14.4556f)),
            single(box(3.3199f, -0.1944f, 14.4556f)),
            single(box(2.7952f, -0.1220f, 4.7485f))
    );

    static final UnitBounds VIKING_WARRIOR = new UnitBounds(
            new BoundingBox[]{
                    box(1.3711f, -0.0003f, 2.3131f),
                    box(1.4483f, -0.0803f, 2.7070f),
                    box(2.3132f, -0.0207f, 3.2753f),
                    box(2.9025f, -0.0649f, 2.3677f)
            },
            STANDARD_UNIT_ANIM_TYPES
    );

    static final UnitBounds VIKING_PEON = new UnitBounds(
            new BoundingBox[]{
                    box(1.0778f, 0.0026f, 2.0155f),
                    box(1.2768f, -0.1812f, 2.2373f),
                    box(1.5251f, -0.2061f, 1.9787f),
                    box(2.8993f, -0.1547f, 2.0181f)
            },
            STANDARD_UNIT_ANIM_TYPES
    );

    static final UnitBounds VIKING_CHIEFTAIN = new UnitBounds(
            new BoundingBox[]{
                    box(3.7905f, -0.0000f, 3.3144f),
                    box(4.6391f, -0.1875f, 4.2714f),
                    box(4.0929f, -0.0585f, 3.9888f),
                    box(5.5353f, -0.4045f, 5.5463f),
                    box(4.8117f, -0.5369f, 5.6095f),
                    box(3.1135f, -0.2702f, 4.6702f)
            },
            VIKING_CHIEFTAIN_ANIM_TYPES
    );

    static final UnitBounds NATIVE_WARRIOR = new UnitBounds(
            new BoundingBox[]{
                    box(1.2947f, -0.0055f, 2.9580f),
                    box(1.6364f, -0.5440f, 3.1482f),
                    box(2.3921f, -0.5585f, 2.9483f),
                    box(3.3991f, -0.2001f, 2.9365f)
            },
            STANDARD_UNIT_ANIM_TYPES
    );

    static final UnitBounds NATIVE_PEON = new UnitBounds(
            new BoundingBox[]{
                    box(0.8384f, 0.0002f, 2.0878f),
                    box(1.3432f, -0.2234f, 2.2690f),
                    box(1.4201f, -0.1561f, 2.0763f),
                    box(2.5860f, -0.2534f, 2.1919f)
            },
            STANDARD_UNIT_ANIM_TYPES
    );

    static final UnitBounds NATIVE_CHIEFTAIN = new UnitBounds(
            new BoundingBox[]{
                    box(1.9751f, -0.0573f, 3.3169f),
                    box(2.6033f, -0.2305f, 3.8052f),
                    box(4.3586f, -0.5000f, 3.6981f),
                    box(3.3748f, -0.7023f, 5.4011f),
                    box(3.9240f, -0.6144f, 4.3340f)
            },
            NATIVE_CHIEFTAIN_ANIM_TYPES
    );

    private RaceGeometry() {
    }
}
