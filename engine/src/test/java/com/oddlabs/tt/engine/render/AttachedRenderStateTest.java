package com.oddlabs.tt.engine.render;

import com.oddlabs.tt.simulation.model.Model;
import com.oddlabs.util.Color;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link AttachedRenderState}.
 */
final class AttachedRenderStateTest {

    private static final class TestAccessory implements StaticAccessory {
        private final boolean hasTeamColor;

        TestAccessory(boolean hasTeamColor) {
            this.hasTeamColor = hasTeamColor;
        }

        @Override
        public boolean isVisible(Model parent, CameraState camera) {
            return true;
        }

        @Override
        public boolean hasTeamColor() {
            return hasTeamColor;
        }

        @Override
        public void getRelativeTransform(Matrix4f dest, Model parent) {
            dest.translate(1f, 2f, 3f);
        }
    }

    private static final class TestBillboardAccessory implements BillboardAccessory {
        @Override
        public boolean isVisible(Model parent, CameraState camera) {
            return true;
        }

        @Override
        public void animate(float dt) {
        }

        @Override
        public void getRelativeTransform(Matrix4f dest, Model parent) {
            dest.translate(0f, 0f, 5f);
        }
    }

    private static final class DummySceneContext implements SceneContext {
        @Override
        public RenderQueues getRenderQueues() {
            throw new UnsupportedOperationException();
        }

        @Override
        public CameraState getCamera() {
            return null;
        }

        @Override
        public boolean isResponding(Model model) {
            return false;
        }
    }

    @Test
    void testTeamColorOutputDelegation() {
        Color.Linear teamRed = new Color.Linear(1f, 0f, 0f, 1f);

        ModelVisitor<Model> visitor = new ModelVisitor<>() {
            @Override
            public SpriteKey getSpriteKey(ElementSceneContext<Model> render_state) {
                return null;
            }

            @Override
            public Color getSelectionColor(ElementSceneContext<Model> render_state) {
                return Color.Linear.WHITE;
            }

            @Override
            public Color getTeamColor(ElementSceneContext<Model> render_state) {
                return teamRed;
            }

            @Override
            public VisualPattern getPattern(ElementSceneContext<Model> render_state) {
                return VisualPattern.NONE;
            }

            @Override
            public void getTransform(ElementSceneContext<Model> render_state, Matrix4f dest) {
                dest.identity();
            }
        };

        ElementSceneContext<Model> parentContext = new ElementSceneContext<>(new DummySceneContext());
        parentContext.setup(visitor, null);

        AttachedRenderState state = new AttachedRenderState();

        // Standard unit accessory inherits parent team color
        state.setup(parentContext, new TestAccessory(true));
        assertEquals(teamRed, state.getTeamColor());

        // Accessory with hasTeamColor = false (or BillboardAccessory) returns TRANSPARENT
        state.setup(parentContext, new TestAccessory(false));
        assertEquals(Color.Linear.TRANSPARENT, state.getTeamColor());

        state.setup(parentContext, new TestBillboardAccessory());
        assertEquals(Color.Linear.TRANSPARENT, state.getTeamColor());
    }

    @Test
    void testRespectParentScaleFactors() {
        ModelVisitor<Model> scaledVisitor = new ModelVisitor<>() {
            @Override
            public SpriteKey getSpriteKey(ElementSceneContext<Model> render_state) {
                return null;
            }

            @Override
            public Color getSelectionColor(ElementSceneContext<Model> render_state) {
                return Color.Linear.WHITE;
            }

            @Override
            public Color getTeamColor(ElementSceneContext<Model> render_state) {
                return Color.Linear.WHITE;
            }

            @Override
            public VisualPattern getPattern(ElementSceneContext<Model> render_state) {
                return VisualPattern.NONE;
            }

            @Override
            public void getTransform(ElementSceneContext<Model> render_state, Matrix4f dest) {
                dest.translation(10f, 20f, 30f).scale(2.0f, 3.0f, 4.0f);
            }
        };

        ElementSceneContext<Model> parentContext = new ElementSceneContext<>(new DummySceneContext());
        parentContext.setup(scaledVisitor, null);

        AttachedRenderState state = new AttachedRenderState();
        state.setup(parentContext, new TestAccessory(true));

        Matrix4f result = new Matrix4f();
        state.getTransform(result);

        // Parent position is (10, 20, 30), scale is (2, 3, 4).
        // Relative offset is (1, 2, 3), scaled by (2, 3, 4) -> (2, 6, 12).
        // World translation should be (12, 26, 42).
        assertEquals(12f, result.m30(), 1e-4f);
        assertEquals(26f, result.m31(), 1e-4f);
        assertEquals(42f, result.m32(), 1e-4f);

        // Matrix scale factors should remain (2, 3, 4).
        float sx = (float) Math.sqrt(result.m00() * result.m00() + result.m01() * result.m01() + result.m02() * result.m02());
        float sy = (float) Math.sqrt(result.m10() * result.m10() + result.m11() * result.m11() + result.m12() * result.m12());
        float sz = (float) Math.sqrt(result.m20() * result.m20() + result.m21() * result.m21() + result.m22() * result.m22());
        assertEquals(2.0f, sx, 1e-4f);
        assertEquals(3.0f, sy, 1e-4f);
        assertEquals(4.0f, sz, 1e-4f);
    }

    @Test
    void testWhiteModelVisitorHasTransparentTeamColor() {
        WhiteModelVisitor<Model> visitor = WhiteModelVisitor.getInstance();
        ElementSceneContext<Model> context = new ElementSceneContext<>(new DummySceneContext());
        assertEquals(Color.Linear.TRANSPARENT, visitor.getTeamColor(context));
    }
}
