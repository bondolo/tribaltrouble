package com.oddlabs.tt.content.tutorial;

import com.oddlabs.tt.client.camera.Camera;
import com.oddlabs.tt.client.camera.StaticCamera;
import com.oddlabs.tt.client.delegate.GameStatsDelegate;
import com.oddlabs.tt.client.viewer.InGameInfo;
import com.oddlabs.tt.client.viewer.WorldViewer;
import com.oddlabs.tt.content.menu.InGameMainMenu;
import com.oddlabs.tt.content.menu.InGameMenuHook;
import com.oddlabs.tt.content.menu.Menu;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.Group;
import org.jspecify.annotations.NonNull;

public final class TutorialInGameInfo implements InGameInfo, InGameMenuHook {
    private int next_tutorial = -1;

    @Override
    public void openInGameMenu(@NonNull WorldViewer viewer, @NonNull Camera camera) {
        viewer.getGUIRoot().pushDelegate(new InGameMainMenu(viewer, new StaticCamera(camera.getState())));
    }

    public boolean setNextTutorial(GUIRoot gui_root, int next_tutorial) {
        if (TutorialForm.checkTutorial(gui_root, next_tutorial)) {
            this.next_tutorial = next_tutorial;
            return true;
        } else
            return false;
    }

    @Override
    public boolean isRated() {
        return false;
    }

    @Override
    public boolean isMultiplayer() {
        return false;
    }

    @Override
    public float getRandomStartPosition() {
        return 0f;
    }

    @Override
    public void addGUI(WorldViewer viewer, @NonNull InGameMainMenu menu, Group game_infos) {
        menu.addAbortButton(Menu.i18n("end_tutorial"));
    }

    @Override
    public void addGameOverGUI(WorldViewer viewer, GameStatsDelegate delegate, int header_y, Group group) {
        throw new UnsupportedOperationException("GameOver GUI not implemented for Tutorial");
    }

    @Override
    public void abort(@NonNull WorldViewer viewer) {
        next_tutorial = -1;
        viewer.close();
    }

    @Override
    public void close(@NonNull WorldViewer viewer) {
        if (next_tutorial != -1)
            TutorialForm.startTutorial(viewer.getNetwork(), viewer.getGUIRoot(), next_tutorial);
        else
            Menu.startMenu(viewer.getNetwork(), viewer.getGUIRoot().getGUI());
    }
}
