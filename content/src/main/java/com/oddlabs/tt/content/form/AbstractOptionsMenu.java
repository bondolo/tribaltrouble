package com.oddlabs.tt.content.form;

import com.oddlabs.tt.base.util.Utils;
import com.oddlabs.tt.client.Peer;
import com.oddlabs.tt.gui.CancelListener;
import com.oddlabs.tt.gui.Form;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.HorizButton;
import com.oddlabs.tt.gui.Label;
import com.oddlabs.tt.gui.Origin;
import com.oddlabs.tt.gui.PanelGroup;
import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.simulation.SimulationConfig;

import java.util.ResourceBundle;

import static com.oddlabs.tt.gui.Placement.BOTTOM_LEFT;
import static com.oddlabs.tt.gui.Placement.LEFT_MID;

/**
 * Base options menu containing sub-panels for general, graphics, keybindings, accessibility, sound, and language.
 */
public abstract class AbstractOptionsMenu extends Form {
    private static final int BUTTON_WIDTH = 100;
    public static final ResourceBundle bundle = ResourceBundle.getBundle(OptionsMenu.class.getName());

    static String i18n(String key, Object... args) {
        return Utils.getBundleString(bundle, key, args);
    }

    private final GeneralPanel generalPanel;
    private final GraphicsPanel graphicsPanel;

    AbstractOptionsMenu(GUIRoot gui_root, Peer engine) {
        Label label_headline = new Label(i18n("options_caption"), Skin.getSkin().getHeadlineFont());
        addChild(label_headline);

        generalPanel = new GeneralPanel(gui_root, this::changeGamespeed);
        graphicsPanel = new GraphicsPanel(gui_root, this, engine);

        PanelGroup panel_group = new PanelGroup(
                generalPanel,
                graphicsPanel,
                new KeyBindingPanel(gui_root),
                new AccessibilityPanel(gui_root),
                new SoundPanel(gui_root, engine.getAudioManager()),
                new LanguagePanel(gui_root)
        );
        addChild(panel_group);

        // Buttons
        HorizButton button_close = new HorizButton(i18n("close"), BUTTON_WIDTH);
        button_close.addMouseClickListener(new CancelListener(this));
        addChild(button_close);

        HorizButton button_about = new HorizButton(i18n("about"), BUTTON_WIDTH);
        button_about.addMouseClickListener((_, _, _, _) -> gui_root.addModalForm(new CreditsForm(
                gui_root.getGUI().getSettings().last_revision)));
        addChild(button_about);

        // Place objects
        label_headline.place();
        panel_group.place(label_headline, BOTTOM_LEFT);
        button_close.place(Origin.AT_END);
        button_about.place(button_close, LEFT_MID);
        compileCanvas();
    }

    @Override
    protected void displayChangedNotify(int width, int height) {
        super.displayChangedNotify(width, height);
        centerPos();
        graphicsPanel.updateScaleLabel();
    }

    private static int preferred_gamespeed = SimulationConfig.DEFAULT_GAME_SPEED;

    public static int getPreferredGamespeed() {
        return preferred_gamespeed;
    }

    public static void setPreferredGamespeed(int speed) {
        preferred_gamespeed = speed;
    }

    protected final void chooseGamespeed(int speed) {
        generalPanel.chooseGamespeed(speed);
    }

    protected void changeGamespeed(int index) {
        setPreferredGamespeed(index);
    }
}
