// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.registry;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.combat.gear.GearStats;
import io.github.soundgoodizerfan.feedback.core.unit.VoltageBand;
import io.github.soundgoodizerfan.feedback.machine.cable.CableGauge;
import io.github.soundgoodizerfan.feedback.machine.cable.CableItem;
import io.github.soundgoodizerfan.feedback.process.Cut;
import io.github.soundgoodizerfan.feedback.process.MadeOf;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.weapon.Cartridge;
import io.github.soundgoodizerfan.feedback.weapon.GunTuning;
import io.github.soundgoodizerfan.feedback.weapon.Magazine;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The creative menu: seven tabs, one sort rule set. Read this before adding an item.
 *
 * <h2>Which tab</h2>
 * Ask what the player does with the thing, top to bottom; the first yes wins.
 * <ol>
 *   <li><b>Weapons &amp; Combat</b>: it hurts, shields, fires, or moves the wearer by force (guns, ammunition and
 *       the stations that make it, melee gear, shield, grapple).</li>
 *   <li><b>Food &amp; Farming</b>: it is eaten, cooked with, grown, or a kitchen station.</li>
 *   <li><b>Control &amp; Vehicles</b>: it senses, decides, signals or steers (sensors, controllers, computers,
 *       disks, redstone devices, vehicle bodies, seat, thruster, tires).</li>
 *   <li><b>Tools &amp; Gear</b>: it is held, worn or read, not placed (hand tools, instruments, worn gear,
 *       knowledge items).</li>
 *   <li><b>Machines &amp; Stations</b>: it is placed and works, or is a bench (furnaces, power, fluids,
 *       logistics, reactors, cultures).</li>
 *   <li><b>Parts &amp; Components</b>: it is built into something else (rotation parts, joints, coils, cables,
 *       heads, electrodes).</li>
 *   <li><b>Materials</b>: it is stuff (generated forms, ingots, ores, soils, intermediates, buckets).</li>
 * </ol>
 *
 * <h2>Order inside a tab</h2>
 * Sections run in the order the player meets them. Inside a section, items are grouped by family (every mold
 * together, every cable together, every furnace together). Inside a family, simple comes before advanced: by the
 * era the thing needs (hand work, then mechanical, then thermal and chemical, then electric, then computing), and a
 * bare variant before a refined one. Per-material items (one per material that can be one) stay together as one
 * family, in material file order, which is what {@link FormItems#materials()} gives.
 *
 * <h2>Editing</h2>
 * <ul>
 *   <li>A section is a {@code List<DeferredItem<?>>} below, or a method for items built as stacks (per material,
 *       per variant). Put a new item in the section it belongs to, where the tier rule puts it.</li>
 *   <li>Every item lists in exactly one tab. The Tools tab ends with {@link #unclaimedModular()}: a
 *       {@link FModular} item that no section names lands there in registration order, so a new modular type
 *       shows up even when forgotten; the fix is to name it in its section.</li>
 *   <li>Development cheats go in {@link #buildOperatorTab}, never in a tab here.</li>
 *   <li>JEI categories have their own order, documented on {@code FeedbackJeiPlugin#registerCategories}.</li>
 * </ul>
 */
public class FCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Feedback.MOD_ID);

    // ---- Tools & Gear ----

    /** Hand tools: grip and prise, shape, then powered hand tools. */
    private static final List<DeferredItem<?>> HAND_TOOLS = of(FItems.TONGS, FItems.WRENCH, FItems.MORTAR,
            FModular.HAFTED_TOOL, FModular.SAWBLADE, FModular.SHEARS, FModular.GLOVE, FModular.SCRATCH_FILES,
            FItems.JAR_BATON, FItems.SOLDERING_IRON, FItems.WICK, FItems.ARC_WELDER, FItems.POWER_CUTTER);

    /** Instruments by what they read: size and mass, heat and pressure, light and field, electricity, matter. */
    private static final List<DeferredItem<?>> INSTRUMENTS = of(FItems.INSTRUMENT_BAG,
            FItems.CALIPERS, FModular.DRAW_GAUGE, FModular.BALANCE_SCALE, FModular.SPECTROMETER, FModular.RADIO_SET, FModular.HAMMER_CAM, FItems.COHERER_CLAPPER, FModular.CAMERA, FItems.SORT_GAUGE,
            FItems.THERMOMETER, FItems.PYROMETER, FItems.PRESSURE_GAUGE, FItems.HYDROMETER,
            FItems.LIGHT_METER, FItems.REACH_PROBE, FItems.XP_METER, FItems.FLUX_METER,
            FItems.ELECTROSCOPE, FItems.GALVANOMETER, FItems.COMPONENT_TESTER, FItems.OSCILLOSCOPE,
            FModular.SURVEY_METER, FModular.GEIGER_COUNTER, FModular.DOSIMETER, FModular.TEST_STRIP);

    /** Worn, head to foot, then what the wearer carries. */
    private static final List<DeferredItem<?>> WORN = of(FModular.HELMET, FModular.CHESTPLATE, FModular.LEGGINGS,
            FModular.BOOTS, FModular.RESPIRATOR, FModular.SUIT_MODULE, FModular.BACKPACK, FModular.BED);

    /** Knowledge: blank, then written, then what holds it. */
    private static final List<DeferredItem<?>> KNOWLEDGE = of(FModular.BLANK_TREATISE, FModular.BLANK_RITE, FModular.BLUEPRINT,
            FModular.BLUEPRINT_BOOK, FModular.TREATISE_BINDER, FItems.UNLEARNING_TOKEN);

    // ---- Weapons & Combat ----

    /** Ranged, simple to advanced: bows, spring, powder, electric. */
    private static final List<DeferredItem<?>> RANGED = of(FModular.BOW, FModular.CROSSBOW, FModular.LAUNCHER,
            FWeapons.GUN, FModular.ELECTRIC_GUN, FModular.CASTER);

    private static final List<DeferredItem<?>> MELEE_AND_DEFENCE = of(FModular.SHIELD, FItems.WHIPLASH,
            FItems.FEEDBACKER, FItems.KNUCKLEBLASTER, FItems.GRAPPLE, FItems.GRAPPLE_WINCH);

    /** Ammunition making: moulds, then the mixing and loading stations, then charges. */
    private static final List<DeferredItem<?>> AMMUNITION_MAKING = of(FItems.BULLET_MOLD, FItems.BALL_MOLD,
            FWeapons.MIXER_ITEM, FWeapons.RELOADING_PRESS_ITEM, FModular.CHARGE, FWeapons.DEMOLITION_CHARGE_ITEM);

    // ---- Food & Farming ----

    private static final List<DeferredItem<?>> KITCHEN = of(FModular.PREP_TABLE_ITEM, FItems.DRYING_RACK,
            FItems.BREWING_STAND, FModular.SPATULA, FModular.SPOON, FItems.JAR);

    /** Staples and dishes, raw to made. */
    private static final List<DeferredItem<?>> FOOD_ITEMS = of(FItems.DOUGH, FItems.LOAF, FItems.FLATBREAD_DOUGH,
            FItems.FLATBREAD, FItems.PASTRY_DOUGH, FItems.PASTRY_SHELL, FItems.NOODLES, FItems.COOKED_NOODLES,
            FItems.RICE, FItems.STEAMED_RICE, FItems.FRIED_EGG, FItems.ROASTED_PUMPKIN,
            FModular.DISH, FModular.SKEWER, FModular.SOUP, FModular.WRAP, FModular.PIE, FModular.BREAD);

    private static final List<DeferredItem<?>> CROPS = of(FItems.TURNIP, FItems.TOMATO, FItems.ONION, FItems.GARLIC,
            FItems.CABBAGE, FItems.CORN, FItems.SOYBEAN, FItems.CHILE_PEPPER, FItems.GINGER, FItems.PEPPER,
            FItems.FERTILISER);

    // ---- Control & Vehicles ----

    /** Signals: a note, a redstone line, then sensors by what they sense. */
    private static final List<DeferredItem<?>> SIGNALS = of(FItems.REDSTONE_EMITTER, FItems.REDSTONE_RECEIVER,
            FItems.ANALOG_ADAPTER, FItems.TEMPERATURE_SENSOR, FItems.CURRENT_SENSOR, FItems.DATA_SENSOR, FItems.RADIATION_SENSOR, FItems.AURA_SENSOR);

    /** Controllers: mechanical, electric, then computing; media and cards last. */
    private static final List<DeferredItem<?>> CONTROLLERS = of(FItems.TIMER, FItems.CONTROLLER, FItems.PROGRAMMER,
            FItems.ELECTRIC_CONTROLLER, FItems.ELECTRIC_PROGRAMMER, FItems.COMPUTER, FItems.PUNCH_CARD,
            FItems.MEMORY_BOARD, FItems.FLOPPY_DISK, FItems.USB_DRIVE, FItems.FUNCTION_CARD, FItems.MEMORY_DRUM,
            FItems.CARD_RACK, FItems.USB_PORT, FItems.ENDERNET_UPGRADE, FItems.DATA_STICK, FItems.DATA_CONNECTOR);

    /** Vehicle bodies: lift, gas cells (cloth to membrane), valves and tanks, seat, thrust, ground contact. */
    private static final List<DeferredItem<?>> VEHICLE_PARTS = of(FVehicles.LIFT_ITEM, FVehicles.GAS_CELL_CLOTH_ITEM,
            FVehicles.GAS_CELL_RUBBER_ITEM, FVehicles.GAS_CELL_PLATE_ITEM, FVehicles.GAS_CELL_BLADDER_ITEM,
            FVehicles.GAS_CELL_MEMBRANE_ITEM, FVehicles.VENT_VALVE_ITEM, FVehicles.BALLAST_TANK_ITEM,
            FVehicles.FLIGHT_SENSOR_ITEM, FVehicles.BEARING_SENSOR_ITEM, FVehicles.MICRO_CORE, FVehicles.VOXEL_CASE, FVehicles.SEAT_ITEM, FModular.THRUSTER, FModular.JET,
            FItems.TIRE_RUBBER);

    // ---- Machines & Stations ----

    /** Benches: where designs and parts are made, hand work first. */
    private static final List<DeferredItem<?>> STATIONS = of(FModular.BLUEPRINT_TABLE_ITEM, FModular.TREATISE_SHELF_ITEM,
            FModular.WOODWORKING_TABLE_ITEM, FModular.STONECUTTER_ITEM, FModular.SMITHING_TABLE_ITEM,
            FModular.ANVIL_ITEM, FModular.ENGINEERING_TABLE_ITEM, FModular.RESEARCH_TABLE_ITEM, FModular.ENCHANTING_TABLE_ITEM, FItems.AUTO_ENGINEER);

    /** Fire and furnaces (hand-fed to powered), crucibles, casting and moulds, then cold. */
    private static final List<DeferredItem<?>> HEAT = of(FItems.CAMPFIRE, FItems.SOUL_CAMPFIRE, FItems.FIREBOX,
            FItems.HEARTH, FItems.FURNACE, FItems.SMOKER, FItems.BLAST_FURNACE, FItems.IMPROVED_FURNACE,
            FItems.KILN, FItems.ANNEALING_FURNACE, FItems.PUDDLING_FURNACE, FItems.ARC_HEARTH, FItems.ARC_HEAD,
            FItems.INDUCTION_STOVE, FItems.REFLOW_OVEN, FModular.BURNER, FModular.BOILER, FModular.HEAT_EXCHANGER,
            FModular.THERMAL_STORE, FModular.CRUCIBLE, FItems.CASTING_BASIN,
            FItems.INGOT_MOLD, FItems.TUBE_MOLD, FItems.GLASS_BLOCK_MOLD, FItems.GLASS_PANE_MOLD,
            FItems.GLASS_BOTTLE_MOLD, FItems.PICKAXE_HEAD_MOLD, FItems.AXE_HEAD_MOLD, FItems.SHOVEL_HEAD_MOLD,
            FItems.HOE_HEAD_MOLD, FItems.SICKLE_HEAD_MOLD, FItems.HAMMER_HEAD_MOLD, FItems.SAW_BLADE_MOLD, FItems.ATOMISING_MOLD,
            FItems.ICE_BOX, FItems.REFRIGERATION_UNIT, FItems.CRYOCOOLER, FItems.VACUUM_DRYER, FItems.THRESHER,
            FItems.SPENT_BLAZE_ROD, FItems.INERT_BLAZE_DUST);

    /** Shaping metal: hammer, draw, roll, press, drill, turn; then the test rigs. */
    private static final List<DeferredItem<?>> METALWORKING = of(FItems.MECHANICAL_HAMMER, FItems.WIRE_DRAWER,
            FItems.ROLLING_MILL, FItems.MECHANICAL_PRESS, FItems.DRILL_PRESS, FItems.GEAR_CUTTER, FItems.LATHE, FItems.VICE,
            FItems.IMPACT_PENDULUM, FItems.STRIKE_POST);

    /** Making motion and charge: muscle and water, steam and gas, electric; then storing and converting it. */
    private static final List<DeferredItem<?>> POWER = of(FModular.HAND_CRANK, FModular.WATER_WHEEL,
            FItems.STEAM_ENGINE, FItems.STEAM_TURBINE, FItems.GAS_TURBINE, FModular.DYNAMOTOR,
            FModular.GALVANIC_CELL, FItems.GALVANIC_CELL_PACK, FModular.THERMOELECTRIC_GENERATOR, FModular.THERMOCOUPLE_PROBE, FItems.ELECTROSTATIC_GENERATOR, FItems.RUB_GENERATOR, FModular.LEYDEN_JAR,
            FModular.BATTERY, FItems.CHARGER, FItems.ENERGY_FITTING, FItems.VIS_TAP, FItems.ROBOT_DOCK, FItems.ROBOT_MAST, FItems.ROBOT_ARM, FItems.ROBOT_BEACON, FItems.ROBOT_REEL, FItems.ROBOT_TETHER, FItems.CHARGING_GANTRY, FItems.RECTIFIER, FItems.CONVERTER,
            FItems.CONVERTER_TAP, FModular.ELECTROMAGNET, FModular.LAMP, FItems.ELECTROLYTIC_CELL, FItems.ELECTROLYSER);

    /** Fluids and gases: move, hold, control, treat, then the oil field. */
    private static final List<DeferredItem<?>> FLUIDS = of(FModular.PUMP, FModular.COMPRESSOR, FItems.VACUUM_PUMP,
            FModular.GAS_TANK, FModular.PRESSURE_VESSEL, FItems.VALVE, FItems.THERMAL_VALVE, FItems.RELIEF_VALVE, FItems.AIR_INTAKE, FItems.RADIATOR_PANEL, FItems.AIR_VALVE,
            FItems.BELLOWS, FItems.DAMPER, FItems.FLARE, FItems.HYDRIDE_BED, FItems.SCRUBBING_BED, FItems.ESSENTIA_VENT, FItems.ESSENTIA_ADAPTER, FItems.CONDENSER, FItems.REBOILER,
            FModular.TRAY, FItems.REACTOR, FModular.GAS_CENTRIFUGE,
            FItems.SEEP, FItems.SOUL_SEEP, FItems.DERRICK_BASE, FItems.DERRICK_CASING, FItems.DERRICK_BIT,
            FItems.WELLHEAD);

    /** Handling: hand-fed blocks, then belts, tubes and pods, then the ender ports. */
    private static final List<DeferredItem<?>> HANDLING = of(FItems.HOPPER, FItems.DROPPER, FItems.DISPENSER, FItems.DISPENSER_ACTUATOR,
            FItems.LEAD_CHEST, FModular.CONVEYOR_BELT);

    private static final List<DeferredItem<?>> TUBES = of(FItems.TUBE, FItems.LINED_TUBE, FItems.WIDE_TUBE,
            FItems.EVACUATED_TUBE, FItems.TUBE_JUNCTION, FItems.PROGRAMMED_JUNCTION, FItems.TUBE_STATION,
            FItems.SORTING_STATION, FModular.LINEAR_MOTOR, FItems.CARRIER, FItems.MAGNETIC_CARRIER, FItems.POD,
            FItems.ENDER_PORT, FItems.DATA_PORT, FItems.OPTICAL_TERMINAL, FItems.ENDER_ANCHOR);

    /** Cultures, taps and relays: the biological and mystical machines. */
    private static final List<DeferredItem<?>> CULTURES = of(FItems.CULTURE_DISH, FItems.CULTURE_SEED,
            FItems.CREEPER_PEN, FItems.CREEPER_SAC, FItems.CREEPER_CHARGE, FItems.RELAY_ORGAN, FItems.ORB_ABSORBER,
            FItems.SCULK_RELAY, FItems.DISENCHANT_STONE, FItems.CATALYST_TAP, FItems.ORE_TAP, FItems.TRADE_TAP,
            FItems.SMELTER_TAP, FItems.ORE_PICKUP, FItems.TOKEN_READER, FItems.CHARGE_CELL, FItems.VESSEL_PORT, FModular.XP_CONTAINER);

    /** Fission: vessel, coolant, control, then fuel and what comes out. */
    private static final List<DeferredItem<?>> REACTORS = of(FItems.REACTOR_CORE, FItems.FUEL_CHANNEL,
            FItems.CONTROL_ASSEMBLY, FItems.SCRAM_TRIP, FItems.COOLANT_PORT, FItems.COOLANT_CHANNEL,
            FItems.NEUTRON_REFLECTOR, FItems.GRAPHITE_MODERATOR, FItems.SHOCK_CAPSULE, FItems.STAR_SEAT,
            FItems.SPENT_FUEL_POOL, FItems.FUEL_ROD, FItems.THORIUM_FUEL_ROD, FItems.ENRICHED_FUEL_ROD,
            FItems.BREEDER_ROD, FItems.SPENT_FUEL_ROD);

    // ---- Parts & Components ----

    /** Rotation, in transmission order: shaft, bearings, gears, belts, then what controls the line. */
    private static final List<DeferredItem<?>> ROTATION = of(FItems.SHAFT, FItems.BEARING, FItems.ROTARY_BEARING,
            FItems.SMALL_COG, FItems.LARGE_COG, FItems.RING_GEAR, FItems.LARGE_RING_GEAR, FItems.SMALL_PULLEY,
            FItems.LARGE_PULLEY, FItems.BELT, FItems.GEARBOX, FItems.DIFFERENTIAL, FItems.FREEWHEEL, FItems.ROTATION_ADAPTER,
            FItems.CLUTCH, FItems.OVERSPEED_TRIP, FItems.ESCAPEMENT, FItems.SHEAVE, FItems.PITON, FItems.WINCH, FModular.FLYWHEEL,
            FItems.MICRO_WEDGE);

    /** Joints: fixed, then free to move. */
    private static final List<DeferredItem<?>> JOINTS = of(FItems.LOCKED_JOINT, FItems.BALL_JOINT,
            FItems.SLIDER_JOINT, FItems.PISTON_JOINT, FItems.CRANK_LINKAGE, FItems.JOINT_LINK);

    /** Machine tooling and linings: what wears or shields inside a machine. */
    private static final List<DeferredItem<?>> TOOLING = of(FItems.GRINDING_WHEEL,
            FItems.MAGMA_HEAD, FItems.REFRACTORY_LINING, FItems.INSULATION, FItems.STARTER, FItems.RADIATOR,
            FItems.CRYSTAL_HOLDER, FItems.HEATSINK, FItems.BIMETALLIC_STRIP);

    /** Coils, then electrical components, passive to active. */
    private static final List<DeferredItem<?>> ELECTRICAL = of(FModular.HEATING_COIL,
            FItems.INDUCTION_COIL, FItems.PREHEAT_COIL, FItems.FUSE, FItems.BREAKER, FItems.COHERER,
            FItems.WHISKER, FItems.GALENA, FItems.PYRITE, FItems.RELAY, FItems.TRACE_BOARD, FItems.PHOSPHOR_SCREEN);

    // ---- Materials ----

    /** Metals, alloys and ceramics as ingots and stock, base metal then alloy, cast then worked. */
    private static final List<DeferredItem<?>> STOCK = of(FItems.TIN_INGOT, FItems.ZINC_INGOT, FItems.LEAD_INGOT,
            FItems.SILVER_INGOT, FItems.BRONZE_INGOT, FItems.BRASS_INGOT, FItems.CAST_IRON, FItems.STEEL_INGOT,
            FItems.TEMPERED_STEEL, FItems.HARDENED_STEEL, FItems.GROUND_HARDENED_STEEL, FItems.CAST_NETHERITE_INGOT,
            FItems.GLASS_INGOT, FItems.QUARTZ_GLASS_INGOT, FItems.COPPER_TUBING, FItems.GLASS_TUBING,
            FItems.CERAMIC, FItems.GLAZED_CERAMIC, FItems.TAINT_GLAZE, FItems.FIREBRICK);

    /** Ores and the ground: stone and deepslate ore side by side, raw before ore, then deposits, plants, soils. */
    private static final List<DeferredItem<?>> ORES = of(FItems.COAL_ORE, FItems.DEEPSLATE_COAL_ORE,
            FItems.IRON_ORE, FItems.DEEPSLATE_IRON_ORE, FItems.COPPER_ORE, FItems.DEEPSLATE_COPPER_ORE,
            FItems.GOLD_ORE, FItems.DEEPSLATE_GOLD_ORE, FItems.LAPIS_ORE, FItems.DEEPSLATE_LAPIS_ORE,
            FItems.REDSTONE_ORE, FItems.DEEPSLATE_REDSTONE_ORE, FItems.DIAMOND_ORE, FItems.DEEPSLATE_DIAMOND_ORE,
            FItems.EMERALD_ORE, FItems.DEEPSLATE_EMERALD_ORE, FItems.NETHER_GOLD_ORE, FItems.NETHER_QUARTZ_ORE,
            FItems.RAW_TIN, FItems.TIN_ORE, FItems.DEEPSLATE_TIN_ORE, FItems.RAW_ZINC, FItems.ZINC_ORE,
            FItems.DEEPSLATE_ZINC_ORE, FItems.RAW_LEAD, FItems.LEAD_ORE, FItems.DEEPSLATE_LEAD_ORE,
            FItems.GRAPHITE_ORE, FItems.DEEPSLATE_GRAPHITE_ORE, FItems.SALTPETRE_ORE, FItems.DEEPSLATE_SALTPETRE_ORE,
            FItems.SULFUR_ORE, FItems.GALENA_ORE, FItems.PYRITE_ORE, FItems.KAOLINITE_DEPOSIT, FItems.SALT_DEPOSIT,
            FItems.COPPER_FLOWER, FItems.ZINC_VIOLET, FItems.BLADDER_CAMPION, FItems.RUSTY_SOIL,
            FItems.BLEACHED_SOIL, FItems.SOOTY_SOIL, FItems.TARNISHED_SOIL, FItems.BLACK_SAND,
            FItems.GOSSAN_RUBBLE, FItems.BASTION_RUBBLE);

    /** Reagents and refined minerals. */
    private static final List<DeferredItem<?>> CHEMICALS = of(FItems.SALT, FItems.SALTPETRE, FItems.SULFUR,
            FItems.GRAPHITE, FItems.KAOLINITE, FItems.ALUMINA, FItems.STRAW, FItems.CHAFF, FItems.HUSK_SAND, FItems.PURIFIED_REDSTONE,
            FItems.DIAMOND_GRIT, FItems.DIAMOND_SEED);

    /** Process intermediates and wastes, in the order the work goes: powders, compacts, blanks, then slags. */
    private static final List<DeferredItem<?>> INTERMEDIATES = of(FItems.BURNT_IRON, FItems.SUPERCONDUCTOR_BLEND,
            FItems.CALCINED_BLEND, FItems.SHEATHED_BILLET, FItems.CERMET_POWDER, FItems.CERMET_GREEN_COMPACT, FItems.SKY_NOTEBOOK, FItems.ALUMINA_GREEN_COMPACT, FItems.ALUMINA_TILE,
            FItems.TUNGSTEN_CARBIDE_POWDER, FItems.TUNGSTEN_CARBIDE_GREEN_COMPACT, FItems.RARE_EARTH_MAGNET_POWDER,
            FItems.RARE_EARTH_MAGNET_GREEN_COMPACT, FItems.RARE_EARTH_MAGNET_BLANK, FItems.OSMIUM_SPONGE,
            FItems.OSMIUM_POWDER, FItems.OSMIUM_GREEN_COMPACT, FItems.IRIDIUM_RESIDUE, FItems.IRIDIUM_SPONGE,
            FItems.IRIDIUM_POWDER, FItems.IRIDIUM_GREEN_COMPACT, FItems.MOLYBDENUM_CALCINE, FItems.RHENIUM_OXIDE,
            FItems.RHENIUM_SPONGE, FItems.RHENIUM_POWDER, FItems.RHENIUM_GREEN_COMPACT, FItems.GOLD_AMALGAM, FItems.NETHERITE_POWDER,
            FItems.NETHERITE_GREEN_COMPACT, FItems.POROUS_AGGREGATE, FItems.POROUS_SINTER, FItems.SYNTHETIC_PEARL,
            FItems.TUNED_EYE, FItems.GHAST_BLADDER, FItems.BLOOMERY_SLAG, FItems.SILICATE_SLAG, FItems.RED_MUD,
            FItems.RARE_EARTH_RESIDUE, FItems.CORE_SLAG, FItems.SCRAP_METAL, FItems.CHARRED_REMAINS,
            FItems.CRACKED_CERAMIC, FItems.WORN_NETHERITE_BITS);

    /** Every section named above, to find the modular items no tab claims. */
    private static final List<List<DeferredItem<?>>> SECTIONS = List.of(HAND_TOOLS, INSTRUMENTS, WORN, KNOWLEDGE,
            RANGED, MELEE_AND_DEFENCE, AMMUNITION_MAKING, KITCHEN, FOOD_ITEMS, CROPS, SIGNALS, CONTROLLERS,
            VEHICLE_PARTS, STATIONS, HEAT, METALWORKING, POWER, FLUIDS, HANDLING, TUBES, CULTURES, REACTORS,
            ROTATION, JOINTS, TOOLING, ELECTRICAL, STOCK, ORES, CHEMICALS, INTERMEDIATES);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TOOLS = tab("tools",
            () -> new ItemStack(FItems.WRENCH.get()), FCreativeTabs::fillTools);
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> WEAPONS = tab("weapons",
            () -> new ItemStack(FWeapons.GUN.get()), FCreativeTabs::fillWeapons);
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MACHINES = tab("machines",
            () -> new ItemStack(FItems.BLAST_FURNACE.get()), FCreativeTabs::fillMachines);
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> PARTS = tab("parts",
            () -> new ItemStack(FItems.SMALL_COG.get()), FCreativeTabs::fillParts);
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CONTROL = tab("control",
            () -> new ItemStack(FItems.CONTROLLER.get()), FCreativeTabs::fillControl);
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> FOOD = tab("food",
            () -> new ItemStack(FItems.LOAF.get()), FCreativeTabs::fillFood);
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MATERIALS = tab("materials",
            () -> new ItemStack(FItems.STEEL_INGOT.get()), FCreativeTabs::fillMaterials);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MOTES = tab("motes",
            () -> io.github.soundgoodizerfan.feedback.vehicle.micro.MoteItem.of(net.minecraft.world.level.block.Blocks.STONE, 1), FCreativeTabs::fillMotes);

    private FCreativeTabs() {
    }

    private static DeferredHolder<CreativeModeTab, CreativeModeTab> tab(String id, Supplier<ItemStack> icon,
                                                                      Consumer<CreativeModeTab.Output> fill) {
        return TABS.register(id, () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.feedback." + id))
                .icon(icon)
                .displayItems((params, output) -> fill.accept(output))
                .build());
    }

    private static List<DeferredItem<?>> of(DeferredItem<?>... items) {
        return List.of(items);
    }

    private static void all(CreativeModeTab.Output out, List<DeferredItem<?>> section) {
        section.forEach(i -> out.accept(i.get()));
    }

    private static void forMaterials(java.util.function.BiConsumer<net.minecraft.resources.ResourceLocation, Material> each) {
        FormItems.materials().forEach(each);
    }

    private static ItemStack madeOf(DeferredItem<?> item, net.minecraft.resources.ResourceLocation id, String form) {
        ItemStack stack = new ItemStack(item.get());
        stack.set(FDataComponents.MADE_OF.get(), new MadeOf(id, form, 1));
        return stack;
    }

    // ---- Tab bodies ----

    private static void fillTools(CreativeModeTab.Output out) {
        all(out, HAND_TOOLS);
        all(out, INSTRUMENTS);
        for (Cut cut : Cut.values()) {
            ItemStack probe = new ItemStack(FItems.MASS_PROBE.get());
            probe.set(FDataComponents.CUT.get(), cut);
            out.accept(probe);
        }
        all(out, WORN);
        all(out, KNOWLEDGE);
        // One exact rite per attunement, so every effect can be tried at its top level.
        io.github.soundgoodizerfan.feedback.modular.attunement.Attunements.known().values().stream().sorted(java.util.Comparator.comparing(e -> e.id().toString()))
                .forEach(e -> out.accept(io.github.soundgoodizerfan.feedback.modular.attunement.Rites.of(e.id(), 1f)));
        unclaimedModular().forEach(out::accept);
    }

    /** Modular items no section names: they land at the end of Tools in registration order. */
    private static List<Item> unclaimedModular() {
        Set<Item> claimed = new HashSet<>();
        SECTIONS.forEach(s -> s.forEach(i -> claimed.add(i.get())));
        claimed.add(FModular.CREATIVE_TREATISE.get());
        return FModular.items().stream().filter(i -> !claimed.contains(i)).toList();
    }

    private static void fillWeapons(CreativeModeTab.Output out) {
        // A plain Gun: the core gives it the gun type's starter design (a 9 mm steel handgun) on first tick.
        all(out, RANGED);
        all(out, MELEE_AND_DEFENCE);
        out.accept(gear(FItems.FEEDBACKER, "tempered_steel", "leather", 1.43f, 0.29f,
                io.github.soundgoodizerfan.feedback.combat.parry.ParryTuning.PUNCH_PROJECTILES));
        out.accept(gear(FItems.KNUCKLEBLASTER, "tempered_steel", "leather", 0f, 0.5f,
                io.github.soundgoodizerfan.feedback.combat.punch.PunchTuning.HEAVY_PUNCH));
        out.accept(gear(FItems.WHIPLASH, "wrought_iron", "wrought_iron", 0f, 0f,
                io.github.soundgoodizerfan.feedback.combat.whip.WhipTuning.WHIPLASH));
        // Ammunition, simple to advanced: the empty magazine, a loaded one, a round, a shotgun shell.
        out.accept(new ItemStack(FWeapons.MAGAZINE_ITEM.get()));
        ItemStack loaded = new ItemStack(FWeapons.MAGAZINE_ITEM.get());
        loaded.set(FWeapons.MAGAZINE.get(), new Magazine(
                Collections.nCopies(GunTuning.GUN_MAGAZINE_STANDARD, Cartridge.reference(9))));
        out.accept(loaded);
        ItemStack round = new ItemStack(FWeapons.CARTRIDGE_ITEM.get());
        round.set(FWeapons.CARTRIDGE.get(), Cartridge.reference(9));
        out.accept(round);
        ItemStack shell = new ItemStack(FWeapons.CARTRIDGE_ITEM.get());
        shell.set(FWeapons.CARTRIDGE.get(), Cartridge.referenceShot(18));
        out.accept(shell);
        all(out, AMMUNITION_MAKING);
    }

    /** The three Feedback gear items in the menu: a worked example of each, with the technique it grants. */
    private static ItemStack gear(DeferredItem<?> item, String head, String grip, float a, float b,
                                  net.minecraft.resources.ResourceLocation grant) {
        ItemStack stack = new ItemStack(item.get());
        stack.set(FDataComponents.GEAR.get(), new GearStats(Feedback.id(head), Feedback.id(grip), a, b, 0f, 0f, 0f, 0f));
        stack.set(FDataComponents.GRANTS.get(), List.of(grant));
        return stack;
    }

    private static void fillMachines(CreativeModeTab.Output out) {
        all(out, STATIONS);
        all(out, HEAT);
        all(out, METALWORKING);
        all(out, POWER);
        all(out, FLUIDS);
        all(out, HANDLING);
        FLogistics.fill(out);
        FStore.fill(out);
        all(out, TUBES);
        all(out, CULTURES);
        all(out, REACTORS);
    }

    private static void fillParts(CreativeModeTab.Output out) {
        all(out, ROTATION);
        all(out, JOINTS);
        all(out, TOOLING);
        // One cut cog per metal a Gear Cutter can bite (it has a plate) and per tooth count.
        forMaterials((id, m) -> {
            if (m.item(id, "plate").isPresent()
                    && m.hardness().value() <= io.github.soundgoodizerfan.feedback.machine.MachineTuning.GEAR_CUTTER_ST.value())
                for (int teeth : io.github.soundgoodizerfan.feedback.machine.cog.CogSpec.OPTIONS) {
                    ItemStack cog = new ItemStack(io.github.soundgoodizerfan.feedback.machine.cog.CogSpec.isLarge(teeth)
                            ? FItems.LARGE_COG.get() : FItems.SMALL_COG.get());
                    cog.set(FDataComponents.COG.get(), new io.github.soundgoodizerfan.feedback.machine.cog.CogSpec(id, teeth));
                    out.accept(cog);
                }
        });
        // One saw head per material hard enough to be one, then a plasma head for those that carry current.
        forMaterials((id, m) -> {
            if (isHard(m))
                out.accept(madeOf(FItems.SAW_HEAD, id, "ingot"));
        });
        forMaterials((id, m) -> {
            if (isHard(m) && m.ampacity().isPresent())
                out.accept(madeOf(FItems.PLASMA_HEAD, id, "ingot"));
        });
        // A waterjet nozzle per hard material (it carries no current, so any hard one).
        forMaterials((id, m) -> {
            if (isHard(m))
                out.accept(madeOf(FItems.WATERJET_HEAD, id, "ingot"));
        });
        // One of each vacuum tube part per material its role can use: an envelope insulates and takes heat, a cathode melts high and emits, a grid conducts, a plate sheds heat.
        for (var part : List.of(new String[] {"tube_envelope", "E"}, new String[] {"tube_cathode", "C"}, new String[] {"tube_grid", "G"}, new String[] {"tube_plate", "P"}))
            FModular.PARTS.stream().filter(p -> p.getId().getPath().equals(part[0])).findFirst().ifPresent(item -> forMaterials((id, m) -> {
                if (io.github.soundgoodizerfan.feedback.modular.bake.TubeDesign.suits(part[1], m))
                    out.accept(madeOf(item, id, m.item(id, "tubing").isPresent() ? "tubing" : m.item(id, "wire").isPresent() ? "wire" : m.item(id, "plate").isPresent() ? "plate" : "ingot"));
            }));
        // One of each resonator and lattice part per material its role can use (a piezo crystal, a conductor, a material that holds experience, an insulator).
        for (String part : List.of("resonator_crystal", "resonator_electrode", "lattice_body", "lattice_frame"))
            FModular.PARTS.stream().filter(p -> p.getId().getPath().equals(part)).findFirst().ifPresent(item -> forMaterials((id, m) -> {
                if (io.github.soundgoodizerfan.feedback.modular.bake.ElementDesign.suits(part, m))
                    out.accept(madeOf(item, id, m.item(id, "crystal_plate").isPresent() ? "crystal_plate" : m.item(id, "foil").isPresent() ? "foil"
                            : m.item(id, "plate").isPresent() ? "plate" : m.item(id, "vein").isPresent() ? "vein" : "shard"));
            }));
        // One electrode per material that can carry an arc.
        forMaterials((id, m) -> {
            if (m.electrodeLimit().isPresent())
                out.accept(io.github.soundgoodizerfan.feedback.machine.arc.ElectrodeItem.of(id));
        });
        all(out, ELECTRICAL);
        // One cable per conductor: every material that states an ampacity; bare, coated per band, other gauges, a post.
        forMaterials((id, m) -> {
            if (m.ampacity().isEmpty())
                return;
            out.accept(CableItem.of(id, 1));
            for (VoltageBand band : VoltageBand.values())
                out.accept(CableItem.of(id, 1, band));
            for (CableGauge gauge : CableGauge.values()) {
                if (gauge == CableGauge.STANDARD || gauge == CableGauge.FINE)
                    continue;
                out.accept(CableItem.of(id, 1, null, gauge));
                out.accept(CableItem.of(id, 1, VoltageBand.LV, gauge));
            }
            out.accept(madeOf(FItems.FENCE, id, "wire"));
        });
        for (CableGauge gauge : CableGauge.values()) {
            if (gauge == CableGauge.STANDARD || gauge == CableGauge.FINE)
                continue;
            ItemStack breaker = new ItemStack(FItems.BREAKER.get());
            breaker.set(FDataComponents.CABLE_GAUGE.get(), gauge);
            out.accept(breaker);
        }
        forMaterials((id, m) -> {
            if (m.xpStore().filter(x -> x.conductance() > 0).isPresent())
                out.accept(io.github.soundgoodizerfan.feedback.machine.xp.XpWireItem.of(id));
        });
        forMaterials((id, m) -> {
            if (m.separation().diamagnetism() > 0f)
                out.accept(io.github.soundgoodizerfan.feedback.machine.levitation.LevitationPlateItem.of(id));
        });
        forMaterials((id, m) -> {
            if (m.separation().diamagnetism() > 0f)
                out.accept(io.github.soundgoodizerfan.feedback.machine.levitation.LevitationSleeveItem.of(id));
        });
        forMaterials((id, m) -> {
            if (m.mechanical().yieldStrength().isPresent())
                out.accept(io.github.soundgoodizerfan.feedback.machine.bearing.CouplingSleeveItem.of(id));
        });
        forMaterials((id, m) -> {
            if (m.electrical().pyroCoupling().filter(c -> c > 0f).isPresent())
                out.accept(io.github.soundgoodizerfan.feedback.machine.pyro.PyroPlateItem.of(id));
        });
    }

    private static boolean isHard(Material m) {
        return m.kind().filter(k -> k == Material.Kind.METAL || k == Material.Kind.CERAMIC
                || k == Material.Kind.CRYSTAL).isPresent();
    }

    private static void fillControl(CreativeModeTab.Output out) {
        all(out, SIGNALS);
        all(out, CONTROLLERS);
        for (String app : io.github.soundgoodizerfan.feedback.control.computer.ComputerApps.DISK_APPS)
            out.accept(io.github.soundgoodizerfan.feedback.control.computer.ComputerApps.disk(app));
        FAutomation.fill(out);
        all(out, VEHICLE_PARTS);
        FVehicles.TIRE_HUB_ITEMS.forEach(hub -> out.accept(hub.get()));
    }

    /** A mote per block that can be cut into motes (the creative tab takes single stacks only). */
    private static void fillMotes(CreativeModeTab.Output out) {
        net.minecraft.core.registries.BuiltInRegistries.BLOCK.stream()
                .filter(b -> b.asItem() != net.minecraft.world.item.Items.AIR && io.github.soundgoodizerfan.feedback.vehicle.micro.MoteItem.eligible(b.defaultBlockState()))
                .forEach(b -> out.accept(io.github.soundgoodizerfan.feedback.vehicle.micro.MoteItem.of(b, 1)));
        // A fine build of every type a cell hosts, as its starter design: the machine finespace takes from a quarter of the parts.
        var materials = FormItems.materials();
        var types = io.github.soundgoodizerfan.feedback.modular.type.ModularTypes.known();
        types.keySet().stream().filter(io.github.soundgoodizerfan.feedback.vehicle.micro.FineHosts::hostableType).sorted().forEach(id ->
                io.github.soundgoodizerfan.feedback.modular.blueprint.ToolDesigns.starter(id, types, materials).ifPresent(design -> {
                    ItemStack stack = new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id));
                    if (io.github.soundgoodizerfan.feedback.modular.item.ModularStacks.assemble(stack, design.withFine(true), materials, types))
                        out.accept(stack);
                }));
    }

    private static void fillFood(CreativeModeTab.Output out) {
        all(out, KITCHEN);
        // One skillet and one stock pot per material that is plate and conducts heat.
        // items(id), not forms(): forms() holds only the hand-listed forms, and plate is a generated one.
        forMaterials((id, m) -> {
            if (m.items(id).containsKey("plate") && m.thermal().conductivity().isPresent())
                out.accept(io.github.soundgoodizerfan.feedback.machine.skillet.SkilletItem.of(id));
        });
        forMaterials((id, m) -> {
            if (m.items(id).containsKey("plate") && m.thermal().conductivity().isPresent())
                out.accept(io.github.soundgoodizerfan.feedback.machine.stockpot.StockpotItem.of(id));
        });
        all(out, FOOD_ITEMS);
        // One fresh bottle per effect node on the brewing map, at its base length and first level.
        io.github.soundgoodizerfan.feedback.process.ClientTable.POTION_EFFECTS.get().forEach(e -> {
            ItemStack bottle = new ItemStack(FItems.POTION.get());
            bottle.set(FDataComponents.POTION_STATE.get(), new io.github.soundgoodizerfan.feedback.potion.PotionState(
                    java.util.Optional.of(e.effect()), 0, e.baseDuration(), 1f, "water", List.of(0f, 0f, 0f), 0L, 20f, -1));
            out.accept(bottle);
        });
        FCrops.SEEDS.forEach(seed -> out.accept(seed.get()));
        all(out, CROPS);
    }

    private static void fillMaterials(CreativeModeTab.Output out) {
        FormItems.all().forEach(generated -> out.accept(generated.item().get()));
        all(out, STOCK);
        all(out, ORES);
        all(out, CHEMICALS);
        out.accept(io.github.soundgoodizerfan.feedback.item.BloomItem.of(FItems.IRON_BLOOM.get(), 3));
        out.accept(io.github.soundgoodizerfan.feedback.item.BloomItem.of(FItems.IRON_BLOOM.get(), 1));
        all(out, INTERMEDIATES);
        out.accept(FItems.COPPER_BUCKET.get());
        // One filled bucket per source fluid: the bucket is one item, so each fluid's "form" is a component.
        FFluids.FLUIDS.getEntries().stream().map(h -> h.get())
                .filter(f -> f.defaultFluidState().isSource()).forEach(f -> {
                    ItemStack filled = new ItemStack(FItems.COPPER_BUCKET.get());
                    filled.set(FDataComponents.BUCKET_CONTENT.get(), SimpleFluidContent.copyOf(
                            new FluidStack(f, io.github.soundgoodizerfan.feedback.item.CopperBucketItem.CAPACITY_MB)));
                    out.accept(filled);
                });
    }

    public static void register(IEventBus modBus) {
        TABS.register(modBus);
        modBus.addListener(FCreativeTabs::buildOperatorTab);
    }

    /**
     * Development cheats belong with vanilla's own -- a command block and a barrier already
     * live here, and "obtainable only by someone who already knows it exists" is exactly what
     * this tab is for. None of these items is in a tab above; this is their only creative-menu home.
     */
    private static void buildOperatorTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.OP_BLOCKS) {
            event.accept(FItems.DEBUG_HELMET.get());
            event.accept(FModular.CREATIVE_TREATISE.get());
            event.accept(FModular.CREATIVE_TOOL_TABLE_ITEM.get());
            event.accept(FItems.CREATIVE_CONTROLLER.get());
            event.accept(FItems.DEBUG_SHELF.get());
            event.accept(FItems.CREATIVE_FLUID.get());
            event.accept(FItems.CREATIVE_AIR.get());
            event.accept(FItems.CREATIVE_HEAT.get());
            event.accept(FItems.HEAT_PROBE.get());
            event.accept(FItems.CREATIVE_ROTATION.get());
            event.accept(FItems.CREATIVE_ELECTRIC.get());
            event.accept(FItems.CREATIVE_XP.get());
            event.accept(FItems.MATERIAL_PROBE.get());
            event.accept(FItems.CAMERA_POLE.get());
            event.accept(FItems.BLOCK_CAMERA.get());
            // A sample circuit with free figures (many fast quiet elements, a good joint, traces that never lift): operator tab only.
            ItemStack sample = new ItemStack(FItems.CIRCUIT.get());
            sample.set(FDataComponents.CIRCUIT.get(), new io.github.soundgoodizerfan.feedback.process.Circuit(16, 1f,
                    io.github.soundgoodizerfan.feedback.process.ProcessTuning.CIRCUIT_NOISE_FLOOR,
                    io.github.soundgoodizerfan.feedback.process.Board.Joint.GOOD, 10000f, 1000f, 0f, 0f, 0f, 0f,
                    io.github.soundgoodizerfan.feedback.process.Circuit.clockRateOf(1f),
                    io.github.soundgoodizerfan.feedback.process.Circuit.Line.NONE));
            event.accept(sample);
        }
    }
}
