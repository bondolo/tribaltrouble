package com.oddlabs.tt.client.delegate;

import com.oddlabs.tt.client.camera.GameCamera;
import com.oddlabs.tt.gui.CursorType;
import com.oddlabs.tt.gui.MouseButton;
import com.oddlabs.tt.input.GameAction;
import com.oddlabs.tt.input.InputEvent;
import com.oddlabs.tt.input.InputPhase;
import com.oddlabs.tt.simulation.model.Action;
import com.oddlabs.tt.client.viewer.WorldViewer;

public class TargetDelegate extends ControllableCameraDelegate<GameCamera> {
    private final Action action;

    public TargetDelegate(WorldViewer viewer, GameCamera camera, Action action) {
        super(viewer, camera);
        this.action = action;
    }

    @Override
    protected void pushZoomDelegate() {
        getGUIRoot().pushDelegate(new ZoomDelegate(getViewer(), getCamera()));
    }

    @Override
    public boolean canHoverBehind() {
        return true;
    }

    @Override
    protected final CursorType getCursorType() {
        return CursorType.TARGET;
    }

    @Override
    public void handleInput(InputEvent event) {
        if (event.getPhase() == InputPhase.PRESSED || event.getPhase() == InputPhase.REPEAT) {
            if (event.consumeAction(GameAction.UI_CANCEL)) {
                pop();
                event.consume();
                return;
            }
        }
        super.handleInput(event);
        if (event.isConsumed()) return;
    }

    @Override
    public void mousePressed(MouseButton button, int x, int y) {
        if (button == MouseButton.LEFT) {
            getViewer().getPicker().pickTarget(getViewer().getSelection().getCurrentSelection(),
                    getCamera().getState(), getViewer().getPeerHub().getPlayerInterface(),
                    x, y, action);
            pop();
        } else {
            super.mousePressed(button, x, y);
        }
    }
}
