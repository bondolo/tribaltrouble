package com.oddlabs.tt.client.render;

import com.oddlabs.tt.audio.AudioImplementation;
import com.oddlabs.tt.client.viewer.Selection;
import com.oddlabs.tt.engine.render.CameraState;
import com.oddlabs.tt.engine.render.RenderQueues;
import com.oddlabs.tt.engine.render.RenderTools;
import com.oddlabs.tt.simulation.model.AbstractElementNode;
import com.oddlabs.tt.simulation.model.Element;
import com.oddlabs.tt.simulation.model.ElementNode;
import com.oddlabs.tt.simulation.player.Player;
import org.jspecify.annotations.NonNull;

/**
 * Traverses element spatial hierarchy and dispatches visible entities to {@link RenderState}.
 */
final class ElementRenderer<T extends Element<T>> {

    private final @NonNull RenderState render_state;
    private final boolean picking;
    private CameraState camera;

    private boolean visible_override;

    ElementRenderer(@NonNull Player local_player, @NonNull RenderQueues render_queues, @NonNull Picker picker,
            boolean picking, @NonNull SpriteSorter sprite_sorter, Selection selection,
            @NonNull AudioImplementation audio) {
        this.picking = picking;
        this.render_state = new RenderState(local_player, sprite_sorter, render_queues, picker, selection, audio);
    }

    @NonNull
    RenderState getRenderState() {
        return render_state;
    }

    void setup(CameraState camera_state) {
        this.camera = camera_state;
        render_state.setup(picking, camera);
    }

    public void visit(@NonNull AbstractElementNode<T> node) {
        RenderTools.FrustumIntersection frustum_state = camera.inNoDetailMode()
                ? RenderTools.FrustumIntersection.ALL_INSIDE // Force all in frustum for map mode
                : RenderTools.inFrustum(node, camera.getFrustum());

        if (visible_override || frustum_state != RenderTools.FrustumIntersection.ALL_OUTSIDE) {
            boolean old_override = visible_override;
            visible_override = visible_override || frustum_state == RenderTools.FrustumIntersection.ALL_INSIDE;

            for (T element = node.getModels().getFirst(); element != null; element = element.getNext()) {
                render_state.visit(element);
            }
            if (node instanceof ElementNode<T> elementNode) {
                for (AbstractElementNode<T> child : elementNode.children()) {
                    visit(child);
                }
            }

            visible_override = old_override;
        }
    }
}
