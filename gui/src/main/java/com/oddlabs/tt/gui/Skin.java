package com.oddlabs.tt.gui;

import com.oddlabs.tt.engine.render.IconQuad;
import com.oddlabs.tt.engine.render.ModeIconQuads;
import com.oddlabs.tt.engine.font.Font;
import com.oddlabs.tt.engine.render.Texture;
import com.oddlabs.tt.engine.resource.FontFile;
import com.oddlabs.tt.engine.resource.Resources;
import org.jspecify.annotations.Nullable;
import org.w3c.dom.Node;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.SequencedMap;

import static com.oddlabs.tt.gui.Icons.getIconQuads;
import static com.oddlabs.tt.gui.Icons.getInt;
import static com.oddlabs.tt.gui.Icons.getNamedColor;
import static com.oddlabs.tt.gui.Icons.getNamedIconQuad;
import static com.oddlabs.tt.gui.Icons.getNamedIconQuads;
import static com.oddlabs.tt.gui.Icons.getNodeByName;

/**
 * Skin for GUI
 */
public final class Skin {
    static final ScopedValue<Skin> CURRENT = ScopedValue.newInstance();

    private final Font edit_font;
    private final Font button_font;
    private final Font headline_font;

    private final ModeIconQuads plus_button;
    private final ModeIconQuads minus_button;
    private final ModeIconQuads accept_button;
    private final ModeIconQuads cancel_button;
    private final ModeIconQuads back_button;
    private final Horizontal horiz_button_pressed;
    private final Horizontal horiz_button_unpressed;
    private final FormData form_data;
    private final Box edit_box;
    private final Box background_box;
    private final ModeIconQuads check_box_marked;
    private final ModeIconQuads check_box_unmarked;
    private final ModeIconQuads radio_button_marked;
    private final ModeIconQuads radio_button_unmarked;
    private final GroupData group_data;
    private final ScrollBarData scroll_bar_data;
    private final SliderData slider_data;
    private final PulldownData pulldown_data;
    private final ProgressBarData progress_bar_data;
    private final MultiColumnComboBoxData multi_columnCombo_box_data;
    private final ToolTipBoxInfo tool_tip;
    private final ModeIconQuads diode;
    private final PanelData panel_data;
    private final IconQuad flag_default;
    private final SequencedMap<String, IconQuad> flags;

    public static Skin getSkin() {
        return CURRENT.orElseThrow(() -> new IllegalStateException("Skin not in scope"));
    }

    public static void run(Skin skin, Runnable operation) {
        ScopedValue.where(CURRENT, skin).run(operation);
    }

    public static <V, X extends Throwable> V call(Skin skin,
            ScopedValue.CallableOp<V, X> operation) throws X {
        return ScopedValue.where(CURRENT, skin).call(operation);
    }

    public Skin(String xml_file) {
        Node root = Icons.loadFile(xml_file, new GUIErrorHandler());
        Texture texture = Icons.loadTexture(root);
        edit_font = parseEditFont(root);
        button_font = parseButtonFont(root);
        headline_font = parseHeadlineFont(root);

        plus_button = getNamedIconQuads(root, "plus_button", texture);
        minus_button = getNamedIconQuads(root, "minus_button", texture);
        accept_button = getNamedIconQuads(root, "accept_button", texture);
        cancel_button = getNamedIconQuads(root, "cancel_button", texture);
        back_button = getNamedIconQuads(root, "back_button", texture);
        check_box_marked = parseCheckBoxMarked(root, texture);
        check_box_unmarked = parseCheckBoxUnmarked(root, texture);
        radio_button_marked = parseRadioButtonMarked(root, texture);
        radio_button_unmarked = parseRadioButtonUnmarked(root, texture);
        horiz_button_pressed = parseHorizButtonPressed(root, texture);
        horiz_button_unpressed = parseHorizButtonUnpressed(root, texture);
        form_data = parseFormData(root, texture);
        edit_box = parseBox(root, "editbox", texture);
        background_box = parseBox(root, "backgroundbox", texture);
        group_data = parseGroupData(root, texture);
        scroll_bar_data = parseScrollBarData(root, texture);
        slider_data = parseSliderData(root, texture);
        pulldown_data = parsePulldownData(root, texture);
        progress_bar_data = parseProgressBarData(root, texture);
        multi_columnCombo_box_data = parseMultiColumnComboBoxData(root, texture);
        tool_tip = parseToolTipInfo(root, texture);
        diode = getNamedIconQuads(root, "diode", texture);
        panel_data = parsePanelData(root, texture);
        flag_default = getNamedIconQuad(root, "flag_default", texture);
        var flagMap = new LinkedHashMap<String, IconQuad>();
        flagMap.put("da", getNamedIconQuad(root, "flag_da", texture));
        flagMap.put("de", getNamedIconQuad(root, "flag_de", texture));
        flagMap.put("en", getNamedIconQuad(root, "flag_en", texture));
        flagMap.put("es", getNamedIconQuad(root, "flag_es", texture));
        flagMap.put("it", getNamedIconQuad(root, "flag_it", texture));
        flagMap.put("pt", getNamedIconQuad(root, "flag_pt", texture));
        flags = Collections.unmodifiableSequencedMap(flagMap);
    }

