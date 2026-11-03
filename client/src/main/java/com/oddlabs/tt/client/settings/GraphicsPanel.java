package com.oddlabs.tt.client.settings;

import com.oddlabs.tt.base.global.Settings;
import com.oddlabs.tt.client.DisplayModeController;
import com.oddlabs.tt.client.Peer;
import com.oddlabs.tt.gui.CheckBox;
import com.oddlabs.tt.gui.ColumnInfo;
import com.oddlabs.tt.gui.DisplayChangeForm;
import com.oddlabs.tt.gui.Form;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.Group;
import com.oddlabs.tt.gui.Label;
import com.oddlabs.tt.gui.MultiColumnComboBox;
import com.oddlabs.tt.gui.Panel;
import com.oddlabs.tt.gui.Row;
import com.oddlabs.tt.gui.Slider;
import com.oddlabs.tt.gui.SortedLabel;
import com.oddlabs.tt.gui.event.RowListener;
import com.oddlabs.tt.gui.GUISettings;
import com.oddlabs.tt.window.SerializableDisplayMode;
import com.oddlabs.tt.window.Window;
import com.oddlabs.tt.window.WindowSettings;

import java.util.List;

import static com.oddlabs.tt.gui.Placement.BOTTOM_LEFT;
import static com.oddlabs.tt.gui.Placement.RIGHT_MID;
import static com.oddlabs.tt.gui.Placement.RIGHT_TOP;

/**
 * Configuration panel for graphical settings, display resolutions, and UI scale.
 */
public final class GraphicsPanel extends Panel {
    private final Label label_pct;
    private final Window window;
    private final Settings settings;
    private final DisplayModeController displayController;
    private final GUIRoot gui_root;

