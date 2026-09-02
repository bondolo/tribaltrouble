package com.oddlabs.tt.content.campaign;

import com.oddlabs.tt.content.menu.Menu;
import com.oddlabs.tt.gui.CancelButton;
import com.oddlabs.tt.gui.Form;
import com.oddlabs.tt.gui.MessageForm;
import com.oddlabs.tt.gui.ButtonObject;
import com.oddlabs.tt.gui.EditLine;
import com.oddlabs.tt.gui.FocusDirection;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.Group;
import com.oddlabs.tt.gui.Label;
import com.oddlabs.tt.gui.MouseButton;
import com.oddlabs.tt.gui.OKButton;
import com.oddlabs.tt.gui.Origin;
import com.oddlabs.tt.gui.PulldownButton;
import com.oddlabs.tt.gui.PulldownItem;
import com.oddlabs.tt.gui.PulldownMenu;
import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.gui.event.EnterListener;
import com.oddlabs.tt.gui.event.MouseClickListener;
import com.oddlabs.tt.base.util.Utils;
import com.oddlabs.tt.engine.render.Renderer;
import com.oddlabs.tt.simulation.model.Difficulty;
import com.oddlabs.tt.simulation.model.Race;
import com.oddlabs.util.DeterministicSerializerLoopbackInterface;
import org.jspecify.annotations.Nullable;

import java.io.FileNotFoundException;
import java.io.InvalidClassException;
import java.nio.file.NoSuchFileException;
import java.util.Arrays;
import java.util.ResourceBundle;

import static com.oddlabs.tt.gui.Placement.BOTTOM_LEFT;
import static com.oddlabs.tt.gui.Placement.LEFT_MID;

/**
 * UI form for creating a new single-player campaign.
 */
public final class NewCampaignForm extends Form implements DeterministicSerializerLoopbackInterface<CampaignState[]> {
    private static final int BUTTON_WIDTH = 100;
    private static final int EDITLINE_WIDTH = 240;

    private static final int INDEX_VIKINGS = 0;
    private static final int INDEX_NATIVES = 1;

    private static final ResourceBundle bundle = ResourceBundle.getBundle(NewCampaignForm.class.getName());

    private static String i18n(String key, Object... args) {
        return Utils.getBundleString(bundle, key, args);
    }

    private final GUIRoot gui_root;
    private final Menu main_menu;
    private final CampaignForm campaign_form;
    private final EditLine editline_name = new EditLine(EDITLINE_WIDTH, 200);
    private final PulldownMenu<Race> race_pulldown = new PulldownMenu<>();
    private final PulldownMenu<Difficulty> difficulty_pulldown = new PulldownMenu<>();
    private CampaignState @Nullable [] campaign_states;

    public NewCampaignForm(Menu main_menu,
            CampaignForm campaign_form) {
        this.main_menu = main_menu;
        this.gui_root = main_menu.getGUIRoot();
        this.campaign_form = campaign_form;
        // headline
        Label label_headline = new Label(i18n("caption"), Skin.getSkin().getHeadlineFont());
        addChild(label_headline);

        // name
        Group group = new Group();
        Label name_label = new Label(i18n("name"), Skin.getSkin().getEditFont());
        editline_name.addEnterListener(new NameListener());
        group.addChild(name_label);
        group.addChild(editline_name);

        // race
        Label race_label = new Label(i18n("race"), Skin.getSkin().getEditFont());
        race_pulldown.addItem(new PulldownItem<>(i18n("vikings"), Race.VIKINGS));
        race_pulldown.addItem(new PulldownItem<>(i18n("natives"), Race.NATIVES));
        race_pulldown.addItemChosenListener((PulldownMenu<Race> menu, int item_index) -> {
            if (menu.getChosenItem().map(PulldownItem::getAttachment).orElse(Race.VIKINGS) == Race.NATIVES
                    && (!Renderer.getRenderer().getSettings().has_native_campaign)) {
                menu.chooseItem(INDEX_VIKINGS);
                gui_root.addModalForm(new MessageForm(i18n("native_unavailable")));
            }
        });
        PulldownButton<Race> race_pb = new PulldownButton<>(gui_root, race_pulldown, INDEX_VIKINGS, 100);
        group.addChild(race_label);
        group.addChild(race_pb);

        // difficulty
        Label difficulty_label = new Label(i18n("difficulty"), Skin.getSkin().getEditFont());
        difficulty_pulldown.addItem(new PulldownItem<>(i18n("easy"), Difficulty.EASY));
        difficulty_pulldown.addItem(new PulldownItem<>(i18n("normal"), Difficulty.NORMAL));
        difficulty_pulldown.addItem(new PulldownItem<>(i18n("hard"), Difficulty.HARD));
        PulldownButton<Difficulty> difficulty_pb = new PulldownButton<>(gui_root, difficulty_pulldown, 1, 100);
        group.addChild(difficulty_label);
        group.addChild(difficulty_pb);

        // place in group
        editline_name.place();
        name_label.place(editline_name, LEFT_MID);
        race_pb.place(editline_name, BOTTOM_LEFT);
        race_label.place(race_pb, LEFT_MID);
        difficulty_pb.place(race_pb, BOTTOM_LEFT);
        difficulty_label.place(difficulty_pb, LEFT_MID);
        group.compileCanvas();

        addChild(group);

        // buttons
        ButtonObject button_ok = new OKButton(BUTTON_WIDTH);
        button_ok.addMouseClickListener(new NameListener());
        addChild(button_ok);
        ButtonObject button_cancel = new CancelButton(BUTTON_WIDTH);
        button_cancel.addMouseClickListener((_, _, _, _) -> this.cancel());
        addChild(button_cancel);

        // place
        label_headline.place();
        group.place(label_headline, BOTTOM_LEFT);

        button_cancel.place(Origin.AT_END);
        button_ok.place(button_cancel, LEFT_MID);

        compileCanvas();
        centerPos();
        LoadCampaignBox.loadSavegames(main_menu.getEngine(), this);
    }

