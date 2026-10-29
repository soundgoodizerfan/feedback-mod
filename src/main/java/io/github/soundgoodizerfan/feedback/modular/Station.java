/*
 * Feedback -- a Minecraft technology mod.
 * Copyright (C) 2026 soundgoodizerfan
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Assets under src/main/resources/assets are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.modular;

import java.util.List;
import java.util.Locale;

import com.mojang.serialization.Codec;

import io.github.soundgoodizerfan.feedback.process.Material;

import net.minecraft.util.StringRepresentable;

/**
 * Which bench a part is made at ({@code docs/tool_shop_rework.md}), and the descriptor of every station the Tool Table screen
 * serves ({@code docs/gui_flow_structure.md}). The three part-making benches split stock by what it is made of, read off the
 * material's kind and never its name, so a pack's own wood or stone is handled the same, and every material has exactly one:
 * metal is smithed, anything organic (wood, bone, leather, fibre) is worked at the woodworking table, and the rest (stone,
 * ceramic, glass, crystal) is cut at the stonecutter. {@link #TOOL}, {@link #GUN} and {@link #PREP} take any stock; the Gun
 * and Prep Tables have no block of their own keyed on this enum, they are what {@link #of(ToolTableMenu)} names for their
 * menus (which keep {@link #TOOL} as their stock filter). Only the smithing table plays the forging pattern as its sole work.
 *
 * <p>The descriptor is data only and server-safe, so the skin is a {@code GuiSkin} constant name that the client reads.
 * {@link #CODEC} is used only by {@link StationBlock}'s block codec, never saved, so the extra entries need no migration.
 */
public enum Station implements StringRepresentable {
    TOOL("STONE", "hammer", "gui.feedback.tool_table.hammer", "tool_table", "tool_table", 0,
            tabs(tab("part", ToolTableMenu.MODE_PART, ToolTableMenu.BTN_MODE), tab("assemble", ToolTableMenu.MODE_ASSEMBLE, ToolTableMenu.BTN_MODE + 1),
                    tab("rework", ToolTableMenu.MODE_REWORK, ToolTableMenu.BTN_MODE + 2)), Panel.FORGE, Panel.ASSEMBLE),
    SMITHING("FORGE", "hammer", "gui.feedback.tool_table.hammer", "tool_table", "tool_table", 0,
            tabs(tab("part", ToolTableMenu.MODE_PART, ToolTableMenu.BTN_MODE), tab("smith", ToolTableMenu.MODE_SMITH, ToolTableMenu.BTN_SMITH)), Panel.FORGE),
    WOODWORKING("WOOD", "axe", "gui.feedback.tool_table.axe", "tool_table", "tool_table", 0,
            tabs(tab("part", ToolTableMenu.MODE_PART, ToolTableMenu.BTN_MODE), tab("saw", ToolTableMenu.MODE_CUT, ToolTableMenu.BTN_CUT)), Panel.CUT),
    STONECUTTING("STONE", "hammer", "gui.feedback.tool_table.hammer", "tool_table", "tool_table", 0,
            tabs(tab("part", ToolTableMenu.MODE_PART, ToolTableMenu.BTN_MODE), tab("cut", ToolTableMenu.MODE_CUT, ToolTableMenu.BTN_CUT)), Panel.CUT),
    GUN("PANEL", "hammer", "gui.feedback.tool_table.hammer", "tool_table", "gun_table", 0,
            tabs(tab("part", ToolTableMenu.MODE_PART, ToolTableMenu.BTN_MODE), tab("assemble", ToolTableMenu.MODE_ASSEMBLE, ToolTableMenu.BTN_MODE + 1),
                    tab("rework", ToolTableMenu.MODE_REWORK, ToolTableMenu.BTN_MODE + 2)), Panel.FORGE, Panel.ASSEMBLE),
    PREP("BRICK", "knife", "gui.feedback.prep_table.knife", "prep_table", "tool_table", 80,
            tabs(tab("part", ToolTableMenu.MODE_PART, ToolTableMenu.BTN_MODE), tab("assemble", ToolTableMenu.MODE_ASSEMBLE, ToolTableMenu.BTN_MODE + 1),
                    new Tab("gui.feedback.prep_table.tab.prepare", ToolTableMenu.MODE_PREPARE, ToolTableMenu.BTN_MODE + 2)),
            Panel.FORGE, Panel.ASSEMBLE, Panel.PREP);

    public static final Codec<Station> CODEC = StringRepresentable.fromEnum(Station::values);