    private Horizontal getHorizontal(Node n, Texture texture) {
        Node horizontal_node = getNodeByName("horizontal", n);
        return new Horizontal(
                getIconQuads(getNodeByName("left", horizontal_node), texture),
                getIconQuads(getNodeByName("center", horizontal_node), texture),
                getIconQuads(getNodeByName("right", horizontal_node), texture)
        );
    }

    private Vertical getVertical(Node n, Texture texture) {
        Node vertical_node = getNodeByName("vertical", n);
        return new Vertical(
                getIconQuads(getNodeByName("bottom", vertical_node), texture),
                getIconQuads(getNodeByName("center", vertical_node), texture),
                getIconQuads(getNodeByName("top", vertical_node), texture)
        );
    }

    private Box getBox(Node n, Texture texture) {
        Node box_node = getNodeByName("box", n);
        ModeIconQuads left_bottom = getIconQuads(getNodeByName("left_bottom", box_node), texture);
        ModeIconQuads bottom = getIconQuads(getNodeByName("bottom", box_node), texture);
        ModeIconQuads right_bottom = getIconQuads(getNodeByName("right_bottom", box_node), texture);
        ModeIconQuads right = getIconQuads(getNodeByName("right", box_node), texture);
        ModeIconQuads right_top = getIconQuads(getNodeByName("right_top", box_node), texture);
        ModeIconQuads top = getIconQuads(getNodeByName("top", box_node), texture);
        ModeIconQuads left_top = getIconQuads(getNodeByName("left_top", box_node), texture);
        ModeIconQuads left = getIconQuads(getNodeByName("left", box_node), texture);
        ModeIconQuads center = getIconQuads(getNodeByName("center", box_node), texture);
        int left_offset = getInt(box_node, "left_offset");
        int bottom_offset = getInt(box_node, "bottom_offset");
        int right_offset = getInt(box_node, "right_offset");
        int top_offset = getInt(box_node, "top_offset");
        return new Box(left_bottom,
                bottom,
                right_bottom,
                right,
                right_top,
                top,
                left_top,
                left, center,
                left_offset,
                bottom_offset,
                right_offset,
                top_offset);
    }

    private Font getFont(Node n) {
        String path = n.getFirstChild().getNodeValue();
        FontFile font_file = new FontFile(path);
        return Resources.findResource(font_file);
    }

    private Font parseEditFont(Node n) {
        Node node = getNodeByName("editfont", n);
        return getFont(node);
    }

    public Font getEditFont() {
        return edit_font;
    }

    private Font parseButtonFont(Node n) {
        Node node = getNodeByName("buttonfont", n);
        return getFont(node);
    }

    public Font getButtonFont() {
        return button_font;
    }

    private Font parseHeadlineFont(Node n) {
        Node node = getNodeByName("headlinefont", n);
        return getFont(node);
    }

    public Font getHeadlineFont() {
        return headline_font;
    }

    private ModeIconQuads parseCheckBoxMarked(Node n, Texture texture) {
        Node node = getNodeByName("checkbox", n);
        node = getNodeByName("marked", node);
        return getIconQuads(node, texture);
    }

    public ModeIconQuads getCheckBoxMarked() {
        return check_box_marked;
    }

    private ModeIconQuads parseCheckBoxUnmarked(Node n, Texture texture) {
        Node node = getNodeByName("checkbox", n);
        node = getNodeByName("unmarked", node);
        return getIconQuads(node, texture);
    }

    public ModeIconQuads getCheckBoxUnmarked() {
        return check_box_unmarked;
    }

    private ModeIconQuads parseRadioButtonMarked(Node n, Texture texture) {
        Node node = getNodeByName("radiobutton", n);
        node = getNodeByName("marked", node);
        return getIconQuads(node, texture);
    }