    public GraphicsPanel(GUIRoot gui_root, Form options, Window window, Settings settings,
            DisplayModeController displayController) {
        super(gui_root, AbstractOptionsMenu.i18n("graphics_caption"));
        this.gui_root = gui_root;
        this.window = window;
        this.settings = settings;
        this.displayController = displayController;
        var labelFont = getSkin().getEditFont();

        // Display mode
        Group mode_group = new Group(gui_root);
        addChild(mode_group);

        Label mode_label = new Label(AbstractOptionsMenu.i18n("display_mode"), labelFont);
        mode_group.addChild(mode_label);

        MultiColumnComboBox<SerializableDisplayMode> mode_list_box = new MultiColumnComboBox<>(gui_root, 200,
                false, new ColumnInfo("", 150));

        WindowSettings windowSettings = WindowSettings.from(settings);
        GUISettings guiSettings = GUISettings.from(settings);

        // Fullscreen
        Group group_fullscreen = new Group(gui_root);
        addChild(group_fullscreen);
        CheckBox cb_fullscreen = new CheckBox(gui_root, windowSettings.fullscreen,
                AbstractOptionsMenu.i18n(
                        "fullscreen"), AbstractOptionsMenu.i18n("fullscreen_tip"));
        cb_fullscreen.addCheckBoxListener(marked -> {
            DisplayChangeForm display_change_form = new DisplayChangeForm(
                    gui_root,
                    switch_now -> {
                        if (switch_now) {
                            displayController.toggleFullscreen();
                        } else {
                            windowSettings.fullscreen = marked;
                        }

                        // Force refresh of available modes for the list box
                        refreshResolutionList(marked, mode_list_box);
                    });
            gui_root.addModalForm(display_change_form);
        });
        group_fullscreen.addChild(cb_fullscreen);
        cb_fullscreen.place();
        group_fullscreen.compileCanvas();

        // UI Scale
        Group group_ui_scale = new Group(gui_root);
        addChild(group_ui_scale);
        Label label_ui_scale = new Label(AbstractOptionsMenu.i18n("ui_scale"), labelFont);
        group_ui_scale.addChild(label_ui_scale);
        label_pct = new Label("100%", labelFont);
        updateScaleLabel();
        group_ui_scale.addChild(label_pct);

        int initialValue = Math.clamp((long) (guiSettings.ui_scale * 1000), 0, 1000);

        Slider slider_ui_scale = new Slider(gui_root, 150, 0, 1000, initialValue);
        group_ui_scale.addChild(slider_ui_scale);

        slider_ui_scale.addValueListener(value -> {
            guiSettings.ui_scale = value / 1000f;
            updateScaleLabel();
            float currentScale = gui_root.getGlobalScale();
            float targetScale = GUIRoot.calculateEffectiveScale(window.getWidth(), window.getHeight(),
                    guiSettings.ui_scale);
            if (targetScale != currentScale) {
                gui_root.displayChanged(window.getWidth(), window.getHeight());
                slider_ui_scale.reanchor();
            }
        });

        slider_ui_scale.addReleaseListener(() -> {
            gui_root.displayChanged(window.getWidth(), window.getHeight());
        });

        label_ui_scale.place();
        label_pct.place(label_ui_scale, RIGHT_MID);
        slider_ui_scale.place(label_ui_scale, BOTTOM_LEFT);
        group_ui_scale.compileCanvas();

        refreshResolutionList(windowSettings.fullscreen, mode_list_box);

        mode_list_box.addRowListener(new RowListener<>() {
            @Override
            public void rowDoubleClicked(SerializableDisplayMode mode) {
                boolean currentIsExclusive = window.isExclusiveFullscreen();
                boolean targetFullscreen = windowSettings.fullscreen;
                boolean targetIsExclusive = targetFullscreen && window.isExclusiveFullscreenMode(mode);

                if (currentIsExclusive || targetIsExclusive) {
                    gui_root.addModalForm(new DisplayChangeForm(gui_root, switch_now -> {
                        displayController.switchMode(mode, switch_now);
                        if (switch_now) {
                            gui_root.displayChanged(mode.getWidth(), mode.getHeight());
                        }
                    }));
                } else {
                    displayController.switchMode(mode, true);
                    gui_root.displayChanged(mode.getWidth(), mode.getHeight());
                }
            }
        });

        mode_group.addChild(mode_list_box);
        mode_label.place();
        mode_list_box.place(mode_label, BOTTOM_LEFT);
        mode_group.compileCanvas();

        // Placement
        mode_group.place();
        group_ui_scale.place(mode_group, RIGHT_TOP);
        group_fullscreen.place(group_ui_scale, BOTTOM_LEFT);
        compileCanvas();
    }

    public GraphicsPanel(GUIRoot gui_root, Form options, Peer peer) {
        this(gui_root, options, peer.getWindow(), peer.getSettings(), peer);
    }

    private void refreshResolutionList(boolean fullscreen, MultiColumnComboBox<
            SerializableDisplayMode> list_box) {
        var currentMode = window.getDisplayMode();
        List<SerializableDisplayMode> modes = fullscreen ? window.getFullscreenDisplayModes() : window
                .getWindowedDisplayModes();

        list_box.clear();
        Row<SerializableDisplayMode, Label> currentRowToSelect = null;
        for (int i = 0; i < modes.size(); i++) {
            var m = modes.get(i);
            String mode_string = AbstractOptionsMenu.i18n("mode", m.getWidth(), m.getHeight(), m.getFrequency());
            Label label = new SortedLabel(mode_string, i, getSkin().getMultiColumnComboBoxData().font());
            var row = new Row<>(gui_root, List.of(label), m);
            list_box.addRow(row);
            if (m.isEquivalent(currentMode)) {
                currentRowToSelect = row;
            }
        }

        if (currentRowToSelect != null) {
            list_box.selectRow(currentRowToSelect);
        } else if (list_box.getSize() > 0) {
            list_box.selectFirst();
        }
    }

    public void updateScaleLabel() {
        float scale = GUIRoot.calculateEffectiveScale(window.getWidth(), window.getHeight(),
                GUISettings.from(settings).ui_scale);
        label_pct.setText(String.format("%d%%", (int) (scale * 100)));
    }
}
