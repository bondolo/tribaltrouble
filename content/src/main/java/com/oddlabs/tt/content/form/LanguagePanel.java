package com.oddlabs.tt.content.form;

import com.oddlabs.tt.content.Languages;
import com.oddlabs.tt.base.global.Settings;
import com.oddlabs.tt.gui.ColumnInfo;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.Group;
import com.oddlabs.tt.gui.IconLabel;
import com.oddlabs.tt.gui.Label;
import com.oddlabs.tt.gui.MessageForm;
import com.oddlabs.tt.gui.MultiColumnComboBox;
import com.oddlabs.tt.gui.Panel;
import com.oddlabs.tt.gui.Row;
import com.oddlabs.tt.gui.event.RowListener;
import com.oddlabs.tt.gui.LocaleSettings;
import com.oddlabs.tt.client.Peer;

import java.util.List;
import java.util.Locale;

import static com.oddlabs.tt.gui.Placement.BOTTOM_LEFT;

/**
 * UI panel for selecting the active interface language and locale.
 */
public class LanguagePanel extends Panel {
    public LanguagePanel(GUIRoot gui_root) {
        super(gui_root, AbstractOptionsMenu.i18n("language_caption"));
        var comboFont = skin.getMultiColumnComboBoxData().font();
        Settings settings = gui_root.getGUI().getSettings();
        LocaleSettings localeSettings = LocaleSettings.from(settings);
        Locale defaultLocale = Peer.getDefaultLocale();

        // Language
        Group language_group = new Group(gui_root);
        addChild(language_group);

        Label language_label = new Label(AbstractOptionsMenu.i18n("language_label"), skin.getEditFont());
        language_group.addChild(language_label);

        var language_list_box = new MultiColumnComboBox<Locale>(gui_root, 200, false,
                new ColumnInfo("", 300));

        // Check language logic
        String currentLanguage = localeSettings.language;
        if (!currentLanguage.equals("default") && !Languages.hasLanguage(Locale.forLanguageTag(currentLanguage))) {
            localeSettings.language = "default";
        }

        // Supported Language list
        Row<Locale, IconLabel> selectedLanguage = null;
        for (var langauge : Languages.getLanguages()) {
            var label = new Label(langauge.getDisplayName(langauge), comboFont);
            var flag = skin.getFlag(langauge.getLanguage());
            var iconLabel = new IconLabel(flag, label);
            Row<Locale, IconLabel> row = new Row<>(gui_root, List.of(iconLabel), langauge);
            language_list_box.addRow(row);
            if (langauge.getLanguage().equals(localeSettings.language)) {
                selectedLanguage = row;
            }
        }

        // System default last
        var label = new Label(AbstractOptionsMenu.i18n("system_default"), comboFont);
        var iconLabel = new IconLabel(skin.getFlagDefault(), label);
        Row<Locale, IconLabel> row = new Row<>(gui_root, List.of(iconLabel), defaultLocale);
        language_list_box.addRow(row);
        if (null == selectedLanguage || localeSettings.language.equals("default")) {
            selectedLanguage = row;
        }

        language_list_box.selectRow(selectedLanguage);
        language_list_box.addRowListener(new RowListener<>() {
            @Override
            public void rowDoubleClicked(Locale locale) {
                localeSettings.language = locale.getVariant().equals("default")
                        ? "default" : locale.toLanguageTag();
                IO.println("set language:" + localeSettings.language);
                gui_root.addModalForm(new MessageForm(gui_root, AbstractOptionsMenu.i18n("language_change_next_run")));
            }
        });

        language_group.addChild(language_list_box);
        language_label.place();
        language_list_box.place(language_label, BOTTOM_LEFT);
        language_group.compileCanvas();

        // Placement
        language_group.place();
        compileCanvas();
    }
}
