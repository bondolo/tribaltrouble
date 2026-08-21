package com.oddlabs.tt.client.gui;

import com.oddlabs.tt.gui.GUIIcon;
import com.oddlabs.tt.engine.render.IconQuad;

public interface CampaignIcons {
    GUIIcon[] getHiddenRoutes();

    IconQuad[] getFaces();

    IconQuad getMap();

    int getNumIslands();

    //	public int getOffsetX();
//	public int getOffsetY();
//	public int getInternalWidth();
//	public int getInternalHeight();
    MapIslandData getMapIslandData(int i);
}
