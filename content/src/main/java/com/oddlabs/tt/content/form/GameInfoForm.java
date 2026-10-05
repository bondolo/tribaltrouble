package com.oddlabs.tt.content.form;

import com.oddlabs.matchmaking.Game;
import com.oddlabs.tt.base.util.Utils;
import com.oddlabs.tt.gui.FocusDirection;
import com.oddlabs.tt.gui.Form;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.Group;
import com.oddlabs.tt.gui.HorizButton;
import com.oddlabs.tt.gui.Label;
import com.oddlabs.tt.gui.OKButton;
import com.oddlabs.tt.gui.OKListener;
import com.oddlabs.tt.gui.Origin;
import com.oddlabs.tt.net.ServerMessageBundler;

import java.util.ResourceBundle;

import static com.oddlabs.tt.gui.Placement.BOTTOM_LEFT;
import static com.oddlabs.tt.gui.Placement.RIGHT_TOP;

/**
 * Modal dialog showing configuration details for a selected game match.
 */
public final class GameInfoForm extends Form {
    private static final ResourceBundle bundle = ResourceBundle.getBundle(GameInfoForm.class.getName());

    private String i18n(String key, Object... args) {
        return Utils.getBundleString(bundle, key, args);
    }

    private final HorizButton ok_button;

    public GameInfoForm(GUIRoot guiRoot, Game game) {
        super(guiRoot, "");
        var editFont = skin.getEditFont();
        Label label_headline = new Label(i18n("game_info"), skin.getHeadlineFont());
        addChild(label_headline);

        Group types = new Group(guiRoot);
        Group values = new Group(guiRoot);

        Label label_name = new Label(i18n("name"), editFont);
        Label label_name_value = new Label(game.getName(), editFont);
        types.addChild(label_name);
        values.addChild(label_name_value);

        Label label_size = new Label(i18n("size"), editFont);
        Label label_size_value = new Label(ServerMessageBundler.getSizeString(game.getSize()), editFont);
        types.addChild(label_size);
        values.addChild(label_size_value);

        Label label_terrain_type = new Label(i18n("terrain_type"), editFont);
        Label label_terrain_type_value = new Label(ServerMessageBundler.getTerrainTypeString(game.getTerrainType()),
                editFont);
        types.addChild(label_terrain_type);
        values.addChild(label_terrain_type_value);

        Label label_hills = new Label(i18n("hills"), editFont);
        Label label_hills_value = new Label(ServerMessageBundler.getHillsString(game.getHills()), editFont);
        types.addChild(label_hills);
        values.addChild(label_hills_value);

        Label label_trees = new Label(i18n("trees"), editFont);
        Label label_trees_value = new Label(ServerMessageBundler.getTreesString(game.getTrees()), editFont);
        types.addChild(label_trees);
        values.addChild(label_trees_value);

        Label label_supplies = new Label(i18n("supplies"), editFont);
        Label label_supplies_value = new Label(ServerMessageBundler.getSuppliesString(game.getSupplies()), editFont);
        types.addChild(label_supplies);
        values.addChild(label_supplies_value);

        Label label_rated = new Label(i18n("rated"), editFont);
        Label label_rated_value = new Label(ServerMessageBundler.getRatedString(game.isRated()), editFont);
        types.addChild(label_rated);
        values.addChild(label_rated_value);

        Label label_gamespeed = new Label(i18n("gamespeed"), editFont);
        Label label_gamespeed_value = new Label(ServerMessageBundler.getGamespeedString(game.getGamespeed()), editFont);
        types.addChild(label_gamespeed);
        values.addChild(label_gamespeed_value);

        Label label_mapcode = new Label(i18n("mapcode"), editFont);
        Label label_mapcode_value = new Label(game.getMapcode(), editFont);
        types.addChild(label_mapcode);
        values.addChild(label_mapcode_value);

        label_name.place();
        label_rated.place(label_name, BOTTOM_LEFT);
        label_gamespeed.place(label_rated, BOTTOM_LEFT);
        label_terrain_type.place(label_gamespeed, BOTTOM_LEFT);
        label_size.place(label_terrain_type, BOTTOM_LEFT);
        label_hills.place(label_size, BOTTOM_LEFT);
        label_trees.place(label_hills, BOTTOM_LEFT);
        label_supplies.place(label_trees, BOTTOM_LEFT);
        label_mapcode.place(label_supplies, BOTTOM_LEFT);
        types.compileCanvas();
        addChild(types);

        label_name_value.place();
        label_rated_value.place(label_name_value, BOTTOM_LEFT);
        label_gamespeed_value.place(label_rated_value, BOTTOM_LEFT);
        label_terrain_type_value.place(label_gamespeed_value, BOTTOM_LEFT);
        label_size_value.place(label_terrain_type_value, BOTTOM_LEFT);
        label_hills_value.place(label_size_value, BOTTOM_LEFT);
        label_trees_value.place(label_hills_value, BOTTOM_LEFT);
        label_supplies_value.place(label_trees_value, BOTTOM_LEFT);
        label_mapcode_value.place(label_supplies_value, BOTTOM_LEFT);
        values.compileCanvas();
        addChild(values);

        ok_button = new OKButton(guiRoot, 100);
        addChild(ok_button);
        ok_button.addMouseClickListener(new OKListener(this));

        label_headline.place();
        types.place(label_headline, BOTTOM_LEFT);
        values.place(types, RIGHT_TOP);

        ok_button.place(Origin.AT_END);
        compileCanvas();
        centerPos();
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
