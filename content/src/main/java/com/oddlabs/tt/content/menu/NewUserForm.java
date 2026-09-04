package com.oddlabs.tt.content.menu;

import com.oddlabs.matchmaking.Login;
import com.oddlabs.matchmaking.LoginDetails;
import com.oddlabs.tt.net.AccountSettings;
import com.oddlabs.tt.gui.MessageForm;
import com.oddlabs.tt.gui.ButtonObject;
import com.oddlabs.tt.gui.CancelButton;
import com.oddlabs.tt.gui.EditLine;
import com.oddlabs.tt.gui.FocusDirection;
import com.oddlabs.tt.gui.Form;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.Group;
import com.oddlabs.tt.gui.HorizButton;
import com.oddlabs.tt.gui.Label;
import com.oddlabs.tt.gui.MouseButton;
import com.oddlabs.tt.gui.Origin;
import com.oddlabs.tt.gui.PasswordLine;
import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.gui.event.EnterListener;
import com.oddlabs.tt.gui.event.MouseClickListener;
import com.oddlabs.tt.base.util.Utils;

import java.util.ResourceBundle;

import static com.oddlabs.tt.gui.Placement.BOTTOM_LEFT;
import static com.oddlabs.tt.gui.Placement.BOTTOM_RIGHT;
import static com.oddlabs.tt.gui.Placement.LEFT_MID;
import static com.oddlabs.tt.gui.Placement.RIGHT_MID;

public final class NewUserForm extends Form {
    private static final int MIN_PASSWORD_LENGTH = 6;

    private static final int BUTTON_WIDTH = 100;
    private static final int BUTTON_WIDTH_LONG = 150;
    private static final int EDITLINE_WIDTH = 240;

    private final Menu main_menu;
    private final EditLine editline_username;
    private final EditLine editline_email;
    private final PasswordLine editline_password;
    private final PasswordLine editline_verify;
    private static final ResourceBundle bundle = ResourceBundle.getBundle(NewUserForm.class.getName());

    private String i18n(String key, Object... args) {
        return Utils.getBundleString(bundle, key, args);
    }

    private final GUIRoot gui_root;

    public NewUserForm(Menu main_menu) {
        this.main_menu = main_menu;
        this.gui_root = main_menu.getGUIRoot();

        CreateUserListener create_listener = new CreateUserListener();
        // headline
        Label label_headline = new Label(i18n("create_new_user_caption"), Skin.getSkin().getHeadlineFont());
        addChild(label_headline);

        // login
        Group login_group = new Group();
        Label label_username = new Label(i18n("user_name"), Skin.getSkin().getEditFont());
        editline_email = new EditLine(EDITLINE_WIDTH, 255);
        editline_email.addEnterListener(create_listener);
        Label label_email = new Label(i18n("email"), Skin.getSkin().getEditFont());
        editline_username = new EditLine(EDITLINE_WIDTH, 255);
        editline_username.addEnterListener(create_listener);
        Label label_password = new Label(i18n("password"), Skin.getSkin().getEditFont());
        editline_password = new PasswordLine(EDITLINE_WIDTH, 255);
        editline_password.addEnterListener(create_listener);
        Label label_verify = new Label(i18n("reenter_password"), Skin.getSkin().getEditFont());
        editline_verify = new PasswordLine(EDITLINE_WIDTH, 255);
        editline_verify.addEnterListener(create_listener);
        login_group.addChild(label_username);
        login_group.addChild(editline_username);
        login_group.addChild(label_email);
        login_group.addChild(editline_email);
        login_group.addChild(label_password);
        login_group.addChild(editline_password);
        login_group.addChild(label_password);
        login_group.addChild(editline_verify);
        login_group.addChild(label_verify);

        label_username.place(label_headline, BOTTOM_LEFT);
        editline_username.place(label_username, RIGHT_MID);
        editline_email.place(editline_username, BOTTOM_RIGHT);
        label_email.place(editline_email, LEFT_MID);
        editline_password.place(editline_email, BOTTOM_RIGHT);
        label_password.place(editline_password, LEFT_MID);
        editline_verify.place(editline_password, BOTTOM_RIGHT);
        label_verify.place(editline_verify, LEFT_MID);
        login_group.compileCanvas();

        addChild(login_group);

        // warning
        Label label_one_user = new Label(i18n("one_user_per_key"), Skin.getSkin().getEditFont());
        addChild(label_one_user);

        // buttons
        Group group_buttons = new Group();


        ButtonObject button_create = new HorizButton(i18n("create_user"), BUTTON_WIDTH_LONG);
        button_create.addMouseClickListener(create_listener);
        ButtonObject button_cancel = new CancelButton(BUTTON_WIDTH);
        button_cancel.addMouseClickListener((_, _, _, _) -> this.cancel());

        group_buttons.addChild(button_create);
        group_buttons.addChild(button_cancel);

        button_cancel.place();
        button_create.place(button_cancel, LEFT_MID);

        group_buttons.compileCanvas();
        addChild(group_buttons);

        // Place objects

        // headline
        label_headline.place();
        label_one_user.place(label_headline, BOTTOM_LEFT);
        login_group.place(label_one_user, BOTTOM_LEFT);


        group_buttons.place(Origin.AT_END);

        compileCanvas();
    }

    @Override
    public void setFocus(FocusDirection direction) {
        if (direction == FocusDirection.BACKWARD) {
            super.setFocus(direction);
        } else {
            editline_username.setFocus(direction);
        }
    }

    private void createUser() {
        String username = editline_username.getContents();
        String password = editline_password.getPasswordDigest();
        LoginDetails login_details = new LoginDetails(editline_email.getContents());
        if (!editline_password.getContents().equals(editline_verify.getContents())) {
            gui_root.addModalForm(new MessageForm(i18n("no_match")));
            editline_password.clear();
            editline_verify.clear();
        } else if (editline_password.getContents().length() < MIN_PASSWORD_LENGTH) {
            String min_length_err = i18n("min_length_error", MIN_PASSWORD_LENGTH);
            gui_root.addModalForm(new MessageForm(min_length_err));
        } else if (!login_details.isValid()) {
            gui_root.addModalForm(new MessageForm(i18n("invalid_email")));
        } else {
            Login login = new Login(username, password);
            if (!login.isValid())
                gui_root.addModalForm(new MessageForm(i18n("invalid_login")));
            else
                doCreateUser(username, login_details, password, login);
        }
    }

    private void doCreateUser(String username, LoginDetails login_details, String password,
            Login login) {
        var settings = main_menu.getGUIRoot().getGUI().getEngine().getSettings();
        var account = AccountSettings.from(settings);
        account.username = username;
        account.pw_digest = password;
        Form connecting_form = new MatchmakingConnectingForm(this, main_menu, login, login_details);
        gui_root.addModalForm(connecting_form);
    }

    private final class CreateUserListener implements MouseClickListener, EnterListener {
        @Override
        public void mouseClicked(MouseButton button, int x, int y, int clicks) {
            createUser();
        }

        @Override
        public void enterPressed(CharSequence text) {
            createUser();
        }
    }
}
