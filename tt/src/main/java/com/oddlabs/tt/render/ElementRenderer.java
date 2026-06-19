package com.oddlabs.tt.render;

import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.model.Element;
import com.oddlabs.tt.model.snapshot.EntitySnapshot;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.viewer.Selection;
import org.jspecify.annotations.NonNull;

import java.util.List;

final class ElementRenderer<T extends Element<T>> {

    private final @NonNull RenderState render_state;
    private final boolean picking;
    private CameraState camera;

    private boolean visible_override;

    ElementRenderer(@NonNull Player local_player, @NonNull RenderQueues render_queues, @NonNull Picker picker,
            boolean picking, @NonNull SpriteSorter sprite_sorter, Selection selection) {
        this.picking = picking;
        this.render_state = new RenderState(local_player, sprite_sorter, render_queues, picker, selection);
    }

    @NonNull
    RenderState getRenderState() {
        return render_state;
    }

    void setup(CameraState camera_state) {
        this.camera = camera_state;
        render_state.setup(picking, camera);
    }

    public void renderSnapshot(@NonNull List<@NonNull EntitySnapshot> entities, @NonNull CameraState camera_state) {
        setup(camera_state);
        for (EntitySnapshot entity : entities) {
            RenderTools.FrustumIntersection frustum_state = camera.inNoDetailMode()
                    ? RenderTools.FrustumIntersection.ALL_INSIDE // Force all in frustum for map mode
                    : RenderTools.inFrustum(entity.bounds(), camera.getFrustum());

            if (visible_override || frustum_state != RenderTools.FrustumIntersection.ALL_OUTSIDE) {
                boolean old_override = visible_override;
                visible_override = visible_override || frustum_state == RenderTools.FrustumIntersection.ALL_INSIDE;
                render_state.setVisibleOverride(visible_override);
                render_state.visit(entity);
                visible_override = old_override;
            }
        }
    }
}
