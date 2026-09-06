package com.oddlabs.tt.content.campaign;

import com.oddlabs.tt.gui.GUIIcon;
import com.oddlabs.tt.engine.render.IconQuad;

/**
 * Provides access to icon resources, maps, and island definitions for a campaign.
 */
interface CampaignIcons {
    GUIIcon[] getHiddenRoutes();

    IconQuad[] getFaces();

    IconQuad getMap();

    int getNumIslands();

    MapIslandData getMapIslandData(int i);
}
