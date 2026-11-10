package com.oddlabs.tt.client.gui;

import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.NonFocusIconButton;
import com.oddlabs.tt.engine.render.IconQuad;
import com.oddlabs.tt.engine.render.ModeIconQuads;
import com.oddlabs.tt.input.GameAction;
import com.oddlabs.tt.simulation.model.MagicType;
import com.oddlabs.tt.simulation.model.Unit;
import com.oddlabs.tt.engine.render.GUIRenderer;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Button representing a magical ability that charges over time and allows triggering magic.
 */
public final class RechargeButton extends NonFocusIconButton {
    private final MagicType magic_type;
    private final GUIIcons icons;
    private @Nullable Unit unit;

    public RechargeButton(GUIRoot guiRoot, ModeIconQuads icon,
            @Nullable GameAction action, Supplier<String> tool_tip, MagicType magic_type, GUIIcons icons) {
        super(guiRoot, icon, action, tool_tip);
        this.magic_type = magic_type;
        this.icons = icons;
        setCanFocus(true);
        var normal = icon.quad(ModeIconQuads.Mode.NORMAL);
        setDim(normal.getWidth(), normal.getHeight());
    }

    public final void setUnit(Unit unit) {
        this.unit = unit;
    }

    @Override
    protected final void postRender(GUIRenderer renderer) {
        if (unit == null) {
            return;
        }
        float progress = unit.getMagicProgress(magic_type);
        if (!unit.isDead() && progress < 1f) {
            IconQuad watchQuad = icons.getWatch(progress);
            renderer.drawIcon(watchQuad, getWidth() - watchQuad.getWidth(), getHeight() - watchQuad.getHeight());
        }
    }
}
