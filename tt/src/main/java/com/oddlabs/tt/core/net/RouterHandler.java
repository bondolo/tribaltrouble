package com.oddlabs.tt.core.net;

import com.oddlabs.router.RouterClientInterface;

public interface RouterHandler extends RouterClientInterface {
    void routerFailed(Exception e);
}
