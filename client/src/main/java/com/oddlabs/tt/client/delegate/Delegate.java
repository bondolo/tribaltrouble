package com.oddlabs.tt.client.delegate;


import com.oddlabs.tt.engine.render.CameraState;
import com.oddlabs.tt.gui.FocusDirection;
import com.oddlabs.tt.gui.GUIObject;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.engine.render.GUIRenderer;
import com.oddlabs.tt.engine.render.LandscapeRenderer;
import com.oddlabs.tt.engine.render.MatrixStack;
import com.oddlabs.tt.engine.render.RenderQueues;
import com.oddlabs.tt.engine.render.state.RenderContext;
import com.oddlabs.tt.gui.delegate.InputDelegate;
import com.oddlabs.util.Color;

/**
 * Base class for all top-level UI states (delegates) in the game. Delegates handle
 * specific interaction modes (e.g., menus, in-game selection, targeting) and participate
 * in both 2D and 3D rendering passes.
 */
public abstract class Delegate extends GUIObject implements InputDelegate {
    private static final Color.Linear BACKGROUND_ALPHA = Color.Linear.BLACK.alpha(0.3f);

    @Override
    protected boolean shouldHandleActivate() {
        return false;
    }

    protected Delegate() {
        setPos(0, 0);
        setCanFocus(true);
    }

    @Override
    public void setFocus(FocusDirection direction) {
        super.setFocus(direction);
        switchFocus(direction, false);
    }

    @Override
    public void displayChangedNotify(int width, int height) {
        setDim(width, height);
    }

    @Override
    protected void doAdd() {
        super.doAdd();
        GUIRoot root = getParentGUIRoot();
        if (root != null) {
            displayChanged(root.getWidth(), root.getHeight());
            if (root.getModalDelegate() == null) {
                restoreFocus();
            }
        } else {
            restoreFocus();
        }
    }

    public void render3D(RenderContext context, LandscapeRenderer renderer, RenderQueues render_queues,
            CameraState state, MatrixStack modelViewStack, MatrixStack projectionStack) {
    }

    @Override
    public void render2D(GUIRenderer renderer) {
    }

    @Override
    public boolean keyboardBlocked() {
        return false;
    }

    protected final void renderBackgroundAlpha(GUIRenderer renderer) {
        renderer.drawColoredQuad(0, 0, getWidth(), getHeight(), BACKGROUND_ALPHA);
    }
}
