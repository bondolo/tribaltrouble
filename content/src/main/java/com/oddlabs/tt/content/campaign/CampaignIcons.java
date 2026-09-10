package com.oddlabs.tt.content.campaign;

import com.oddlabs.tt.content.campaign.natives.NativeCampaignIcons;
import com.oddlabs.tt.content.campaign.viking.VikingCampaignIcons;
import com.oddlabs.tt.gui.GUIIcon;
import com.oddlabs.tt.engine.render.IconQuad;

/**
 * Provides access to icon resources, maps, and island definitions for a campaign.
 */
public sealed interface CampaignIcons permits NativeCampaignIcons, VikingCampaignIcons {
    GUIIcon[] getHiddenRoutes();

    IconQuad[] getFaces();

    IconQuad getMap();

    int getNumIslands();

    MapIslandData getMapIslandData(int i);
}