    @Override
    protected void doCancel() {
        main_menu.setMenu(campaign_form);
    }

    @Override
    public void setFocus(FocusDirection direction) {
        if (direction == FocusDirection.BACKWARD) {
            super.setFocus(direction);
        } else {
            editline_name.setFocus(direction);
        }
    }

    private boolean nameIsUnique(String name) {
        return campaign_states == null || Arrays.stream(campaign_states)
                .map(CampaignState::getName)
                .noneMatch(campaign_name -> campaign_name.equals(name));
    }

    private void save() {
        String name = editline_name.getContents().trim();
        if (name.isEmpty()) {
            gui_root.addModalForm(new MessageForm(i18n("invalid")));
            return;
        }
        if (!nameIsUnique(name)) {
            gui_root.addModalForm(new MessageForm(i18n("exists")));
            return;
        }

        CampaignState[] new_states = campaign_states != null
                ? Arrays.copyOf(campaign_states, campaign_states.length + 1)
                : new CampaignState[1];
        Campaign campaign;
        Race chosenRace = race_pulldown.getChosenItem().map(PulldownItem::getAttachment).orElse(Race.VIKINGS);
        switch (chosenRace) {
            case VIKINGS -> {
                campaign = new VikingCampaign(gui_root, main_menu.getAudioManager());
                campaign.getState().setRace(Race.VIKINGS);
            }
            case NATIVES -> {
                campaign = new NativeCampaign(gui_root, main_menu.getAudioManager());
                campaign.getState().setRace(Race.NATIVES);
            }
            default -> throw new IllegalArgumentException();
        }
        campaign.getState().setName(name);
        campaign.getState().setDate(System.currentTimeMillis());

        Difficulty difficulty = difficulty_pulldown.getChosenItem().map(PulldownItem::getAttachment).orElse(
                Difficulty.NORMAL);
        campaign.getState().setDifficulty(difficulty);
        new_states[new_states.length - 1] = campaign.getState();
        LoadCampaignBox.saveSavegames(main_menu.getEngine(), new_states, this);
        remove();
    }

    @Override
    public void loadSucceeded(CampaignState[] campaign_states) {
        this.campaign_states = campaign_states;
    }

    @Override
    public void failed(Throwable e) {
        if (e instanceof FileNotFoundException || e instanceof NoSuchFileException) {
        } else if (e instanceof InvalidClassException) {
        } else {
            String failed_message = i18n("failed_message", LoadCampaignBox.SAVEGAMES_FILE_NAME, e.getMessage());
            gui_root.addModalForm(new MessageForm(failed_message));
        }
    }

    private final class NameListener implements MouseClickListener, EnterListener {
        @Override
        public void mouseClicked(MouseButton button, int x, int y, int clicks) {
            save();
        }

        @Override
        public void enterPressed(CharSequence text) {
            save();
        }
    }
}
