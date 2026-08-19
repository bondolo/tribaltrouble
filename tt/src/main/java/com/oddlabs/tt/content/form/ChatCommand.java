package com.oddlabs.tt.content.form;

import com.oddlabs.tt.gui.*;
import com.oddlabs.tt.gui.event.*;
import com.oddlabs.tt.client.gui.*;

import com.oddlabs.tt.net.ChatMethod;
import com.oddlabs.tt.net.MatchmakingClient;
import com.oddlabs.tt.base.util.InfoPrinter;
import com.oddlabs.tt.base.util.Utils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

/** Processor for user slash-commands in chat windows. */
public final class ChatCommand {
    private static final Map<String, ChatMethod> commands = Map.of(
            "message", ChatCommand::sendMessage,
            "msg", ChatCommand::sendMessage,
            "tell", ChatCommand::sendMessage,
            "whisper", ChatCommand::sendMessage,
            "info", ChatCommand::getInfo,
            "finger", ChatCommand::getInfo,
            "ignore", ChatCommand::ignore,
            "unignore", ChatCommand::unignore,
            "ignorelist", ChatCommand::ignoreList);

    private static final Set<String> ignored_nicks = new HashSet<>();

    private static final ResourceBundle bundle = ResourceBundle.getBundle(ChatCommand.class.getName());

    private static @NonNull String i18n(@NonNull String key, @NonNull Object @NonNull... args) {
        return Utils.getBundleString(bundle, key, args);
    }

    public static boolean filterCommand(@NonNull InfoPrinter info_printer, @NonNull MatchmakingClient client,
            @NonNull String text) {
        return filterCommand(info_printer, client, Collections.emptyMap(), text);
    }

    public static boolean filterCommand(@NonNull InfoPrinter info_printer, @NonNull MatchmakingClient client,
            @NonNull Map<@NonNull String, @NonNull ChatMethod> custom_commands, @NonNull String text) {
        if (!text.startsWith("/"))
            return false;
        int fist_space = firstSpace(text);
        String cmd = text.substring(1, fist_space);
        String args = text.substring(fist_space).trim();
        ChatMethod method = custom_commands.getOrDefault(cmd, commands.get(cmd));
        if (method != null) {
            method.execute(info_printer, client, args);
        } else {
            String unknown_cmd_message = i18n("unknown_command", cmd);
            info_printer.print(unknown_cmd_message);
        }
        return true;
    }

    private static int firstSpace(@NonNull String text) {
        int first_space = text.indexOf(' ');
        return first_space == -1 ? text.length() : first_space;
    }

    private static void sendMessage(@NonNull InfoPrinter info_printer, @NonNull MatchmakingClient client,
            @NonNull String text) {
        int first_space = firstSpace(text);
        String nick = text.substring(0, first_space);
        String message = text.substring(first_space).trim();
        if (!client.isConnected())
            info_printer.print(i18n("not_connected"));
        else
            client.sendPrivateMessage(nick, message);
    }

    private static void getInfo(@NonNull InfoPrinter info_printer, @NonNull MatchmakingClient client,
            @NonNull String text) {
        int first_space = firstSpace(text);
        String nick = text.substring(0, first_space);
        if (!client.isConnected())
            info_printer.print(i18n("not_connected"));
        else
            client.requestInfo(nick);
    }

    public static void ignore(@NonNull InfoPrinter info_printer, @NonNull String text) {
        ignore(info_printer, null, text);
    }

    public static void ignore(@NonNull InfoPrinter info_printer, @Nullable MatchmakingClient client,
            @NonNull String text) {
        int first_space = firstSpace(text);
        String nick = text.substring(0, first_space);
        boolean result = ignored_nicks.add(nick.toLowerCase());
        if (result) {
            String msg = i18n("ignoring", nick);
            info_printer.print(msg);
        }
    }

    public static void unignore(@NonNull InfoPrinter info_printer, @NonNull String text) {
        unignore(info_printer, null, text);
    }

    public static void unignore(@NonNull InfoPrinter info_printer, @Nullable MatchmakingClient client,
            @NonNull String text) {
        int first_space = firstSpace(text);
        String nick = text.substring(0, first_space);
        boolean result = ignored_nicks.remove(nick.toLowerCase());
        if (result) {
            String msg = i18n("unignoring", nick);
            info_printer.print(msg);
        }
    }

    public static boolean isIgnoring(@NonNull String nick) {
        return ignored_nicks.contains(nick.toLowerCase());
    }

    private static void ignoreList(@NonNull InfoPrinter info_printer, @Nullable MatchmakingClient client, String text) {
        String[] nicks = new String[ignored_nicks.size()];
        ignored_nicks.toArray(nicks);
        String result;
        if (nicks.length == 0) {
            result = i18n("ignore_list_empty");
        } else {
            result = i18n("ignore_list");
            result += " " + String.join(" ", nicks);

        }
        info_printer.print(result);
    }

    private ChatCommand() {
    }
}
