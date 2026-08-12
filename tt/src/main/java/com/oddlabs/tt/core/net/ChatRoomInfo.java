package com.oddlabs.tt.core.net;

import com.oddlabs.matchmaking.ChatRoomUser;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Data record holding information about a matchmaking chat room.
 */
public record ChatRoomInfo(@NonNull String name, @NonNull ChatRoomUser @Nullable [] users) {
}
