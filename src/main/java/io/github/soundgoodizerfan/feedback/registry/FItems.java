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
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.item.CalipersItem;
import io.github.soundgoodizerfan.feedback.item.DataConnectorItem;
import io.github.soundgoodizerfan.feedback.item.DebugHelmetItem;
import io.github.soundgoodizerfan.feedback.item.HandHammerItem;
import io.github.soundgoodizerfan.feedback.item.MoldItem;
import io.github.soundgoodizerfan.feedback.item.TemperatureSensorItem;
import io.github.soundgoodizerfan.feedback.item.ThermometerItem;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class FItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Feedback.MOD_ID);

    public static final DeferredItem<BlockItem> SHAFT = ITEMS.registerSimpleBlockItem(FBlocks.SHAFT);
    public static final DeferredItem<BlockItem> SMALL_COG = ITEMS.registerSimpleBlockItem(FBlocks.SMALL_COG);
    public static final DeferredItem<BlockItem> LARGE_COG = ITEMS.registerSimpleBlockItem(FBlocks.LARGE_COG);
    public static final DeferredItem<BlockItem> GEARBOX = ITEMS.registerSimpleBlockItem(FBlocks.GEARBOX);
    public static final DeferredItem<BlockItem> HAND_CRANK = ITEMS.registerSimpleBlockItem(FBlocks.HAND_CRANK);
    public static final DeferredItem<BlockItem> WATER_WHEEL = ITEMS.registerSimpleBlockItem(FBlocks.WATER_WHEEL);

    public static final DeferredItem<BlockItem> CRANK_LINKAGE = ITEMS.registerSimpleBlockItem(FBlocks.CRANK_LINKAGE);
    public static final DeferredItem<BlockItem> MECHANICAL_HAMMER = ITEMS.registerSimpleBlockItem(FBlocks.MECHANICAL_HAMMER);

    public static final DeferredItem<BlockItem> CLUTCH = ITEMS.registerSimpleBlockItem(FBlocks.CLUTCH);
    public static final DeferredItem<BlockItem> TIMER = ITEMS.registerSimpleBlockItem(FBlocks.TIMER);
    public static final DeferredItem<BlockItem> CONTROLLER = ITEMS.registerSimpleBlockItem(FBlocks.CONTROLLER);
    public static final DeferredItem<BlockItem> PROGRAMMER = ITEMS.registerSimpleBlockItem(FBlocks.PROGRAMMER);

    /**
     * Blank until printed by a Programmer. No default {@code PROGRAM_GRAPH} component is set
     * here -- setting one via {@code Item.Properties#component} would resolve
     * {@code FDataComponents.PROGRAM_GRAPH.get()} while {@code FItems}'s own static fields are
     * still initialising, before the data component registry has bound it. Every read site
     * already uses {@code ItemStack#getOrDefault(..., ProgramGraph.EMPTY)}, so an absent
     * component reads the same as an explicit empty one.
     */
    public static final DeferredItem<Item> PUNCH_CARD =
            ITEMS.registerItem("punch_card", Item::new, new Item.Properties().stacksTo(1));

    /**
     * The one route to a plate that needs no machine, and the reason there is one at all.
     *
     * <p>Philosophy 7 promises manual production stays theoretically possible for a surprising
     * share of the game. Until this existed the promise was empty in the first ten minutes: a plate
     * needed a Mechanical Hammer, and a Mechanical Hammer needs plates.
     */
    public static final DeferredItem<HandHammerItem> HAND_HAMMER =
            ITEMS.registerItem("hand_hammer", HandHammerItem::new,
                    new Item.Properties().stacksTo(1).durability(FTuning.HAND_HAMMER_DURABILITY));

    /**
     * The slice's first instrument. Registered here with the blocks rather than off in a tools
     * section, because §7 insists an instrument is a yield technology and not a tier -- it sits
     * alongside the machinery it improves, not above it.
     */
    public static final DeferredItem<Item> CALIPERS =
            ITEMS.registerItem("calipers", CalipersItem::new, new Item.Properties().stacksTo(1));

    // Beat 1's overrun chain. Every step is a real item, and only the last one is a mistake:
    // foil is a genuine sidegrade that beat 2's thermometer needs. See slice 1, "Overrun".
    public static final DeferredItem<Item> COPPER_PLATE = ITEMS.registerSimpleItem("copper_plate");
    public static final DeferredItem<Item> COPPER_FOIL = ITEMS.registerSimpleItem("copper_foil");
    public static final DeferredItem<Item> COPPER_SCRAP = ITEMS.registerSimpleItem("copper_scrap");

    // --- beat 2: heat -----------------------------------------------------------------------

    public static final DeferredItem<BlockItem> FIREBOX = ITEMS.registerSimpleBlockItem(FBlocks.FIREBOX);
    public static final DeferredItem<BlockItem> SMALL_CRUCIBLE = ITEMS.registerSimpleBlockItem(FBlocks.SMALL_CRUCIBLE);
    public static final DeferredItem<BlockItem> LARGE_CRUCIBLE = ITEMS.registerSimpleBlockItem(FBlocks.LARGE_CRUCIBLE);
    public static final DeferredItem<BlockItem> INSULATION = ITEMS.registerSimpleBlockItem(FBlocks.INSULATION);
    public static final DeferredItem<BlockItem> DAMPER = ITEMS.registerSimpleBlockItem(FBlocks.DAMPER);
    public static final DeferredItem<BlockItem> BELLOWS = ITEMS.registerSimpleBlockItem(FBlocks.BELLOWS);
    public static final DeferredItem<BlockItem> BIMETALLIC_STRIP = ITEMS.registerSimpleBlockItem(FBlocks.BIMETALLIC_STRIP);

    public static final DeferredItem<BlockItem> BOILER = ITEMS.registerSimpleBlockItem(FBlocks.BOILER);
    public static final DeferredItem<BlockItem> STEAM_ENGINE = ITEMS.registerSimpleBlockItem(FBlocks.STEAM_ENGINE);

    /**
     * The slice's second instrument, and the one that reveals rather than refines.
     * <p>
     * Copper foil is in it, which is why beat 1's first overrun had to be a sidegrade: the mistake
     * the hammer makes on the way past a plate is the material this is built out of.
     */
    public static final DeferredItem<Item> THERMOMETER =
            ITEMS.registerItem("thermometer", ThermometerItem::new, new Item.Properties().stacksTo(1));

    // Beat 2's materials. Burnt Iron is the overrun and it is a dead end on purpose -- the
    // thermal chain's first mistake is a loss, where the mechanical chain's first mistake was
    // foil. One of the two beats has to teach that overrun is sometimes simply bad.
    public static final DeferredItem<Item> STEEL_INGOT = ITEMS.registerSimpleItem("steel_ingot");
    public static final DeferredItem<Item> BURNT_IRON = ITEMS.registerSimpleItem("burnt_iron");
    public static final DeferredItem<Item> HARDENED_STEEL = ITEMS.registerSimpleItem("hardened_steel");
    /** Tempering's result -- brittle no longer. See {@code data/feedback/thermal_process/tempered_steel.json}. */
    public static final DeferredItem<Item> TEMPERED_STEEL = ITEMS.registerSimpleItem("tempered_steel");
    public static final DeferredItem<Item> STEEL_PLATE = ITEMS.registerSimpleItem("steel_plate");

    /** What molten iron casts into by default now -- see {@code data/feedback/thermal_process/
     * cast_iron.json} for how it becomes {@link #STEEL_INGOT} instead of {@code minecraft:iron_ingot}
     * being the direct steelmaking input, an alternate, slower-to-set-up-but-cheaper-per-batch
     * route than the original iron_ingot+charcoal recipe, which is left untouched. */
    public static final DeferredItem<Item> CAST_IRON = ITEMS.registerSimpleItem("cast_iron");

    // Tin and zinc: no ore, no worldgen yet -- see TODO.md. Registered as plain raw ingots so
    // bronze and brass's alloy chemistry exists and can be pointed at a real ore later without
    // touching a single recipe.
    public static final DeferredItem<Item> TIN_INGOT = ITEMS.registerSimpleItem("tin_ingot");
    public static final DeferredItem<Item> ZINC_INGOT = ITEMS.registerSimpleItem("zinc_ingot");

    /** Copper's two alloys. See {@code data/feedback/alloy/} for the composition windows and
     * {@link io.github.soundgoodizerfan.feedback.process.AlloyMix} for how a crucible mixes them. */
    public static final DeferredItem<Item> BRONZE_INGOT = ITEMS.registerSimpleItem("bronze_ingot");
    public static final DeferredItem<Item> BRASS_INGOT = ITEMS.registerSimpleItem("brass_ingot");

    /** Melts and casts exactly like a metal -- see {@code data/feedback/thermal_process/
     * molten_glass.json} and {@code casting/glass_ingot.json}. Deliberately not TFC's glassblowing
     * minigame; that stays a bigger, separate feature if it's ever wanted. */
    public static final DeferredItem<Item> GLASS_INGOT = ITEMS.registerSimpleItem("glass_ingot");

    /** Fired, not melted -- a vessel taken too hot vitrifies the batch into nothing rather than
     * producing a liquid to cast. See {@code data/feedback/thermal_process/ceramic.json}. */
    public static final DeferredItem<Item> CERAMIC = ITEMS.registerSimpleItem("ceramic");

    /** Dipped, not fired a second time -- a real ceramic glaze is a glass coating, so this is
     * {@link #CERAMIC} dipped in a vessel holding molten glass. See {@code
     * data/feedback/dip/glazed_ceramic.json} and {@code machine/dip/Dipping.java}. */
    public static final DeferredItem<Item> GLAZED_CERAMIC = ITEMS.registerSimpleItem("glazed_ceramic");

    // Firebrick's own raw material, distinct from ordinary pottery clay -- no ore, no worldgen
    // yet, same call as tin/zinc.
    public static final DeferredItem<Item> KAOLINITE = ITEMS.registerSimpleItem("kaolinite");
    public static final DeferredItem<Item> FIREBRICK = ITEMS.registerSimpleItem("firebrick");

    /** Real graphitization is solid-state -- carbon recrystallizes under extreme heat, it never
     * passes through a liquid phase -- so this is a {@code ThermalProcess} like steel, not a melt.
     * {@code data/feedback/thermal_process/graphite.json}'s band (2200-2600 Tu) is well above
     * anything the mod's current fuels reach; that is a deliberate, honest hard gate (§7 -- the
     * recipe was never locked, no fire is hot enough yet) rather than a mistake to fix. */
    public static final DeferredItem<Item> GRAPHITE = ITEMS.registerSimpleItem("graphite");

    /** A full metal, same shape as copper/iron/gold/tin/zinc -- melt, cast, nothing speculative. */
    public static final DeferredItem<Item> LEAD_INGOT = ITEMS.registerSimpleItem("lead_ingot");

    /** A shaped good, not a new thermal process -- ordinary crafting from {@link #COPPER_PLATE}.
     * See {@code recipe/copper_tubing.json}. */
    public static final DeferredItem<Item> COPPER_TUBING = ITEMS.registerSimpleItem("copper_tubing");

    /** Nether quartz's own melt, real vitreous silica ("fused quartz" / "quartz glass" is the
     * genuine term, not an invented one) -- higher purity than sand and a higher softening point
     * to match, rather than the same {@link #GLASS_INGOT} at a hotter number. Sand glass carries
     * whatever else was in the sand; quartz doesn't. See {@code thermal_process/
     * molten_quartz_glass.json}. */
    public static final DeferredItem<Item> QUARTZ_GLASS_INGOT = ITEMS.registerSimpleItem("quartz_glass_ingot");

    /**
     * Perfect instrumentation, and deliberately absent from {@link FCreativeTabs#MAIN} --
     * lives in vanilla's Operator Utilities tab instead, alongside the command block.
     * <p>
     * It is a development cheat, not the top of the instrument ladder -- see
     * {@link DebugHelmetItem}. Anything reachable from the mod's own tab reads as content.
     */
    public static final DeferredItem<DebugHelmetItem> DEBUG_HELMET = ITEMS.register("debug_helmet",
            () -> new DebugHelmetItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<BlockItem> FURNACE = ITEMS.registerSimpleBlockItem(FBlocks.FURNACE);
    public static final DeferredItem<BlockItem> SMOKER = ITEMS.registerSimpleBlockItem(FBlocks.SMOKER);
    public static final DeferredItem<BlockItem> BLAST_FURNACE = ITEMS.registerSimpleBlockItem(FBlocks.BLAST_FURNACE);
    public static final DeferredItem<BlockItem> IMPROVED_FURNACE = ITEMS.registerSimpleBlockItem(FBlocks.IMPROVED_FURNACE);
    public static final DeferredItem<BlockItem> KILN = ITEMS.registerSimpleBlockItem(FBlocks.KILN);
    public static final DeferredItem<BlockItem> ANNEALING_FURNACE = ITEMS.registerSimpleBlockItem(FBlocks.ANNEALING_FURNACE);
    public static final DeferredItem<BlockItem> HEAT_EXCHANGER = ITEMS.registerSimpleBlockItem(FBlocks.HEAT_EXCHANGER);
    public static final DeferredItem<BlockItem> PRESSURE_VESSEL = ITEMS.registerSimpleBlockItem(FBlocks.PRESSURE_VESSEL);

    /** One shape, an ingot -- see {@link MoldItem}'s own doc for why a second shape waits. */
    public static final DeferredItem<MoldItem> INGOT_MOLD =
            ITEMS.register("ingot_mold", () -> new MoldItem(new Item.Properties().stacksTo(1)));

    // --- fitting / data link tech demo --------------------------------------------------------

    /** The mod's first fitting. See {@code fitting.sensor.TemperatureSensorFitting}. */
    public static final DeferredItem<Item> TEMPERATURE_SENSOR =
            ITEMS.register("temperature_sensor", () -> new TemperatureSensorItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> DATA_CONNECTOR =
            ITEMS.register("data_connector", () -> new DataConnectorItem(new Item.Properties().stacksTo(1)));

    /**
     * A development cheat, like {@link DebugHelmetItem} -- deliberately absent from
     * {@link FCreativeTabs#MAIN}; see that class for where it actually lives.
     */
    public static final DeferredItem<BlockItem> DEBUG_CONTROLLER = ITEMS.registerSimpleBlockItem(FBlocks.DEBUG_CONTROLLER);

    private FItems() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }
}
