package com.oddlabs.tt.render;

import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.snapshot.EntitySnapshot;
import com.oddlabs.util.Color;
import org.joml.Matrix4f;
import org.jspecify.annotations.NonNull;

import java.util.Optional;

/**
 * A specialized {@link ModelVisitor} that renders models with a neutral white team color.
 * Used as a base for custom visitors or for temporary visual effects.
 */
class WhiteModelVisitor<S extends EntitySnapshot> extends ModelVisitor<S> {
    private static final WhiteModelVisitor<EntitySnapshot> INSTANCE = new WhiteModelVisitor<>();

    @SuppressWarnings("unchecked")
    @NonNull
    public static <S extends EntitySnapshot> WhiteModelVisitor<S> getInstance() {
        return (WhiteModelVisitor<S>) INSTANCE;
    }

    @Override
    public @NonNull Optional<SpriteKey> getSpriteKey(@NonNull ElementRenderState<S> render_state) {
        return Optional.empty();
    }

    @Override
    public @NonNull Color getSelectionColor(@NonNull ElementRenderState<S> render_state) {
        return Color.Linear.WHITE;
    }

    @Override
    public @NonNull Color getTeamColor(@NonNull ElementRenderState<S> render_state) {
        return Color.Linear.WHITE;
    }

    @Override
    public Selectable.@NonNull VisualPattern getPattern(@NonNull ElementRenderState<S> render_state) {
        return Selectable.VisualPattern.NONE;
    }

    @Override
    public void getTransform(@NonNull ElementRenderState<S> render_state, @NonNull Matrix4f dest) {
        EntitySnapshot entity = render_state.getEntity();
        float angle = (float) Math.atan2(entity.dirY(), entity.dirX());
        dest.translation(entity.x(), entity.y(), entity.z())
                .rotate(angle, 0f, 0f, 1f);
    }
}
