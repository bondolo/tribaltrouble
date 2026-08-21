package com.oddlabs.tt.net;

import com.oddlabs.matchmaking.Profile;
import com.oddlabs.matchmaking.TunnelAddress;

public record TunnelIdentifier(Profile profile, TunnelAddress address) {


    @Override
    public String toString() {
        return "profile: " + profile + " tunnel address: " + address;
    }
}
