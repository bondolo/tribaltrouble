package com.oddlabs.matchmaking;

import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;

public final class ChatRoomUser implements Serializable {
    @Serial
    private static final long serialVersionUID = 1;

    private final String nick;
    private final boolean playing;

    public ChatRoomUser(String nick, boolean playing) {
        this.nick = nick;
        this.playing = playing;
    }

    public String getNick() {
        return nick;
    }

    public boolean isPlaying() {
        return playing;
    }

    @Override
    public int hashCode() {
        return nick.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object other) {
        return other instanceof ChatRoomUser user && user.nick.equals(nick);
    }
}