    public ModeIconQuads getRadioButtonMarked() {
        return radio_button_marked;
    }

    private ModeIconQuads parseRadioButtonUnmarked(Node n, Texture texture) {
        Node node = getNodeByName("radiobutton", n);
        node = getNodeByName("unmarked", node);
        return getIconQuads(node, texture);
    }

    public ModeIconQuads getRadioButtonUnmarked() {
        return radio_button_unmarked;
    }

    private Horizontal parseHorizButtonPressed(Node n, Texture texture) {
        Node node = getNodeByName("horiz_button", n);
        node = getNodeByName("horiz_pressed", node);
        return getHorizontal(node, texture);
    }

    public Horizontal getHorizButtonPressed() {
        return horiz_button_pressed;
    }

    private Horizontal parseHorizButtonUnpressed(Node n, Texture texture) {
        Node node = getNodeByName("horiz_button", n);
        node = getNodeByName("horiz_unpressed", node);
        return getHorizontal(node, texture);
    }

    public Horizontal getHorizButtonUnpressed() {
        return horiz_button_unpressed;
    }

    private ScrollBarData parseScrollBarData(Node n, Texture texture) {
        Node node = getNodeByName("vert_scroll", n);
        Vertical scroll_bar = getVertical(node, texture);

        Node temp;
        temp = getNodeByName("less", node);
        temp = getNodeByName("pushbutton", temp);
        temp = getNodeByName("pressed", temp);
        ModeIconQuads scroll_down_button_pressed = getIconQuads(temp, texture);

        temp = getNodeByName("less", node);
        temp = getNodeByName("pushbutton", temp);
        temp = getNodeByName("unpressed", temp);
        ModeIconQuads scroll_down_button_unpressed = getIconQuads(temp, texture);

        temp = getNodeByName("less", node);
        ModeIconQuads scroll_down_arrow = getIconQuads(temp, texture);

        temp = getNodeByName("more", node);
        temp = getNodeByName("pushbutton", temp);
        temp = getNodeByName("pressed", temp);
        ModeIconQuads scroll_up_button_pressed = getIconQuads(temp, texture);

        temp = getNodeByName("more", node);
        temp = getNodeByName("pushbutton", temp);
        temp = getNodeByName("unpressed", temp);
        ModeIconQuads scroll_up_button_unpressed = getIconQuads(temp, texture);

        temp = getNodeByName("more", node);
        ModeIconQuads scroll_up_arrow = getIconQuads(temp, texture);

        temp = getNodeByName("vert_scroll_button", n);
        Vertical scroll_button = getVertical(temp, texture);

        return new ScrollBarData(scroll_bar,
                scroll_down_button_pressed,
                scroll_down_button_unpressed,
                scroll_down_arrow,
                scroll_up_button_pressed,
                scroll_up_button_unpressed,
                scroll_up_arrow,
                scroll_button,
                getInt(node, "left_offset"),
                getInt(node, "bottom_offset"),
                getInt(node, "top_offset"));
    }

    public ScrollBarData getScrollBarData() {
        return scroll_bar_data;
    }

    private SliderData parseSliderData(Node n, Texture texture) {
        Node node = getNodeByName("slider", n);
        Horizontal slider = getHorizontal(node, texture);

        ModeIconQuads button = getIconQuads(node, texture);

        return new SliderData(slider,
                button,
                getInt(node, "left_offset"),
                getInt(node, "right_offset"));
    }

    public SliderData getSliderData() {
        return slider_data;
    }

    private PulldownData parsePulldownData(Node n, Texture texture) {
        Node node = getNodeByName("pulldown_menu", n);

        Node temp;
        temp = getNodeByName("pulldown_top", node);
        Horizontal pulldownTop = getHorizontal(temp, texture);

        temp = getNodeByName("pulldown_bottom", node);
        Horizontal pulldownBottom = getHorizontal(temp, texture);

        Node item_node = getNodeByName("pulldown_item", n);
        Box pulldownItem = getBox(item_node, texture);

        Node button_node = getNodeByName("pulldown_button", n);
        Horizontal pulldown_button = getHorizontal(button_node, texture);

        ModeIconQuads arrow = getIconQuads(button_node, texture);

        return new PulldownData(pulldownTop,
                pulldownBottom,
                pulldownItem,
                pulldown_button,
                arrow,
                getInt(button_node, "arrow_offset_right"),
                getInt(button_node, "text_offset_left"),
                getFont(getNodeByName("pulldownfont", n)));
    }

