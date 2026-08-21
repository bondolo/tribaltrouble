package com.oddlabs.tt.net;

import com.oddlabs.matchmaking.ChatRoomUser;
import org.jspecify.annotations.Nullable;

/**
 * Data record holding information about a matchmaking chat room.
 */
public record ChatRoomInfo(String name, ChatRoomUser @Nullable [] users) {
}
