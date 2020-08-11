package com.oddlabs.tt.tutorial;

import com.oddlabs.tt.delegate.GameStatsDelegate;
import com.oddlabs.tt.delegate.InGameMainMenu;
import com.oddlabs.tt.delegate.Menu;
import com.oddlabs.tt.form.TutorialForm;
import com.oddlabs.tt.form.TutorialForm.TutorialType;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.Group;
import com.oddlabs.tt.render.Renderer;
import com.oddlabs.tt.util.Utils;
import com.oddlabs.tt.viewer.InGameInfo;
import com.oddlabs.tt.viewer.WorldViewer;

public final strictfp class TutorialInGameInfo implements InGameInfo {
	private TutorialType next_tutorial;

	public boolean setNextTutorial(GUIRoot gui_root, TutorialType next_tutorial) {
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
	public void addGUI(WorldViewer viewer, InGameMainMenu menu, Group game_infos) {
		menu.addAbortButton(Utils.getBundleString(Menu.bundle, "end_tutorial"));
	}

    @Override
	public void addGameOverGUI(WorldViewer viewer, GameStatsDelegate delegate, int header_y, Group group) {
		throw new RuntimeException("Not implemented");
	}

    @Override
	public void abort(WorldViewer viewer) {
		next_tutorial = null;
		viewer.close();
	}

    @Override
	public void close(WorldViewer viewer) {
		if (next_tutorial != null)
			TutorialForm.startTutorial(viewer.getNetwork(), viewer.getGUIRoot(), next_tutorial);
		else
			Renderer.startMenu(viewer.getNetwork(), viewer.getGUIRoot().getGUI());
	}
}
