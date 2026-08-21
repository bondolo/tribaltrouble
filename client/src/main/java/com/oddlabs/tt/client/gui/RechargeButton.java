package com.oddlabs.tt.client.gui;

import com.oddlabs.tt.gui.MouseButton;
import com.oddlabs.tt.gui.NonFocusIconButton;
import com.oddlabs.tt.engine.render.IconQuad;
import com.oddlabs.tt.engine.render.ModeIconQuads;
import com.oddlabs.tt.input.GameAction;
import com.oddlabs.tt.simulation.model.MagicType;
import com.oddlabs.tt.simulation.model.Unit;
import com.oddlabs.tt.simulation.player.PlayerInterface;
import com.oddlabs.tt.engine.render.GUIRenderer;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/**
 * A button representing a magical ability that charges over time and allows triggering magic.
 */
public class RechargeButton extends NonFocusIconButton {
    private final PlayerInterface player_interface;
    private final MagicType magic_type;
    private Unit unit;

    public RechargeButton(PlayerInterface player_interface, ModeIconQuads icon,
            @Nullable GameAction action, Supplier<String> tool_tip, MagicType magic_type) {
        super(icon, action, tool_tip);
        this.player_interface = player_interface;
        this.magic_type = magic_type;
        setCanFocus(true);
        var normal = icon.quad(ModeIconQuads.Mode.NORMAL);
        setDim(normal.getWidth(), normal.getHeight());
    }

    public final void setUnit(Unit unit) {
        this.unit = unit;
    }

    @Override
    public final void mouseClicked(MouseButton button, int x, int y, int clicks) {
        if (unit.canDoMagic(magic_type))
            player_interface.doMagic(unit, magic_type);
    }

    @Override
    protected final void postRender(GUIRenderer renderer) {
        float progress = unit.getMagicProgress(magic_type);
        if (!unit.isDead() && progress < 1f) {
            IconQuad watchQuad = GUIIcons.getIcons().getWatch(progress);
            renderer.drawIcon(watchQuad, getWidth() - watchQuad.getWidth(), getHeight() - watchQuad.getHeight());
        }
    }
}
