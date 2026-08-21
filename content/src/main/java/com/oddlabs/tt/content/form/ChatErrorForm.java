package com.oddlabs.tt.content.form;

import com.oddlabs.matchmaking.MatchmakingClientInterface;
import com.oddlabs.tt.base.util.Utils;
import com.oddlabs.tt.gui.MessageForm;

import java.util.ResourceBundle;

public final class ChatErrorForm extends MessageForm {
    private static final ResourceBundle bundle = ResourceBundle.getBundle(ChatErrorForm.class.getName());

    private static String i18n(String key, Object... args) {
        return Utils.getBundleString(bundle, key, args);
    }

    private static String getErrorFromCode(int error_code) {
        return i18n(switch (error_code) {
            case MatchmakingClientInterface.CHAT_ERROR_TOO_MANY_USERS -> "chat_error_too_many_users";
            case MatchmakingClientInterface.CHAT_ERROR_INVALID_NAME -> "chat_error_invalid_name";
            case MatchmakingClientInterface.CHAT_ERROR_NO_SUCH_NICK -> "chat_error_no_such_nick";
            default -> throw new IllegalArgumentException("Unknown error code: " + error_code);
        });
    }

    public ChatErrorForm(int error_code) {
        super(getErrorFromCode(error_code));
    }
}
