package com.oddlabs.tt.client.form;

import com.oddlabs.tt.client.gui.CancelListener;
import com.oddlabs.tt.client.gui.Form;
import com.oddlabs.tt.client.gui.GUIRoot;
import com.oddlabs.tt.client.gui.HorizButton;
import com.oddlabs.tt.client.gui.Label;
import com.oddlabs.tt.client.gui.Origin;
import com.oddlabs.tt.client.gui.PanelGroup;
import com.oddlabs.tt.client.gui.Skin;
import com.oddlabs.tt.base.util.Utils;
import org.jspecify.annotations.NonNull;

import java.util.ResourceBundle;

import static com.oddlabs.tt.client.gui.Placement.BOTTOM_LEFT;
import static com.oddlabs.tt.client.gui.Placement.LEFT_MID;

public abstract class AbstractOptionsMenu extends Form {
    private static final int BUTTON_WIDTH = 100;
    public static final ResourceBundle bundle = ResourceBundle.getBundle(OptionsMenu.class.getName());

    static @NonNull String i18n(@NonNull String key, @NonNull Object @NonNull... args) {
        return Utils.getBundleString(bundle, key, args);
    }

    private final @NonNull GeneralPanel generalPanel;
    private final @NonNull GraphicsPanel graphicsPanel;

    AbstractOptionsMenu(@NonNull GUIRoot gui_root) {
        Label label_headline = new Label(i18n("options_caption"), Skin.getSkin().getHeadlineFont());
        addChild(label_headline);

        generalPanel = new GeneralPanel(gui_root, this::changeGamespeed);
        graphicsPanel = new GraphicsPanel(gui_root, this);

        PanelGroup panel_group = new PanelGroup(
                generalPanel,
                graphicsPanel,
                new KeyBindingPanel(gui_root),
                new AccessibilityPanel(gui_root),
                new SoundPanel(gui_root),
                new LanguagePanel(gui_root)
        );
        addChild(panel_group);

        // Buttons
        HorizButton button_close = new HorizButton(i18n("close"), BUTTON_WIDTH);
        button_close.addMouseClickListener(new CancelListener(this));
        addChild(button_close);

        HorizButton button_about = new HorizButton(i18n("about"), BUTTON_WIDTH);
        button_about.addMouseClickListener((_, _, _, _) -> gui_root.addModalForm(new CreditsForm()));
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

    protected final void chooseGamespeed(int speed) {
        generalPanel.chooseGamespeed(speed);
    }

    protected void changeGamespeed(int index) {
        com.oddlabs.tt.base.global.Globals.gamespeed = index;
    }
}
