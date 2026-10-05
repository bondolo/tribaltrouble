package com.oddlabs.tt.content.campaign;

import com.oddlabs.tt.engine.render.IconQuad;
import com.oddlabs.tt.gui.Origin;
import com.oddlabs.tt.client.viewer.WorldViewer;
import org.jspecify.annotations.Nullable;

/** In-game dialog form for campaign events and dialogues. */
public final class InGameCampaignDialogForm extends CampaignDialogForm {
    public InGameCampaignDialogForm(WorldViewer viewer, CharSequence header,
            CharSequence text, IconQuad image, Origin align) {
        this(viewer, header, text, image, align, null);
    }

    public InGameCampaignDialogForm(WorldViewer viewer, CharSequence header,
            CharSequence text, IconQuad image, Origin align, @Nullable Runnable runnable) {
        this(viewer, header, text, image, align, runnable, false);
    }

    public InGameCampaignDialogForm(WorldViewer viewer, CharSequence header,
            CharSequence text, IconQuad image, Origin align, @Nullable Runnable runnable,
            boolean cancel) {
        super(viewer.getGUIRoot(), header, text, image, align, runnable, cancel);
        viewer.setPaused(true);
        addCloseListener(() -> viewer.setPaused(false));
    }
}
