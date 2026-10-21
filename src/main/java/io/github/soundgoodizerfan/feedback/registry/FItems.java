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
import io.github.soundgoodizerfan.feedback.item.BeltItem;
import io.github.soundgoodizerfan.feedback.item.CalipersItem;
import io.github.soundgoodizerfan.feedback.item.CurrentSensorItem;
import io.github.soundgoodizerfan.feedback.item.ElectroscopeItem;
import io.github.soundgoodizerfan.feedback.item.GalvanometerItem;
import io.github.soundgoodizerfan.feedback.item.DataConnectorItem;
import io.github.soundgoodizerfan.feedback.item.DebugHelmetItem;
import io.github.soundgoodizerfan.feedback.item.GloveItem;
import io.github.soundgoodizerfan.feedback.item.HandHammerItem;
import io.github.soundgoodizerfan.feedback.item.TongsItem;
import io.github.soundgoodizerfan.feedback.item.MoldItem;
import io.github.soundgoodizerfan.feedback.item.RefractoryLiningItem;
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
    public static final DeferredItem<BlockItem> BEARING = ITEMS.registerSimpleBlockItem(FBlocks.BEARING);
    public static final DeferredItem<BlockItem> FLYWHEEL = ITEMS.registerSimpleBlockItem(FBlocks.FLYWHEEL);
    public static final DeferredItem<BlockItem> SMALL_COG = ITEMS.registerSimpleBlockItem(FBlocks.SMALL_COG);
    public static final DeferredItem<BlockItem> LARGE_COG = ITEMS.registerSimpleBlockItem(FBlocks.LARGE_COG);
    public static final DeferredItem<BlockItem> GEARBOX = ITEMS.registerSimpleBlockItem(FBlocks.GEARBOX);
    public static final DeferredItem<BlockItem> HAND_CRANK = ITEMS.registerSimpleBlockItem(FBlocks.HAND_CRANK);
    public static final DeferredItem<BlockItem> WATER_WHEEL = ITEMS.registerSimpleBlockItem(FBlocks.WATER_WHEEL);

    public static final DeferredItem<BlockItem> CRANK_LINKAGE = ITEMS.registerSimpleBlockItem(FBlocks.CRANK_LINKAGE);
    public static final DeferredItem<BlockItem> MECHANICAL_HAMMER = ITEMS.registerSimpleBlockItem(FBlocks.MECHANICAL_HAMMER);

    public static final DeferredItem<BlockItem> WIRE_DRAWER = ITEMS.registerSimpleBlockItem(FBlocks.WIRE_DRAWER);
    public static final DeferredItem<BlockItem> ROLLING_MILL = ITEMS.registerSimpleBlockItem(FBlocks.ROLLING_MILL);
    public static final DeferredItem<BlockItem> MECHANICAL_PRESS = ITEMS.registerSimpleBlockItem(FBlocks.MECHANICAL_PRESS);

    public static final DeferredItem<BlockItem> DRILL_PRESS = ITEMS.registerSimpleBlockItem(FBlocks.DRILL_PRESS);




    public static final DeferredItem<BlockItem> LATHE = ITEMS.registerSimpleBlockItem(FBlocks.LATHE);

    /** A physical, wearable component -- philosophy 4's "upgrades are physical components," and
     * the Lathe's {@code Wearing} condition lives on it. See {@code FDataComponents#TOOL_CONDITION}. */
    public static final DeferredItem<Item> LATHE_TOOL_BIT = ITEMS.registerSimpleItem("lathe_tool_bit");

    public static final DeferredItem<BlockItem> GRINDING_WHEEL = ITEMS.registerSimpleBlockItem(FBlocks.GRINDING_WHEEL);

    /** The one route through hardened tool steel -- see {@code removal/hardened_steel.json}. */
    public static final DeferredItem<Item> GROUND_HARDENED_STEEL = ITEMS.registerSimpleItem("ground_hardened_steel");

    public static final DeferredItem<BlockItem> SMALL_PULLEY = ITEMS.registerSimpleBlockItem(FBlocks.SMALL_PULLEY);
    public static final DeferredItem<BlockItem> LARGE_PULLEY = ITEMS.registerSimpleBlockItem(FBlocks.LARGE_PULLEY);

    /** Links two pulleys -- see {@code BeltItem}/{@code BeltLinkManager}. A reusable tool, not
     * consumed per link, the same standing as the Data Connector. */
    public static final DeferredItem<Item> BELT = ITEMS.register("belt",
            () -> new BeltItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<BlockItem> CLUTCH = ITEMS.registerSimpleBlockItem(FBlocks.CLUTCH);
    public static final DeferredItem<BlockItem> OVERSPEED_TRIP = ITEMS.registerSimpleBlockItem(FBlocks.OVERSPEED_TRIP);

    // Machine shop intermediates. Rod is a Rolling Mill output (Operation.ROLL); wire is a Wire
    // Drawer output (Operation.DRAW), drawn from rod. See data/feedback/deformation/.

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
            ITEMS.registerItem("punch_card", Item::new, new Item.Properties());

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
     * Gloves, a curio in the {@code hands} slot, made of leather and rated by leather's own file
     * (see {@code GloveItem}). Philosophy 9: a glowing ingot is a glowing ingot wherever it is, in
     * a hand included -- this is what stands between the two.
     */
    public static final DeferredItem<GloveItem> LEATHER_GLOVES =
            ITEMS.registerItem("leather_gloves",
                    props -> new GloveItem(props, Feedback.id("leather")), new Item.Properties());

    /**
     * Tongs, made of two rods and a grip by {@code TongsRecipe}; the component says which. A
     * blank pair (from a command or the creative tab's bare entry) protects nothing.
     */
    public static final DeferredItem<TongsItem> TONGS =
            ITEMS.registerItem("tongs", TongsItem::new, new Item.Properties());

    /**
     * The slice's first instrument. Registered here with the blocks rather than off in a tools
     * section, because §7 insists an instrument is a yield technology and not a tier -- it sits
     * alongside the machinery it improves, not above it.
     */
    /** Turns a placed block a quarter turn about the face clicked. */
    public static final DeferredItem<Item> WRENCH =
            ITEMS.registerItem("wrench", io.github.soundgoodizerfan.feedback.item.WrenchItem::new, new Item.Properties().stacksTo(1));

    /** Carries instruments, and its contents read as if loose in the pack. */
    public static final DeferredItem<Item> INSTRUMENT_BAG =
            ITEMS.registerItem("instrument_bag", io.github.soundgoodizerfan.feedback.item.InstrumentBagItem::new,
                    new Item.Properties().stacksTo(1));

    public static final DeferredItem<Item> CALIPERS =
            ITEMS.registerItem("calipers", CalipersItem::new, new Item.Properties().stacksTo(1));

    // Beat 1's overrun chain. Every step is a real item, and only the last one is a mistake:
    // foil is a genuine sidegrade that beat 2's thermometer needs. See slice 1, "Overrun".

    // --- beat 2: heat -----------------------------------------------------------------------

    public static final DeferredItem<BlockItem> FIREBOX = ITEMS.registerSimpleBlockItem(FBlocks.FIREBOX);
    public static final DeferredItem<BlockItem> SMALL_CRUCIBLE = ITEMS.registerSimpleBlockItem(FBlocks.SMALL_CRUCIBLE);
    public static final DeferredItem<BlockItem> LARGE_CRUCIBLE = ITEMS.registerSimpleBlockItem(FBlocks.LARGE_CRUCIBLE);
    public static final DeferredItem<BlockItem> INSULATION = ITEMS.registerSimpleBlockItem(FBlocks.INSULATION);
    public static final DeferredItem<BlockItem> DAMPER = ITEMS.registerSimpleBlockItem(FBlocks.DAMPER);
    public static final DeferredItem<BlockItem> VALVE = ITEMS.registerSimpleBlockItem(FBlocks.VALVE);
    public static final DeferredItem<io.github.soundgoodizerfan.feedback.machine.cable.CableItem> CABLE = ITEMS.register("cable",
            () -> new io.github.soundgoodizerfan.feedback.machine.cable.CableItem(FBlocks.CABLE.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> GALVANIC_CELL = ITEMS.registerSimpleBlockItem(FBlocks.GALVANIC_CELL);
    public static final DeferredItem<BlockItem> ELECTROMAGNET = ITEMS.registerSimpleBlockItem(FBlocks.ELECTROMAGNET);
    public static final DeferredItem<BlockItem> DYNAMO = ITEMS.registerSimpleBlockItem(FBlocks.DYNAMO);
    public static final DeferredItem<BlockItem> MOTOR = ITEMS.registerSimpleBlockItem(FBlocks.MOTOR);
    public static final DeferredItem<BlockItem> COPPER_COIL = ITEMS.registerSimpleBlockItem(FBlocks.COPPER_COIL);
    public static final DeferredItem<BlockItem> IRON_COIL = ITEMS.registerSimpleBlockItem(FBlocks.IRON_COIL);
    public static final DeferredItem<BlockItem> BREAKER = ITEMS.registerSimpleBlockItem(FBlocks.BREAKER);
    public static final DeferredItem<BlockItem> FUSE = ITEMS.registerSimpleBlockItem(FBlocks.FUSE);
    public static final DeferredItem<BlockItem> ELECTROLYTIC_CELL =
            ITEMS.registerSimpleBlockItem(FBlocks.ELECTROLYTIC_CELL);
    public static final DeferredItem<BlockItem> LEYDEN_JAR = ITEMS.registerSimpleBlockItem(FBlocks.LEYDEN_JAR);
    public static final DeferredItem<BlockItem> CHARGER = ITEMS.registerSimpleBlockItem(FBlocks.CHARGER);
    public static final DeferredItem<BlockItem> ELECTROSTATIC_GENERATOR =
            ITEMS.registerSimpleBlockItem(FBlocks.ELECTROSTATIC_GENERATOR);
    public static final DeferredItem<BlockItem> BELLOWS = ITEMS.registerSimpleBlockItem(FBlocks.BELLOWS);
    public static final DeferredItem<BlockItem> BIMETALLIC_STRIP = ITEMS.registerSimpleBlockItem(FBlocks.BIMETALLIC_STRIP);

    public static final DeferredItem<BlockItem> BOILER = ITEMS.registerSimpleBlockItem(FBlocks.BOILER);
    public static final DeferredItem<BlockItem> STEAM_ENGINE = ITEMS.registerSimpleBlockItem(FBlocks.STEAM_ENGINE);
    public static final DeferredItem<BlockItem> CONDENSER = ITEMS.registerSimpleBlockItem(FBlocks.CONDENSER);
    public static final DeferredItem<BlockItem> PUMP = ITEMS.registerSimpleBlockItem(FBlocks.PUMP);

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

    /** A shaped good, not a new thermal process -- ordinary crafting from {@code feedback:copper_plate}.
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

    /** The second mold shape: the same melt, cast as bullets ({@code docs/feedback_guns_spec.md} section 5). */
    public static final DeferredItem<MoldItem> BULLET_MOLD =
            ITEMS.register("bullet_mold", () -> new MoldItem(new Item.Properties().stacksTo(1), "bullet"));

    // --- fitting / data link tech demo --------------------------------------------------------

    /** The mod's first fitting. See {@code fitting.sensor.TemperatureSensorFitting}. */
    public static final DeferredItem<Item> TEMPERATURE_SENSOR =
            ITEMS.register("temperature_sensor", () -> new TemperatureSensorItem(new Item.Properties().stacksTo(1)));

    /** Slice 4's instruments (mechanics 6.7): carried, and a fitting on a wire. */
    public static final DeferredItem<Item> GALVANOMETER =
            ITEMS.register("galvanometer", () -> new GalvanometerItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> ELECTROSCOPE =
            ITEMS.register("electroscope", () -> new ElectroscopeItem(new Item.Properties().stacksTo(1)));
    /** The galvanometer as a fitting. See {@code fitting.sensor.CurrentSensorFitting}. */
    public static final DeferredItem<Item> CURRENT_SENSOR =
            ITEMS.register("current_sensor", () -> new CurrentSensorItem(new Item.Properties().stacksTo(1)));

    /** The first {@code UpgradeFitting}. See {@code fitting.upgrade.RefractoryLiningFitting}. */
    public static final DeferredItem<Item> REFRACTORY_LINING =
            ITEMS.register("refractory_lining", () -> new RefractoryLiningItem(new Item.Properties().stacksTo(16)));

    public static final DeferredItem<Item> DATA_CONNECTOR =
            ITEMS.register("data_connector", () -> new DataConnectorItem(new Item.Properties().stacksTo(1)));

    /**
     * A development cheat, like {@link DebugHelmetItem} -- deliberately absent from
     * {@link FCreativeTabs#MAIN}; see that class for where it actually lives.
     */
    public static final DeferredItem<BlockItem> CREATIVE_HEAT = ITEMS.registerSimpleBlockItem(FBlocks.CREATIVE_HEAT);
    public static final DeferredItem<BlockItem> CREATIVE_ROTATION = ITEMS.registerSimpleBlockItem(FBlocks.CREATIVE_ROTATION);
    public static final DeferredItem<BlockItem> CREATIVE_ELECTRIC = ITEMS.registerSimpleBlockItem(FBlocks.CREATIVE_ELECTRIC);
    public static final DeferredItem<BlockItem> CREATIVE_CONTROLLER = ITEMS.registerSimpleBlockItem(FBlocks.CREATIVE_CONTROLLER);

    /** A development cheat, like {@link DebugHelmetItem}: prints an item's material file. */
    public static final DeferredItem<io.github.soundgoodizerfan.feedback.item.MaterialProbeItem> MATERIAL_PROBE =
            ITEMS.register("material_probe",
                    () -> new io.github.soundgoodizerfan.feedback.item.MaterialProbeItem(new Item.Properties().stacksTo(1)));

    private FItems() {
    }

    public static void register(IEventBus modBus) {
        FormItems.registerAll(FBlocks.BLOCKS, ITEMS);
        ITEMS.register(modBus);
    }
}