    /** The kinds of work a station's work stage can host; one client panel each. */
    public enum Panel { FORGE, CUT, PREP, ASSEMBLE }

    /** A tab of the screen: its lang key, the menu mode it shows and the menu button that switches to it. */
    public record Tab(String key, int mode, int button) { }

    private static Tab tab(String name, int mode, int button) {
        return new Tab("gui.feedback.tool_table.tab." + name, mode, button);
    }

    private static List<Tab> tabs(Tab... tabs) {
        return List.of(tabs);
    }

    private final String skin, toolTip, toolLabel, statusScope, scope;
    private final int statusDrop;
    private final List<Tab> tabs;
    private final List<Panel> panels;

    Station(String skin, String toolTip, String toolLabel, String statusScope, String scope, int statusDrop, List<Tab> tabs, Panel... panels) {
        this.skin = skin;
        this.toolTip = toolTip;
        this.toolLabel = toolLabel;
        this.statusScope = statusScope;
        this.scope = scope;
        this.statusDrop = statusDrop;
        this.tabs = tabs;
        this.panels = List.of(panels);
    }

    /** The descriptor for a menu: the Gun and Prep Tables run on {@link #TOOL} menus, so the menu's flags name them. */
    public static Station of(ToolTableMenu menu) {
        return menu.gunTable() ? GUN : menu.dishTable() ? PREP : menu.station();
    }

    /** The {@code GuiSkin} constant name this station is drawn on. */
    public String skin() {
        return skin;
    }

    /** The tabs, in order: the first is always the part list. */
    public List<Tab> tabs() {
        return tabs;
    }

    /** Whether this station hosts that kind of work panel. */
    public boolean has(Panel panel) {
        return panels.contains(panel);
    }

    /** The lang key of the tool slot's label. */
    public String toolLabel() {
        return toolLabel;
    }

    /** The Prep Table: dishes and foods, a type picker, a knife. */
    public boolean dishes() {
        return this == PREP;
    }

    /** The Gun Table: six assembly slots, a round slot and the spec sheet. */
    public boolean gun() {
        return this == GUN;
    }

    /** The woodworking table: an axe and a saw, where the others have a hammer and a cut. */
    public boolean saws() {
        return this == WOODWORKING;
    }

    /** Where the status text sits below the stock grid, or 0 to put it under the rows the tab uses. */
    public int statusDrop() {
        return statusDrop;
    }

    /** Where the blueprint slot sits. */
    public int planY() {
        return gun() ? ToolTableMenu.GUN_PLAN_Y : ToolTableMenu.PLAN_Y;
    }

    /** The lang prefix of the {@code help.*} lines of the rework tab. */
    public String helpScope() {
        return scope;
    }

    /** The lang key of the status line {@code why}: the Tool Table's own, the Prep Table's, or the Gun Table's for an empty tool slot. */
    public String status(String why) {
        return "gui.feedback." + (statusScope.equals("prep_table") ? statusScope : why.equals("put_tool") ? scope : "tool_table") + ".status." + why;
    }

    /** The tooltip key of an empty slot of {@code slot} ({@code hammer, tool, plan, round, part}) here. */
    public String tip(String slot) {
        if (slot.equals("hammer"))
            return toolTip;
        if (slot.equals("round"))
            return slot;
        String key = slot.equals("plan") && dishes() ? "seasoning" : slot;
        if (gun())
            key += "_gun";
        return dishes() && key.equals("part") ? key + "_dish" : key;
    }

    /** Whether stock of {@code m} may be shaped here. */
    public boolean accepts(Material m) {
        return switch (this) {
            case WOODWORKING -> !hot(m) && m.kind().filter(k -> k == Material.Kind.ORGANIC).isPresent();
            case STONECUTTING -> !hot(m) && m.kind().filter(k -> k == Material.Kind.ORGANIC).isEmpty();
            case SMITHING -> hot(m);
            default -> true;
        };
    }

    /** Worked hot: a metal, or anything with a hot-working window. */
    private static boolean hot(Material m) {
        return m.kind().filter(k -> k == Material.Kind.METAL).isPresent() || m.minTemperature().value() > 0;
    }

    /** Whether the bench works with a hand tool: a hammer, an axe at the woodworking table; the stonecutter has none. */
    public boolean usesTool() {
        return this != STONECUTTING;
    }

    /** Whether a shape that is forged (a rhythm played) can be made here. */
    public boolean forges() {
        return this != WOODWORKING && this != STONECUTTING;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
