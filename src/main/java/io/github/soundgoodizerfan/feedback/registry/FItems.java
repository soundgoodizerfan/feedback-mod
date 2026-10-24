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
import io.github.soundgoodizerfan.feedback.item.ReliefValveItem;
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
    public static final DeferredItem<BlockItem> DIAMOND_GRINDING_WHEEL = ITEMS.registerSimpleBlockItem(FBlocks.DIAMOND_GRINDING_WHEEL);

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
                    props -> new GloveItem(props, Feedback.id("wool")), new Item.Properties());

    /**
     * Tongs, made of two rods and a grip by {@code TongsRecipe}; the component says which. A
     * bare entry (creative tab, /give) is a default iron-and-wood pair, so it protects as the recipe's does.
     */
    public static final DeferredItem<TongsItem> TONGS =
            ITEMS.registerItem("tongs", props -> new TongsItem(props.component(FDataComponents.TONGS.get(),
                    new io.github.soundgoodizerfan.feedback.item.Tongs(Feedback.id("iron"), Feedback.id("wood")))),
                    new Item.Properties());

    /** Combat gear curios (bracer slot, belt slot, back slot), baked from materials by {@code GearRecipe}. A blank one does nothing. */
    public static final DeferredItem<io.github.soundgoodizerfan.feedback.combat.gear.GearItem>
            BRACER = ITEMS.registerItem("bracer",
                    props -> new io.github.soundgoodizerfan.feedback.combat.gear.GearItem(props, io.github.soundgoodizerfan.feedback.combat.gear.GearKind.BRACER),
                    new Item.Properties()),
            SASH = ITEMS.registerItem("sash",
                    props -> new io.github.soundgoodizerfan.feedback.combat.gear.GearItem(props, io.github.soundgoodizerfan.feedback.combat.gear.GearKind.SASH),
                    new Item.Properties()),
            FEEDBACKER = ITEMS.registerItem("feedbacker",
                    props -> new io.github.soundgoodizerfan.feedback.combat.gear.GearItem(props.durability(400)
                            // Default components, so a bare /give is the same gauntlet the recipe makes.
                            .component(FDataComponents.GEAR.get(), new io.github.soundgoodizerfan.feedback.combat.gear.GearStats(
                                    Feedback.id("tempered_steel"), Feedback.id("leather"), 1.43f, 0.29f, 0f, 0f, 0f, 0f))
                            .component(FDataComponents.GRANTS.get(), java.util.List.of(
                                    io.github.soundgoodizerfan.feedback.combat.parry.ParryTuning.PUNCH_PROJECTILES)),
                            io.github.soundgoodizerfan.feedback.combat.gear.GearKind.BRACER),
                    new Item.Properties()),
            BACK_PIECE = ITEMS.registerItem("back_piece",
                    props -> new io.github.soundgoodizerfan.feedback.combat.gear.GearItem(props, io.github.soundgoodizerfan.feedback.combat.gear.GearKind.BACK),
                    new Item.Properties());

    /**
     * The slice's first instrument. Registered here with the blocks rather than off in a tools
     * section, because §7 insists an instrument is a yield technology and not a tier -- it sits
     * alongside the machinery it improves, not above it.
     */
    /** Respec consumable for the combat skill trees. */
    public static final DeferredItem<Item> UNLEARNING_TOKEN =
            ITEMS.registerItem("unlearning_token", io.github.soundgoodizerfan.feedback.item.UnlearningTokenItem::new, new Item.Properties().stacksTo(16));

    /** Turns a placed block a quarter turn about the face clicked. */
    public static final DeferredItem<Item> WRENCH =
            ITEMS.registerItem("wrench", io.github.soundgoodizerfan.feedback.item.WrenchItem::new, new Item.Properties().stacksTo(1));

    /** Carries instruments, and its contents read as if loose in the pack. */
    public static final DeferredItem<Item> INSTRUMENT_BAG =
            ITEMS.registerItem("instrument_bag", io.github.soundgoodizerfan.feedback.item.InstrumentBagItem::new,
                    new Item.Properties().stacksTo(1));

    /** A hook on a string line: the second tether source, reels the lighter body. */
    public static final DeferredItem<Item> GRAPPLE =
            ITEMS.registerItem("grapple", io.github.soundgoodizerfan.feedback.item.GrappleItem::new,
                    new Item.Properties().stacksTo(1).durability(io.github.soundgoodizerfan.feedback.modular.ModularTuning.GRAPPLE_DURABILITY));

    /** Crystal plate on a metal backing, hung in the electrolytic cell to read the deposit (mechanics 6.17). */
    public static final DeferredItem<Item> MASS_PROBE = ITEMS.register("mass_probe",
            () -> new io.github.soundgoodizerfan.feedback.item.MassProbeItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> CALIPERS =
            ITEMS.registerItem("calipers", CalipersItem::new, new Item.Properties().stacksTo(1));

    // Beat 1's overrun chain. Every step is a real item, and only the last one is a mistake:
    // foil is a genuine sidegrade that beat 2's thermometer needs. See slice 1, "Overrun".

    // --- beat 2: heat -----------------------------------------------------------------------

    public static final DeferredItem<BlockItem> FIREBOX = ITEMS.registerSimpleBlockItem(FBlocks.FIREBOX);
    public static final DeferredItem<BlockItem> HEARTH = ITEMS.registerSimpleBlockItem(FBlocks.HEARTH);
    public static final DeferredItem<BlockItem> SMALL_CRUCIBLE = ITEMS.registerSimpleBlockItem(FBlocks.SMALL_CRUCIBLE);
    public static final DeferredItem<BlockItem> LARGE_CRUCIBLE = ITEMS.registerSimpleBlockItem(FBlocks.LARGE_CRUCIBLE);
    public static final DeferredItem<BlockItem> INSULATION = ITEMS.registerSimpleBlockItem(FBlocks.INSULATION);
    public static final DeferredItem<BlockItem> GRAPHITE_ORE = ITEMS.registerSimpleBlockItem(FBlocks.GRAPHITE_ORE);
    public static final DeferredItem<BlockItem> DEEPSLATE_GRAPHITE_ORE = ITEMS.registerSimpleBlockItem(FBlocks.DEEPSLATE_GRAPHITE_ORE);
    public static final DeferredItem<BlockItem> COAL_ORE = ITEMS.registerSimpleBlockItem(FBlocks.COAL_ORE);
    public static final DeferredItem<BlockItem> DEEPSLATE_COAL_ORE = ITEMS.registerSimpleBlockItem(FBlocks.DEEPSLATE_COAL_ORE);
    public static final DeferredItem<BlockItem> IRON_ORE = ITEMS.registerSimpleBlockItem(FBlocks.IRON_ORE);
    public static final DeferredItem<BlockItem> DEEPSLATE_IRON_ORE = ITEMS.registerSimpleBlockItem(FBlocks.DEEPSLATE_IRON_ORE);
    public static final DeferredItem<BlockItem> COPPER_ORE = ITEMS.registerSimpleBlockItem(FBlocks.COPPER_ORE);
    public static final DeferredItem<BlockItem> DEEPSLATE_COPPER_ORE = ITEMS.registerSimpleBlockItem(FBlocks.DEEPSLATE_COPPER_ORE);
    public static final DeferredItem<BlockItem> GOLD_ORE = ITEMS.registerSimpleBlockItem(FBlocks.GOLD_ORE);
    public static final DeferredItem<BlockItem> DEEPSLATE_GOLD_ORE = ITEMS.registerSimpleBlockItem(FBlocks.DEEPSLATE_GOLD_ORE);
    public static final DeferredItem<BlockItem> LAPIS_ORE = ITEMS.registerSimpleBlockItem(FBlocks.LAPIS_ORE);
    public static final DeferredItem<BlockItem> DEEPSLATE_LAPIS_ORE = ITEMS.registerSimpleBlockItem(FBlocks.DEEPSLATE_LAPIS_ORE);
    public static final DeferredItem<BlockItem> REDSTONE_ORE = ITEMS.registerSimpleBlockItem(FBlocks.REDSTONE_ORE);
    public static final DeferredItem<BlockItem> DEEPSLATE_REDSTONE_ORE = ITEMS.registerSimpleBlockItem(FBlocks.DEEPSLATE_REDSTONE_ORE);
    public static final DeferredItem<BlockItem> DIAMOND_ORE = ITEMS.registerSimpleBlockItem(FBlocks.DIAMOND_ORE);
    public static final DeferredItem<BlockItem> DEEPSLATE_DIAMOND_ORE = ITEMS.registerSimpleBlockItem(FBlocks.DEEPSLATE_DIAMOND_ORE);
    public static final DeferredItem<BlockItem> EMERALD_ORE = ITEMS.registerSimpleBlockItem(FBlocks.EMERALD_ORE);
    public static final DeferredItem<BlockItem> DEEPSLATE_EMERALD_ORE = ITEMS.registerSimpleBlockItem(FBlocks.DEEPSLATE_EMERALD_ORE);
    public static final DeferredItem<BlockItem> NETHER_GOLD_ORE = ITEMS.registerSimpleBlockItem(FBlocks.NETHER_GOLD_ORE);
    public static final DeferredItem<BlockItem> NETHER_QUARTZ_ORE = ITEMS.registerSimpleBlockItem(FBlocks.NETHER_QUARTZ_ORE);
    public static final DeferredItem<BlockItem> DAMPER = ITEMS.registerSimpleBlockItem(FBlocks.DAMPER);
    public static final DeferredItem<BlockItem> VALVE = ITEMS.registerSimpleBlockItem(FBlocks.VALVE);
    public static final DeferredItem<io.github.soundgoodizerfan.feedback.machine.cable.CableItem> CABLE = ITEMS.register("cable",
            () -> new io.github.soundgoodizerfan.feedback.machine.cable.CableItem(FBlocks.CABLE.get(), new Item.Properties()));
    public static final DeferredItem<io.github.soundgoodizerfan.feedback.machine.cable.CableItem> FENCE = ITEMS.register("fence",
            () -> new io.github.soundgoodizerfan.feedback.machine.cable.CableItem(FBlocks.FENCE.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> GALVANIC_CELL =ITEMS.registerSimpleBlockItem(FBlocks.GALVANIC_CELL);
    public static final DeferredItem<BlockItem> GALVANIC_CELL_PACK = ITEMS.registerSimpleBlockItem(FBlocks.GALVANIC_CELL_PACK);
    public static final DeferredItem<BlockItem> ELECTROMAGNET = ITEMS.registerSimpleBlockItem(FBlocks.ELECTROMAGNET);
    public static final DeferredItem<BlockItem> DYNAMO = ITEMS.registerSimpleBlockItem(FBlocks.DYNAMO);
    public static final DeferredItem<BlockItem> MOTOR = ITEMS.registerSimpleBlockItem(FBlocks.MOTOR);
    public static final DeferredItem<BlockItem> COPPER_COIL = ITEMS.registerSimpleBlockItem(FBlocks.COPPER_COIL);
    public static final DeferredItem<BlockItem> IRON_COIL = ITEMS.registerSimpleBlockItem(FBlocks.IRON_COIL);
    public static final DeferredItem<BlockItem> BREAKER = ITEMS.registerSimpleBlockItem(FBlocks.BREAKER);
    public static final DeferredItem<BlockItem> FUSE = ITEMS.registerSimpleBlockItem(FBlocks.FUSE);
    public static final DeferredItem<BlockItem> COHERER = ITEMS.registerSimpleBlockItem(FBlocks.COHERER);
    public static final DeferredItem<BlockItem> REFLOW_OVEN = ITEMS.registerSimpleBlockItem(FBlocks.REFLOW_OVEN);
    public static final DeferredItem<BlockItem> PHOSPHOR_SCREEN = ITEMS.registerSimpleBlockItem(FBlocks.PHOSPHOR_SCREEN);
    public static final DeferredItem<BlockItem> OSCILLOSCOPE = ITEMS.registerSimpleBlockItem(FBlocks.OSCILLOSCOPE);
    /** A substrate with traces and, once pasted, solder. See {@code process/Board}. */
    public static final DeferredItem<Item> TRACE_BOARD = ITEMS.registerItem("trace_board",
            io.github.soundgoodizerfan.feedback.item.BoardItem::new, new Item.Properties());
    /** A soldered board of mounted parts, carrying its properties. See {@code process/Circuit}. */
    public static final DeferredItem<Item> CIRCUIT = ITEMS.registerItem("basic_electronic_circuit",
            io.github.soundgoodizerfan.feedback.item.CircuitItem::new, new Item.Properties());
    /** Held on a pasted board for a joint; heated at any fire. */
    public static final DeferredItem<Item> SOLDERING_IRON = ITEMS.registerItem("soldering_iron",
            io.github.soundgoodizerfan.feedback.item.SolderingIronItem::new, new Item.Properties());
    /** Says how a placed part responds, at its own resolution. */
    public static final DeferredItem<Item> COMPONENT_TESTER = ITEMS.registerItem("component_tester",
            io.github.soundgoodizerfan.feedback.item.ComponentTesterItem::new, new Item.Properties().stacksTo(1));
    public static final DeferredItem<BlockItem> WHISKER = ITEMS.registerSimpleBlockItem(FBlocks.WHISKER);
    public static final DeferredItem<BlockItem> RELAY = ITEMS.registerSimpleBlockItem(FBlocks.RELAY);
    /** Rectifying crystals (mechanics 6.18); no deposit yet, creative tab only. */
    public static final DeferredItem<Item> GALENA = ITEMS.registerSimpleItem("galena");
    public static final DeferredItem<Item> PYRITE = ITEMS.registerSimpleItem("pyrite");
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

    /** Reads a boiler's or pressure vessel's pressure; the thermometer's sibling. */
    public static final DeferredItem<Item> PRESSURE_GAUGE =
            ITEMS.registerItem("pressure_gauge", io.github.soundgoodizerfan.feedback.item.PressureGaugeItem::new,
                    new Item.Properties().stacksTo(1));

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

    public static final DeferredItem<BlockItem> TIN_ORE = ITEMS.registerSimpleBlockItem(FBlocks.TIN_ORE);
    public static final DeferredItem<BlockItem> DEEPSLATE_TIN_ORE = ITEMS.registerSimpleBlockItem(FBlocks.DEEPSLATE_TIN_ORE);
    public static final DeferredItem<BlockItem> ZINC_ORE = ITEMS.registerSimpleBlockItem(FBlocks.ZINC_ORE);
    public static final DeferredItem<BlockItem> DEEPSLATE_ZINC_ORE = ITEMS.registerSimpleBlockItem(FBlocks.DEEPSLATE_ZINC_ORE);
    public static final DeferredItem<BlockItem> LEAD_ORE = ITEMS.registerSimpleBlockItem(FBlocks.LEAD_ORE);
    public static final DeferredItem<BlockItem> DEEPSLATE_LEAD_ORE = ITEMS.registerSimpleBlockItem(FBlocks.DEEPSLATE_LEAD_ORE);
    public static final DeferredItem<BlockItem> GALENA_ORE = ITEMS.registerSimpleBlockItem(FBlocks.GALENA_ORE);
    public static final DeferredItem<BlockItem> PYRITE_ORE = ITEMS.registerSimpleBlockItem(FBlocks.PYRITE_ORE);
    public static final DeferredItem<BlockItem> KAOLINITE_DEPOSIT = ITEMS.registerSimpleBlockItem(FBlocks.KAOLINITE_DEPOSIT);

    /** What tin, zinc and lead ore drop: ore as dug, before smelting (vanilla has raw_iron, raw_copper, raw_gold). */
    public static final DeferredItem<Item> RAW_TIN = ITEMS.registerSimpleItem("raw_tin");
    public static final DeferredItem<Item> RAW_ZINC = ITEMS.registerSimpleItem("raw_zinc");
    public static final DeferredItem<Item> RAW_LEAD = ITEMS.registerSimpleItem("raw_lead");

    /** A raw ingot like tin and zinc: no ore or melt yet, there for its material's smite. */
    public static final DeferredItem<Item> SILVER_INGOT = ITEMS.registerSimpleItem("silver_ingot");

    /** A shaped good, not a new thermal process -- ordinary crafting from {@code feedback:copper_plate}.
     * See {@code recipe/copper_tubing.json}. */
    public static final DeferredItem<Item> COPPER_TUBING = ITEMS.registerSimpleItem("copper_tubing");

    /** Cast in the tube mold from molten glass. See {@code casting/glass_tubing.json}. */
    public static final DeferredItem<Item> GLASS_TUBING = ITEMS.registerSimpleItem("glass_tubing");

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

    /** The third mold shape: molten glass cast as a tube (a glass tube is drawn or cast in practice; no blowing minigame). */
    public static final DeferredItem<MoldItem> TUBE_MOLD =
            ITEMS.register("tube_mold", () -> new MoldItem(new Item.Properties().stacksTo(1), "tube"));

    // --- fitting / data link tech demo --------------------------------------------------------

    /** The mod's first fitting. See {@code fitting.sensor.TemperatureSensorFitting}. */
    public static final DeferredItem<Item> TEMPERATURE_SENSOR =
            ITEMS.register("temperature_sensor", () -> new TemperatureSensorItem(new Item.Properties().stacksTo(1)));

    /** Slice 4's instruments (mechanics 6.7): carried, and a fitting on a wire. */
    public static final DeferredItem<Item> GALVANOMETER =
            ITEMS.register("galvanometer", () -> new GalvanometerItem(new Item.Properties().stacksTo(1)));
    /** A Leyden jar on a handle: draws a jar's charge, spends it on one hit. See {@code JarBatonItem}. */
    public static final DeferredItem<Item> JAR_BATON =
            ITEMS.register("jar_baton", () -> new io.github.soundgoodizerfan.feedback.item.JarBatonItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> ELECTROSCOPE =
            ITEMS.register("electroscope", () -> new ElectroscopeItem(new Item.Properties().stacksTo(1)));
    /** The galvanometer as a fitting. See {@code fitting.sensor.CurrentSensorFitting}. */
    public static final DeferredItem<Item> CURRENT_SENSOR =
            ITEMS.register("current_sensor", () -> new CurrentSensorItem(new Item.Properties().stacksTo(1)));

    /** The first {@code UpgradeFitting}. See {@code fitting.upgrade.RefractoryLiningFitting}. */
    public static final DeferredItem<Item> REFRACTORY_LINING =
            ITEMS.register("refractory_lining", () -> new RefractoryLiningItem(new Item.Properties().stacksTo(16)));

    /** The boiler's relief valve. See {@code fitting.upgrade.ReliefValveFitting}. */
    public static final DeferredItem<Item> RELIEF_VALVE =
            ITEMS.register("relief_valve", () -> new ReliefValveItem(new Item.Properties().stacksTo(16)));

    public static final DeferredItem<Item> DATA_CONNECTOR =
            ITEMS.register("data_connector", () -> new DataConnectorItem(new Item.Properties().stacksTo(1)));

    /**
     * A development cheat, like {@link DebugHelmetItem} -- deliberately absent from
     * {@link FCreativeTabs#MAIN}; see that class for where it actually lives.
     */
    public static final DeferredItem<BlockItem> CREATIVE_HEAT = ITEMS.registerSimpleBlockItem(FBlocks.CREATIVE_HEAT);
    public static final DeferredItem<BlockItem> CREATIVE_ROTATION = ITEMS.registerSimpleBlockItem(FBlocks.CREATIVE_ROTATION);
    public static final DeferredItem<BlockItem> CREATIVE_XP = ITEMS.registerSimpleBlockItem(FBlocks.CREATIVE_XP);
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
