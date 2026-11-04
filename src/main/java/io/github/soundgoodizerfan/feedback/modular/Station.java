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
 * Which bench a part is made at, and the descriptor of every station the Tool Table screen
 * serves ({@code docs/gui/gui_flow_structure.md}). The three part-making benches split stock by what it is made of, read off the
 * material's kind and never its name, so a pack's own wood or stone is handled the same: metal is smithed, anything
 * organic (wood, bone, leather, fibre) is worked at the woodworking table, and the stonecutter takes any cold stock, organic
 * included, so it does all the woodworking table does as well as stone, ceramic, glass and crystal. {@link #TOOL}, {@link #GUN} and {@link #PREP} take any stock; the Gun
 * and Prep Tables have no block of their own keyed on this enum, they are what {@link #of(ToolTableMenu)} names for their
 * menus (which keep {@link #TOOL} as their stock filter). The anvil takes the smithing table's metal and also forges it (plays the
 * forging pattern); the smithing table makes the unforged shapes only, as the stonecutter does the woodworking table's work and more.
 *
 * <p>The descriptor is data only and server-safe, so the skin is a {@code GuiSkin} constant name that the client reads.
 * {@link #CODEC} is used only by {@link StationBlock}'s block codec, never saved, so the extra entries need no migration.
 */
public enum Station implements StringRepresentable {
    /** The stock filter of the Gun and Prep Tables' menus: any stock, any shape. Not a block of its own. */
    TOOL("STONE", "hammer", "gui.feedback.tool_table.hammer", List.of(), Panel.FORGE),
    SMITHING("FORGE", "hammer", "gui.feedback.tool_table.hammer", List.of(tab("forge"), tab("trim")), Panel.FORGE),
    ANVIL("ANVIL", "hammer", "gui.feedback.tool_table.hammer", List.of(tab("forge"), tab("trim")), Panel.FORGE),
    WOODWORKING("WOOD", "axe", "gui.feedback.tool_table.axe", List.of(tab("parts"), tab("saw"))),
    STONECUTTING("STONE", "saw", "gui.feedback.tool_table.saw", List.of(tab("parts"), tab("cut"))),
    GUN("PANEL", "hammer", "gui.feedback.tool_table.hammer", List.of(), Panel.FORGE),
    PREP("BRICK", "knife", "gui.feedback.prep_table.knife", List.of(), Panel.FORGE, Panel.PREP);

    public static final Codec<Station> CODEC = StringRepresentable.fromEnum(Station::values);

    /** The kinds of in-place work a station hosts on its bench: the forging rhythm, and the Prep Table's cut, knead and pour. */
    public enum Panel { FORGE, PREP }

    /**
     * A folder tab of the bench: its id and lang key. The menu button that selects tab {@code i} is {@code MenuTabs.button(i)}; a station with
     * fewer than two tabs draws no strip. A new station gets tabs by listing them here.
     */
    public record Tab(String id, String key) { }

    private static Tab tab(String id) {
        return new Tab(id, "gui.feedback.bench.tab." + id);
    }

    private final String skin, toolTip, toolLabel;
    private final List<Tab> tabs;
    private final List<Panel> panels;

    Station(String skin, String toolTip, String toolLabel, List<Tab> tabs, Panel... panels) {
        this.tabs = tabs;
        this.skin = skin;
        this.toolTip = toolTip;
        this.toolLabel = toolLabel;
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

    /** The folder tabs, in order; fewer than two means none. */
    public List<Tab> tabs() {
        return tabs.size() < 2 ? List.of() : tabs;
    }

    /** Whether this station hosts that kind of in-place work. */
    public boolean has(Panel panel) {
        return panels.contains(panel);
    }

    /** The lang key of the hand tool slot's label. */
    public String toolLabel() {
        return toolLabel;
    }

    /** The Prep Table: dishes and foods, a type picker, a knife. */
    public boolean dishes() {
        return this == PREP;
    }

    /** The Gun Table: sixteen fitted parts, a round slot and the spec sheet. */
    public boolean gun() {
        return this == GUN;
    }

    /** The woodworking table: an axe and a saw, where the others have a hammer and a cut. */
    public boolean saws() {
        return this == WOODWORKING;
    }

    /** The tooltip key of an empty slot of {@code slot} ({@code hammer, centre, stock, plan, round}) here. */
    public String tip(String slot) {
        if (slot.equals("hammer"))
            return toolTip;
        if (slot.equals("plan") && dishes())
            return "seasoning";
        // The prep table works food, so its centre and stock slots have their own words.
        if (dishes() && (slot.equals("centre") || slot.equals("stock")))
            return slot + "_dish";
        return slot;
    }

    /** Whether stock of {@code m} may be shaped here. */
    public boolean accepts(Material m) {
        return switch (this) {
            case WOODWORKING -> !hot(m) && m.kind().filter(k -> k == Material.Kind.ORGANIC).isPresent();
            case STONECUTTING -> !hot(m);
            case SMITHING, ANVIL -> hot(m);
            default -> true;
        };
    }

    /** Worked hot: a metal, or anything with a hot-working window. */
    private static boolean hot(Material m) {
        return m.kind().filter(k -> k == Material.Kind.METAL).isPresent() || m.minTemperature().value() > 0;
    }

    /** Whether the bench works with a hand tool: a hammer, an axe at the woodworking table, a saw at the stonecutter. */
    public boolean usesTool() {
        return true;
    }

    /** Whether a shape that is forged (a rhythm played) can be made here. The smithing table shapes metal but never forges it; the anvil does. */
    public boolean forges() {
        return this != WOODWORKING && this != STONECUTTING && this != SMITHING;
    }

    /** The metal benches: the smithing table, and the anvil that does all it does and forges as well. Both carry the Trim tab. */
    public boolean smiths() {
        return this == SMITHING || this == ANVIL;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
