package com.oddlabs.tt.client.viewer;

import com.oddlabs.tt.client.gui.WaitingForPlayersForm;
import com.oddlabs.tt.net.StallHandler;
import org.jspecify.annotations.Nullable;

/** Handler for client-side rendering during network stalls. */
final class ViewerStallHandler implements StallHandler {

    private static final float SHOW_WAITING_DELAY_SECONDS = 3f;

    private final WorldViewer viewer;

    private float local_stall_time;
    private int stall_tick;
    private @Nullable WaitingForPlayersForm waiting_for_players_form;

    ViewerStallHandler(WorldViewer viewer) {
        this.viewer = viewer;
    }

    private void resetStallTime() {
        local_stall_time = viewer.getTime();
    }

    @Override
    public void stopStall() {
        if (waiting_for_players_form != null) {
            waiting_for_players_form.remove();
            waiting_for_players_form = null;
        }
    }

    @Override
    public void peerhubFailed() {
        viewer.close();
    }

    @Override
    public void processStall(int tick) {
        if (stall_tick != tick) {
            IO.println("Stalled on tick " + tick);
            stall_tick = tick;
            resetStallTime();
        }
        float elapsed_time = viewer.getTime() - local_stall_time;
        if (tick == 0 || elapsed_time > SHOW_WAITING_DELAY_SECONDS) {
            if (waiting_for_players_form == null) {
                waiting_for_players_form = new WaitingForPlayersForm(viewer);
                viewer.getGUIRoot().addModalForm(waiting_for_players_form);
            }
        }
    }
}
