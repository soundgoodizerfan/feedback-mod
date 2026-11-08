// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
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
    /** Media a Programmer can erase and print over (controller spec section 2): Floppy Disk and USB Drive. */
    public static final TagKey<Item> SOFT_CARDS = TagKey.create(Registries.ITEM, Feedback.id("soft_cards"));
    public static final TagKey<Item> PUNCH_CARDS = TagKey.create(Registries.ITEM, Feedback.id("punch_cards"));
    /** What an Engine Stand prints a design onto: blank paper, or a blueprint to edit. */
    public static final TagKey<Item> BLUEPRINT_PAPER = TagKey.create(Registries.ITEM, Feedback.id("blueprint_paper"));
    /** What a Lathe accepts in its tool post. */
    public static final TagKey<Item> LATHE_TOOL_BITS = TagKey.create(Registries.ITEM, Feedback.id("lathe_tool_bits"));
    /** Held containers a stockpot can ladle a serving into. */
    public static final TagKey<Item> LADLE_CONTAINERS = TagKey.create(Registries.ITEM, Feedback.id("ladle_containers"));
    /** Held tools that flip a piece in a skillet and lift the pan off the fire. */
    public static final TagKey<Item> SPATULA = TagKey.create(Registries.ITEM, Feedback.id("spatula"));
    /** Held tools that stir (held) and skim (tap) a stockpot. */
    public static final TagKey<Item> SPOON = TagKey.create(Registries.ITEM, Feedback.id("spoon"));
    /** Blocks whose item inventory a heat source never warms (they take heat through {@code HeatSource.at} themselves). */
    public static final TagKey<Block> NO_HEAT_PUSH = TagKey.create(Registries.BLOCK, Feedback.id("no_heat_push"));
    /** Blocks that insulate a vessel they touch. */
    public static final TagKey<Block> INSULATION = TagKey.create(Registries.BLOCK, Feedback.id("insulation"));
    /** Blocks an edge can be ground at: shift-use with an edged modular tool in hand. */
    public static final TagKey<Block> GRINDSTONES = TagKey.create(Registries.BLOCK, Feedback.id("grindstones"));
    /** What a boiler makes and an engine runs on. Includes {@code #c:steam}, so other mods' steam works. */
    /** Held items hot enough to light a fuse: flint and steel, a fire charge. */
    public static final TagKey<net.minecraft.world.item.Item> IGNITERS = TagKey.create(Registries.ITEM, Feedback.id("igniters"));
    public static final TagKey<Fluid> STEAM = TagKey.create(Registries.FLUID, Feedback.id("steam"));

    /** Relays that count as a switching element when mounted on a board (basic electronics phase 4). */
    /** Instruments a live circuit can be fitted to, narrowing their step. */
    public static final TagKey<Item> CIRCUIT_FITTABLE = TagKey.create(Registries.ITEM, Feedback.id("circuit_fittable"));
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
