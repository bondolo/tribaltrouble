package com.oddlabs.tt.content.campaign;

import com.oddlabs.tt.gui.Form;
import com.oddlabs.tt.gui.CancelButton;
import com.oddlabs.tt.gui.FocusDirection;
import com.oddlabs.tt.gui.GUIIcon;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.HorizButton;
import com.oddlabs.tt.engine.render.IconQuad;
import com.oddlabs.tt.gui.Label;
import com.oddlabs.tt.gui.LabelBox;
import com.oddlabs.tt.gui.OKButton;
import com.oddlabs.tt.gui.Origin;
import com.oddlabs.tt.input.GameAction;
import com.oddlabs.tt.input.InputEvent;
import com.oddlabs.tt.input.InputPhase;
import org.jspecify.annotations.Nullable;

import java.util.logging.Level;
import java.util.logging.Logger;

import static com.oddlabs.tt.gui.Placement.LEFT_MID;
import static com.oddlabs.tt.gui.Placement.RIGHT_MID;
import static com.oddlabs.tt.gui.Placement.TOP_LEFT;

/** Modal dialog form used in campaign missions to show narrative and objectives. */
public class CampaignDialogForm extends Form {
    private static final Logger logger = Logger.getLogger(CampaignDialogForm.class.getSimpleName());
    private static final int WIDTH = 300;

    private final GUIRoot gui_root;
    private final @Nullable Runnable runnable;
    private final boolean cancel;
    private boolean dismissed = false;

    private final HorizButton ok_button;

    public CampaignDialogForm(GUIRoot gui_root, CharSequence header, CharSequence text, @Nullable IconQuad image,
            Origin align) {
        this(gui_root, header, text, image, align, null);
    }

    public CampaignDialogForm(GUIRoot gui_root, CharSequence header, CharSequence text, @Nullable IconQuad image,
            Origin align, @Nullable Runnable runnable) {
        this(gui_root, header, text, image, align, runnable, false);
    }

    public CampaignDialogForm(GUIRoot gui_root, CharSequence header, CharSequence text, @Nullable IconQuad image,
            Origin align, @Nullable Runnable runnable, boolean cancel) {
        super(gui_root);
        this.gui_root = gui_root;
        this.runnable = runnable;
        this.cancel = cancel;
        this.ok_button = new OKButton(gui_root, 80) {
            @Override
            protected void handleInput(InputEvent event) {
                if (event.getPhase() == InputPhase.PRESSED && event.consumeAction(GameAction.UI_ACTIVATE)) {
                    if (dismissed) return;
                    dismissed = true;
                    run();
                    try {
                        CampaignDialogForm.this.remove();
                    } catch (Exception e) {
                        logger.log(Level.WARNING, "Failed to remove form", e);
                    }
                    event.consume();
                    return;
                }
                super.handleInput(event);
            }
        };
        buildForm(header, text, image, align, cancel);
        ok_button.addMouseClickListener((_, _, _, _) -> {
            if (dismissed) return;
            dismissed = true;
            run();
            try {
                remove();
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to remove form", e);
            }
        });
        ok_button.addInputListener(event -> {
            if (event.getPhase() == InputPhase.PRESSED) {
                if (event.consumeAction(GameAction.UI_CANCEL)) {
                    this.cancel();
                    event.consume();
                }
            }
        });
    }

    protected void run() {
        if (runnable != null)
            runnable.run();
    }

    @Override
    public void cancel() {
        if (dismissed) return;
        dismissed = true;
        super.cancel();
    }

    @Override
    protected final void doCancel() {
        if (!cancel)
            run();
    }

    private void buildForm(CharSequence header, CharSequence text, @Nullable IconQuad image,
            Origin align, boolean cancel) {
        GUIIcon gui_icon = null;
        if (image != null) {
            gui_icon = new GUIIcon(image);
            addChild(gui_icon);
        }
        Label header_label = new Label(header, getSkin().getHeadlineFont());
        addChild(header_label);
        LabelBox label_box = new LabelBox(text, getSkin().getEditFont(), WIDTH);
        addChild(label_box);
        addChild(ok_button);

        if (gui_icon != null) {
            gui_icon.place();
            label_box.place(gui_icon, align == Origin.AT_START ? RIGHT_MID : LEFT_MID);
        } else {
            label_box.place();
        }
        header_label.place(label_box, TOP_LEFT);
        ok_button.place(Origin.AT_END);
        if (cancel) {
            HorizButton cancel_button = new CancelButton(gui_root, 80);
            addChild(cancel_button);
            cancel_button.place(ok_button, RIGHT_MID);
            cancel_button.addMouseClickListener((_, _, _, _) -> this.cancel());
        }

        compileCanvas();
        centerPos();
    }

    @Override
    protected void handleInput(InputEvent event) {
        super.handleInput(event);
    }

    @Override
    public void setFocus(FocusDirection direction) {
        if (direction == FocusDirection.BACKWARD) {
            super.setFocus(direction);
        } else {
            ok_button.setFocus(direction);
        }
    }
}
