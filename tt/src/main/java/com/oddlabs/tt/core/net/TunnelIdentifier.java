package com.oddlabs.tt.core.net;

import com.oddlabs.matchmaking.Profile;
import com.oddlabs.matchmaking.TunnelAddress;
import org.jspecify.annotations.NonNull;

public record TunnelIdentifier(Profile profile, TunnelAddress address) {


    @Override
    public @NonNull String toString() {
        return "profile: " + profile + " tunnel address: " + address;
    }
}
