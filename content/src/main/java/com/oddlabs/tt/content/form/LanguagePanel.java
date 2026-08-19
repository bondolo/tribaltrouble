package com.oddlabs.tt.content.form;

import com.oddlabs.tt.gui.*;
import com.oddlabs.tt.gui.event.*;
import com.oddlabs.tt.client.gui.*;

import com.oddlabs.tt.gui.ColumnInfo;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.Group;
import com.oddlabs.tt.gui.IconLabel;
import com.oddlabs.tt.gui.Label;
import com.oddlabs.tt.gui.MultiColumnComboBox;
import com.oddlabs.tt.gui.Panel;
import com.oddlabs.tt.gui.Row;
import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.gui.event.RowListener;
import com.oddlabs.tt.content.Languages;
import com.oddlabs.tt.engine.render.Renderer;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Locale;

import static com.oddlabs.tt.gui.Placement.BOTTOM_LEFT;

/**
 * UI panel for selecting the application display language.
 */
public class LanguagePanel extends Panel {
    public LanguagePanel(@NonNull GUIRoot gui_root) {
        super(AbstractOptionsMenu.i18n("language_caption"));

        // language
        Group language_group = new Group();
        addChild(language_group);
        Label language_label = new Label(AbstractOptionsMenu.i18n("language_label"), Skin.getSkin().getEditFont());
        language_group.addChild(language_label);

        ColumnInfo[] language_infos = new ColumnInfo[]{new ColumnInfo("", 300)};
        var language_list_box = new MultiColumnComboBox<@NonNull Locale>(gui_root, language_infos, 200, false);

        // Check language logic
        String currentLanguage = Renderer.getRenderer().getSettings().control.language;
        if (!currentLanguage.equals("default") && !Languages.hasLanguage(Locale.forLanguageTag(currentLanguage))) {
            Renderer.getRenderer().getSettings().control.language = "default";
        }

        // Supported Language list
        Row<Locale, IconLabel> selectedLanguage = null;
        for (var langauge : Languages.getLanguages()) {
            var label = new Label(langauge.getDisplayName(langauge), Skin.getSkin().getMultiColumnComboBoxData()
                    .font());
            var flag = Skin.getSkin().getFlag(langauge.getLanguage());
            var iconLabel = new IconLabel(flag, label);
            Row<Locale, IconLabel> row = new Row<>(List.of(iconLabel), langauge);
            language_list_box.addRow(row);
            if (langauge.getLanguage().equals(Renderer.getRenderer().getSettings().control.language)) {
                selectedLanguage = row;
            }
        }

        // System default last
        var label = new Label(AbstractOptionsMenu.i18n("system_default"), Skin.getSkin().getMultiColumnComboBoxData()
                .font());
        var iconLabel = new IconLabel(Skin.getSkin().getFlagDefault(), label);
        Row<Locale, IconLabel> row = new Row<>(List.of(iconLabel), Renderer.getRenderer().getDefaultLocale());
        language_list_box.addRow(row);
        if (null == selectedLanguage || Renderer.getRenderer().getSettings().control.language.equals("default")) {
            selectedLanguage = row;
        }

        language_list_box.selectRow(selectedLanguage);
        language_list_box.addRowListener(new RowListener<>() {
            @Override
            public void rowDoubleClicked(@NonNull Locale locale) {
                Renderer.getRenderer().getSettings().control.language = locale.getVariant().equals("default")
                        ? "default" : locale.toLanguageTag();
                IO.println("set language:" + Renderer.getRenderer().getSettings().control.language);
                gui_root.addModalForm(new MessageForm(AbstractOptionsMenu.i18n("language_change_next_run")));
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