    public PulldownData getPulldownData() {
        return pulldown_data;
    }

    private ProgressBarData parseProgressBarData(Node n, Texture texture) {
        Node node = getNodeByName("progressbar", n);
        Horizontal progressbar = getHorizontal(node, texture);

        Node temp;
        temp = getNodeByName("left", node);
        ModeIconQuads left = getIconQuads(temp, texture);

        temp = getNodeByName("center", node);
        ModeIconQuads center = getIconQuads(temp, texture);

        temp = getNodeByName("right", node);
        ModeIconQuads right = getIconQuads(temp, texture);

        return new ProgressBarData(progressbar,
                left,
                center,
                right,
                getFont(getNodeByName("progressfont", n)));
    }

    public ProgressBarData getProgressBarData() {
        return progress_bar_data;
    }

    private FormData parseFormData(Node n, Texture texture) {
        Node node = getNodeByName("slim_form", n);
        Box slim_form = getBox(node, texture);

        node = getNodeByName("form", n);
        return new FormData(getBox(node, texture),
                slim_form,
                getIconQuads(node, texture),
                getInt(node, "spacing"),
                getInt(node, "section_spacing"),
                getInt(node, "caption_left"),
                getInt(node, "caption_y"),
                getInt(node, "close_right"),
                getInt(node, "close_top"),
                getFont(getNodeByName("formfont", n)));
    }

    public FormData getFormData() {
        return form_data;
    }

    public ModeIconQuads getPlusButton() {
        return plus_button;
    }

    public ModeIconQuads getMinusButton() {
        return minus_button;
    }

    public ModeIconQuads getAcceptButton() {
        return accept_button;
    }

    public ModeIconQuads getCancelButton() {
        return cancel_button;
    }

    public ModeIconQuads getBackButton() {
        return back_button;
    }

    public ModeIconQuads getDiode() {
        return diode;
    }

    private Box parseBox(Node n, String name, Texture texture) {
        Node node = getNodeByName(name, n);
        return getBox(node, texture);
    }

    public Box getEditBox() {
        return edit_box;
    }

    public Box getBackgroundBox() {
        return background_box;
    }

    private GroupData parseGroupData(Node n, Texture texture) {
        Node node = getNodeByName("group", n);
        return new GroupData(getBox(node, texture),
                getInt(node, "caption_left"),
                getInt(node, "caption_y"),
                getInt(node, "caption_offset"),
                getFont(getNodeByName("groupfont", n)));
    }

    public GroupData getGroupData() {
        return group_data;
    }

    private MultiColumnComboBoxData parseMultiColumnComboBoxData(Node n, Texture texture) {
        Node node = getNodeByName("multi_column_combo", n);
        Node desc = getNodeByName("descending", node);
        Node asc = getNodeByName("ascending", node);
        return new MultiColumnComboBoxData(getBox(node, texture),
                parseHorizButtonPressed(node, texture),
                parseHorizButtonUnpressed(node, texture),
                getIconQuads(desc, texture),
                getIconQuads(asc, texture),
                getNamedColor(node, "color1"),
                getNamedColor(node, "color2"),
                getNamedColor(node, "color_marked"),
                getFont(getNodeByName("combofont", n)),
                getInt(node, "caption_offset"));
    }

    public MultiColumnComboBoxData getMultiColumnComboBoxData() {
        return multi_columnCombo_box_data;
    }

    private ToolTipBoxInfo parseToolTipInfo(Node n, Texture texture) {
        Node node = getNodeByName("tool_tip", n);
        return new ToolTipBoxInfo(getHorizontal(node, texture),
                getInt(node, "left_offset"),
                getInt(node, "bottom_offset"),
                getInt(node, "right_offset"),
                getInt(node, "top_offset"));
    }

    public ToolTipBoxInfo getToolTipInfo() {
        return tool_tip;
    }

    private PanelData parsePanelData(Node n, Texture texture) {
        Node node = getNodeByName("panel", n);
        return new PanelData(getBox(node, texture),
                getHorizontal(node, texture),
                getInt(node, "left_caption_offset"),
                getInt(node, "right_caption_offset"),
                getInt(node, "bottom_caption_offset"),
                getInt(node, "left_tab_offset"),
                getInt(node, "bottom_tab_offset"));
    }

    public PanelData getPanelData() {
        return panel_data;
    }

    public IconQuad getFlagDefault() {
        return flag_default;
    }

    public @Nullable IconQuad getFlag(String language) {
        return flags.get(language);
    }
}
