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
package io.github.soundgoodizerfan.feedback.registry;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

/**
 * Tags the mod tests against instead of naming an item, block or fluid -- §11's no identity
 * checks, and so that another mod's equivalent works by joining the tag.
 */
public final class FTags {

    /** What a Controller or Programmer accepts as its program card. */
    public static final TagKey<Item> PUNCH_CARDS = TagKey.create(Registries.ITEM, Feedback.id("punch_cards"));
    /** What an Engine Stand prints a design onto: blank paper, or a blueprint to edit. */
    public static final TagKey<Item> BLUEPRINT_PAPER = TagKey.create(Registries.ITEM, Feedback.id("blueprint_paper"));
    /** What a Lathe accepts in its tool post. */
    public static final TagKey<Item> LATHE_TOOL_BITS = TagKey.create(Registries.ITEM, Feedback.id("lathe_tool_bits"));
    /** Held containers a stockpot can ladle a serving into. */
    public static final TagKey<Item> LADLE_CONTAINERS = TagKey.create(Registries.ITEM, Feedback.id("ladle_containers"));
    /** Blocks that insulate a vessel they touch. */
    public static final TagKey<Block> INSULATION = TagKey.create(Registries.BLOCK, Feedback.id("insulation"));
    /** Blocks an edge can be ground at: shift-use with an edged modular tool in hand. */
    public static final TagKey<Block> GRINDSTONES = TagKey.create(Registries.BLOCK, Feedback.id("grindstones"));
    /** What a boiler makes and an engine runs on. Includes {@code #c:steam}, so other mods' steam works. */
    /** Held items hot enough to light a fuse: flint and steel, a fire charge. */
    public static final TagKey<net.minecraft.world.item.Item> IGNITERS = TagKey.create(Registries.ITEM, Feedback.id("igniters"));
    public static final TagKey<Fluid> STEAM = TagKey.create(Registries.FLUID, Feedback.id("steam"));

    /** Relays that count as a switching element when mounted on a board (basic electronics phase 4). */
    public static final TagKey<Item> CIRCUIT_RELAYS = TagKey.create(Registries.ITEM, Feedback.id("circuit_relays"));
    /** Coherers that count as a switching element when mounted on a board. */
    public static final TagKey<Item> CIRCUIT_COHERERS = TagKey.create(Registries.ITEM, Feedback.id("circuit_coherers"));

    /** Column parts by role, so a pack's own tray, reboiler or condenser block joins a column (spec 6.2). */
    public static final TagKey<Block> COLUMN_REBOILER = TagKey.create(Registries.BLOCK, Feedback.id("column/reboiler"));
    public static final TagKey<Block> COLUMN_TRAY = TagKey.create(Registries.BLOCK, Feedback.id("column/tray"));
    public static final TagKey<Block> COLUMN_CONDENSER = TagKey.create(Registries.BLOCK, Feedback.id("column/condenser"));

    private FTags() {
    }
}
