package com.oddlabs.tt.delegate;


import com.oddlabs.net.NetworkSelector;
import com.oddlabs.tt.camera.Camera;
import com.oddlabs.tt.form.QuestionForm;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.KeyboardEvent;
import com.oddlabs.tt.gui.MenuButton;
import com.oddlabs.tt.util.Utils;
import org.lwjgl.input.Keyboard;

public final strictfp class CampaignMapMenu extends Menu {

	public CampaignMapMenu(NetworkSelector network, GUIRoot gui_root, Camera camera) {
		super(network, gui_root, camera);
		reload();
	}

	private void addAbortButton() {
		String abort_text = Utils.getBundleString(bundle, "end_campaign");
		MenuButton abort = new MenuButton(abort_text, COLOR_NORMAL, COLOR_ACTIVE);
		addChild(abort);
		abort.addMouseClickListener((int button, int x, int y, int clicks) -> {
			setMenuCentered(new QuestionForm(Utils.getBundleString(bundle, "end_game_confirm"), (int buttonform, int xform, int yform, int clicksform) -> {
			CampaignMapForm.closeCampaign(getNetwork(), getGUIRoot().getGUI());
		}));
		});
	}

    @Override
	protected void addButtons() {
		addResumeButton();

		addDefaultOptionsButton();

		addAbortButton();

		addExitButton();
	}

    @Override
	protected void keyPressed(KeyboardEvent event) {
		switch(event.getKeyCode()) {
			case Keyboard.KEY_ESCAPE:
				pop();
				break;
			default:
				super.keyPressed(event);
				break;
		}
	}

    @Override
	protected void renderGeometry() {
		super.renderGeometry();
		renderBackgroundAlpha();
	}
}
