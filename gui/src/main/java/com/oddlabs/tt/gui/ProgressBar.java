package com.oddlabs.tt.gui;

import com.oddlabs.tt.base.util.Utils;
import com.oddlabs.tt.engine.render.GUIRenderer;
import com.oddlabs.tt.engine.render.IconQuad;
import com.oddlabs.tt.engine.render.ModeIconQuads;
import com.oddlabs.tt.gui.render.TextLineRenderer;
import com.oddlabs.util.Color;

import java.util.ResourceBundle;

/**
 * UI progress bar component rendering normalized progress from 0.0 to 1.0.
 */
public final class ProgressBar extends GUIObject {
    private static final ResourceBundle bundle = ResourceBundle.getBundle(ProgressBar.class.getName());

    private String i18n(String key, Object... args) {
        return Utils.getBundleString(bundle, key, args);
    }

    private final boolean text_only;
    private int left_margin;
    private int right_margin;
    private float progress;

    public ProgressBar(int width, boolean text_only) {
        this.text_only = text_only;
        if (text_only) {
            setDim(width, Skin.getSkin().getHeadlineFont().getHeight());
        } else {
            ProgressBarData data = Skin.getSkin().getProgressBarData();
            left_margin = data.leftFill().quad(ModeIconQuads.Mode.NORMAL).getWidth();
            right_margin = data.rightFill().quad(ModeIconQuads.Mode.NORMAL).getWidth();

            assert width > left_margin + right_margin : "Progress bar too small.";
            setDim(width, data.progressBar().getHeight());
        }
        setCanFocus(false);
    }

    public void setProgress(float fraction) {
        this.progress = Math.clamp(fraction, 0f, 1f);
    }

    public float getProgress() {
        return progress;
    }

    private void renderText(GUIRenderer renderer) {
        int percentage = (int) (progress * 100);
        String string = i18n("loading", percentage);
        TextLineRenderer.render(renderer, Skin.getSkin().getHeadlineFont(), string, 0, 0, Float.NEGATIVE_INFINITY,
                Float.POSITIVE_INFINITY, Color.Linear.WHITE);
    }

    @Override
    protected void renderGeometry(GUIRenderer renderer) {
        if (text_only) {
            renderText(renderer);
        } else {
            Skin.getSkin().getProgressBarData().progressBar()
                    .render(renderer, 0, 0, getWidth(), ModeIconQuads.Mode.NORMAL);
            renderFill(renderer, 0);
        }
    }

    private void renderFill(GUIRenderer renderer, int y) {
        ProgressBarData data = Skin.getSkin().getProgressBarData();
        ModeIconQuads left = data.leftFill();
        ModeIconQuads center = data.centerFill();
        ModeIconQuads right = data.rightFill();

        renderer.drawModeIcon(left, ModeIconQuads.Mode.NORMAL, 0, y);

        int available = getWidth() - left_margin - right_margin;
        int width = (int) (progress * available);

        if (width > 0) {
            int current_pos = left_margin + width;
            IconQuad c = center.quad(ModeIconQuads.Mode.NORMAL);
            renderer.drawTexture(c.getTexture(), left_margin, y, width, c.getHeight(), c.getU1(), c.getV1(), c.getU2(),
                    c.getV2(), Color.Standard.WHITE);
            renderer.drawModeIcon(right, ModeIconQuads.Mode.NORMAL, current_pos, y);
        }
    }
}
