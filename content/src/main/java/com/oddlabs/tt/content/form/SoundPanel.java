package com.oddlabs.tt.content.form;

import com.oddlabs.event.Deterministic;
import com.oddlabs.tt.audio.AudioManager;
import com.oddlabs.tt.audio.AudioSettings;
import com.oddlabs.tt.engine.settings.AccessibilitySettings;
import com.oddlabs.tt.gui.CheckBox;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.Group;
import com.oddlabs.tt.gui.Label;
import com.oddlabs.tt.gui.Panel;
import com.oddlabs.tt.gui.PulldownButton;
import com.oddlabs.tt.gui.PulldownItem;
import com.oddlabs.tt.gui.PulldownMenu;
import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.gui.Slider;

import static com.oddlabs.tt.gui.Placement.BOTTOM_LEFT;
import static com.oddlabs.tt.gui.Placement.RIGHT_MID;

/**
 * UI panel for adjusting audio settings, including sound effects and music volume.
 */
public class SoundPanel extends Panel {
    private static final int SLIDER_WIDTH = 270;
    private static final int MAX_VALUE = 20;
    private static final boolean TEMPORARILY_DISABLE_MUSIC_CONTROLS = false;

    public SoundPanel(GUIRoot gui_root, AudioManager audioManager) {
        this(gui_root, AudioSettings.from(gui_root.getGUI().getSettings()),
                AccessibilitySettings.from(gui_root.getGUI().getSettings()),
                audioManager,
                gui_root.getGUI().getEventQueue().getDeterministic());
    }

    public SoundPanel(GUIRoot gui_root, AudioSettings audioSettings,
            AccessibilitySettings accessibilitySettings, AudioManager audioManager,
            Deterministic deterministic
    ) {
        super(AbstractOptionsMenu.i18n("sound_caption"));

        boolean audioCreated = deterministic.log(audioManager != null);

        // Sound
        Group group_music = new Group();
        addChild(group_music);
        Label label_music_low = new Label(AbstractOptionsMenu.i18n("low"), Skin.getSkin().getEditFont());
        group_music.addChild(label_music_low);
        Label label_music_high = new Label(AbstractOptionsMenu.i18n("high"), Skin.getSkin().getEditFont());
        group_music.addChild(label_music_high);
        CheckBox cb_music = new CheckBox(audioSettings.play_music, AbstractOptionsMenu.i18n("music"));
        group_music.addChild(cb_music);
        Label label_music = new Label(AbstractOptionsMenu.i18n("music_volume"), Skin.getSkin().getEditFont());
        group_music.addChild(label_music);

        Slider slider_music = new Slider(SLIDER_WIDTH, 0, MAX_VALUE, (int) (audioSettings.music_gain * MAX_VALUE));
        slider_music.setDisabled(TEMPORARILY_DISABLE_MUSIC_CONTROLS || !cb_music.isMarked());
        group_music.addChild(slider_music);

        cb_music.addCheckBoxListener(marked -> {
            if (audioSettings.play_music != marked) {
                audioManager.toggleMusic();
            }
            slider_music.setDisabled(!marked);
            audioSettings.play_music = marked;
        });
        slider_music.addValueListener(value -> {
            float music_gain = (float) value / MAX_VALUE;
            audioSettings.music_gain = music_gain;
            audioManager.setMusicGain(music_gain);
        });

        cb_music.place();
        label_music.place(cb_music, BOTTOM_LEFT);
        label_music_low.place(label_music, BOTTOM_LEFT);
        slider_music.place(label_music_low, RIGHT_MID);
        label_music_high.place(slider_music, RIGHT_MID);
        group_music.compileCanvas();
        group_music.setDisabled(TEMPORARILY_DISABLE_MUSIC_CONTROLS || !audioCreated);

        Group group_sound = new Group();
        addChild(group_sound);
        Label label_sound_low = new Label(AbstractOptionsMenu.i18n("low"), Skin.getSkin().getEditFont());
        group_sound.addChild(label_sound_low);
        Label label_sound_high = new Label(AbstractOptionsMenu.i18n("high"), Skin.getSkin().getEditFont());
        group_sound.addChild(label_sound_high);
        CheckBox cb_sound = new CheckBox(audioSettings.play_sfx, AbstractOptionsMenu.i18n("sound_effects"));
        group_sound.addChild(cb_sound);
        Label label_sound = new Label(AbstractOptionsMenu.i18n("sound_effects_volume"), Skin.getSkin().getEditFont());
        group_sound.addChild(label_sound);

        Slider slider_sound = new Slider(SLIDER_WIDTH, 0, MAX_VALUE, (int) (audioSettings.sound_gain * MAX_VALUE));
        slider_sound.setDisabled(!cb_sound.isMarked());
        group_sound.addChild(slider_sound);

        cb_sound.addCheckBoxListener(marked -> {
            audioSettings.play_sfx = marked;
            audioManager.setSfxEnabled(marked);
            slider_sound.setDisabled(!marked);
        });
        slider_sound.addValueListener(value -> {
            float sound_gain = (float) value / MAX_VALUE;
            audioSettings.sound_gain = sound_gain;
            audioManager.setSfxGain(sound_gain);
        });

        cb_sound.place();
        label_sound.place(cb_sound, BOTTOM_LEFT);
        label_sound_low.place(label_sound, BOTTOM_LEFT);
        slider_sound.place(label_sound_low, RIGHT_MID);
        label_sound_high.place(slider_sound, RIGHT_MID);
        group_sound.compileCanvas();
        group_sound.setDisabled(!audioCreated);

        // Audio Output
        Group group_output = new Group();
        addChild(group_output);
        Label label_output = new Label(AbstractOptionsMenu.i18n("audio_output"), Skin.getSkin().getEditFont());
        group_output.addChild(label_output);

        PulldownMenu<Boolean> pm_output = new PulldownMenu<>();
        pm_output.addItem(new PulldownItem<>(AbstractOptionsMenu.i18n("audio_output_speakers"), Boolean.FALSE));
        pm_output.addItem(new PulldownItem<>(AbstractOptionsMenu.i18n("audio_output_headphones"), Boolean.TRUE));

        int initialOutput = audioSettings.headphone_mode ? 1 : 0;
        PulldownButton<Boolean> pb_output = new PulldownButton<>(gui_root, pm_output, initialOutput, 150);
        group_output.addChild(pb_output);

        CheckBox cb_visual_alerts = new CheckBox(accessibilitySettings.sound_emojis,
                AbstractOptionsMenu.i18n("sound_emojis"),
                AbstractOptionsMenu.i18n("sound_emojis_tip"));
        group_output.addChild(cb_visual_alerts);
        cb_visual_alerts.addCheckBoxListener(marked -> {
            accessibilitySettings.sound_emojis = marked;
        });

        label_output.place();
        pb_output.place(label_output, RIGHT_MID);
        cb_visual_alerts.place(label_output, BOTTOM_LEFT);

        boolean hrtfSupported = audioManager.isHRTFSupported();

        if (hrtfSupported) {
            pm_output.addItemChosenListener((_, _) -> {
                boolean headphone = pm_output.getChosenItem().map(PulldownItem::getAttachment).orElse(Boolean.FALSE);
                audioSettings.headphone_mode = headphone;
                audioManager.setHeadphoneMode(headphone);
            });
        } else {
            pb_output.setDisabled(true);
            pm_output.chooseItem(0);
        }

        group_output.compileCanvas();

        // Placement
        group_music.place();
        group_sound.place(group_music, BOTTOM_LEFT);
        group_output.place(group_sound, BOTTOM_LEFT);
        compileCanvas();
    }
}
