package com.oddlabs.tt.gui;

import com.oddlabs.tt.engine.render.ModeIconQuads;
import com.oddlabs.tt.input.GameAction;
import com.oddlabs.tt.engine.render.GUIRenderer;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Clickable button displaying a mode-dependent icon quad.
 */
public class IconButton extends ButtonObject {
    private final ModeIconQuads icon;
    private @Nullable IconDisabler icon_disabler;

    public IconButton(GUIRoot guiRoot, ModeIconQuads icon, @Nullable Supplier<String> tool_tip) {
        this(guiRoot, icon, null, tool_tip);
    }

    public IconButton(GUIRoot guiRoot, ModeIconQuads icon, @Nullable GameAction action, @Nullable Supplier<
            String> tool_tip) {
        super(guiRoot.getSkin().getEditFont(), action, tool_tip);
        this.icon = icon;
        var normal = icon.quad(ModeIconQuads.Mode.NORMAL);
        setDim(normal.getWidth(), normal.getHeight());
    }

    public final void setIconDisabler(@Nullable IconDisabler icon_disabler) {
        this.icon_disabler = icon_disabler;
    }

    public final void doUpdate() {
        setDisabled(icon_disabler != null && icon_disabler.isDisabled());
    }

    protected ModeIconQuads getIcon() {
        return icon;
    }

    @Override
    protected void renderGeometry(GUIRenderer renderer) {
        ModeIconQuads.Mode skinMode = isDisabled()
                ? ModeIconQuads.Mode.DISABLED
                : isHovered() || isActive()
                        ? ModeIconQuads.Mode.ACTIVE
                : ModeIconQuads.Mode.NORMAL;

        renderer.drawModeIcon(icon, skinMode, 0, 0);
    }
}
