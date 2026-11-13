// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.registry;

import io.github.soundgoodizerfan.feedback.weapon.GunTuning;
import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class FCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Feedback.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.feedback"))
                    .icon(() -> new ItemStack(FItems.SHAFT.get()))
                    .displayItems((params, output) -> {
                        output.accept(FItems.TONGS.get());
                        // A plain Gun: the core gives it the gun type's starter design (a 9 mm steel handgun) on first tick.
                        output.accept(new ItemStack(FWeapons.GUN.get()));
                        output.accept(FWeapons.RELOADING_PRESS_ITEM.get());
                        output.accept(FWeapons.MIXER_ITEM.get());
                        output.accept(FWeapons.DEMOLITION_CHARGE_ITEM.get());
                        FModular.items().forEach(output::accept);
                        FLogistics.fill(output);
                        FAutomation.fill(output);
                        output.accept(FItems.BULLET_MOLD.get());
                        output.accept(FItems.BALL_MOLD.get());
                        output.accept(FItems.TUBE_MOLD.get());
                        output.accept(FItems.GLASS_BLOCK_MOLD.get());
                        output.accept(FItems.GLASS_PANE_MOLD.get());
                        output.accept(FItems.GLASS_BOTTLE_MOLD.get());
                        output.accept(FItems.PICKAXE_HEAD_MOLD.get());
                        output.accept(FItems.AXE_HEAD_MOLD.get());
                        output.accept(FItems.SHOVEL_HEAD_MOLD.get());
                        output.accept(FItems.HOE_HEAD_MOLD.get());
                        output.accept(FItems.SICKLE_HEAD_MOLD.get());
                        output.accept(FItems.SAW_BLADE_MOLD.get());
                        output.accept(FItems.ATOMISING_MOLD.get());
                        output.accept(new ItemStack(FWeapons.MAGAZINE_ITEM.get()));
                        ItemStack loaded = new ItemStack(FWeapons.MAGAZINE_ITEM.get());
                        loaded.set(FWeapons.MAGAZINE.get(), new io.github.soundgoodizerfan.feedback.weapon.Magazine(
                                java.util.Collections.nCopies(io.github.soundgoodizerfan.feedback.weapon.GunTuning.GUN_MAGAZINE_STANDARD,
                                        io.github.soundgoodizerfan.feedback.weapon.Cartridge.reference(9))));
                        output.accept(loaded);
                        ItemStack round = new ItemStack(FWeapons.CARTRIDGE_ITEM.get());
                        round.set(FWeapons.CARTRIDGE.get(), io.github.soundgoodizerfan.feedback.weapon.Cartridge.reference(9));
                        output.accept(round);
                        ItemStack shell = new ItemStack(FWeapons.CARTRIDGE_ITEM.get());
                        shell.set(FWeapons.CARTRIDGE.get(), io.github.soundgoodizerfan.feedback.weapon.Cartridge.referenceShot(18));
                        output.accept(shell);
                        output.accept(FVehicles.LIFT_ITEM.get());
                        output.accept(FVehicles.GAS_CELL_CLOTH_ITEM.get());
                        output.accept(FVehicles.GAS_CELL_RUBBER_ITEM.get());
                        output.accept(FVehicles.GAS_CELL_PLATE_ITEM.get());
                        output.accept(FVehicles.VENT_VALVE_ITEM.get());
                        output.accept(FVehicles.BALLAST_TANK_ITEM.get());
                        output.accept(FVehicles.FLIGHT_SENSOR_ITEM.get());
                        output.accept(FVehicles.MICRO_CORE.get());
                        output.accept(FVehicles.SEAT_ITEM.get());
                        output.accept(FItems.SHAFT.get());
                        output.accept(FItems.CAMPFIRE.get());
                        output.accept(FItems.SOUL_CAMPFIRE.get());
                        output.accept(FItems.HOPPER.get());
                        output.accept(FItems.DROPPER.get());
                        output.accept(FItems.NOTE_BLOCK.get());
                        FItems.NOISEMAKERS.values().forEach(m -> output.accept(m.get()));
                        output.accept(FItems.DISPENSER.get());
                        output.accept(FItems.BEARING.get());
                        output.accept(FItems.SMALL_COG.get());
                        output.accept(FItems.LARGE_COG.get());
                        output.accept(FItems.GEARBOX.get());
                        output.accept(FItems.DIFFERENTIAL.get());
                        output.accept(FItems.ROTATION_ADAPTER.get());
                        output.accept(FItems.ROTARY_BEARING.get());
                        output.accept(FItems.RING_GEAR.get());
                        output.accept(FItems.LARGE_RING_GEAR.get());
                        output.accept(FItems.MICRO_WEDGE.get());
                        output.accept(FItems.TIRE_RUBBER.get());
                        output.accept(FItems.BALL_JOINT.get());
                        output.accept(FItems.LOCKED_JOINT.get());
                        output.accept(FItems.SLIDER_JOINT.get());
                        output.accept(FItems.PISTON_JOINT.get());
                        output.accept(FItems.CRANK_LINKAGE.get());
                        output.accept(FItems.MECHANICAL_HAMMER.get());
                        output.accept(FItems.WIRE_DRAWER.get());
                        output.accept(FItems.ROLLING_MILL.get());
                        output.accept(FItems.MECHANICAL_PRESS.get());
                        output.accept(FItems.DRILL_PRESS.get());
                        output.accept(FItems.LATHE.get());
                        output.accept(FItems.LATHE_TOOL_BIT.get());
                        output.accept(FItems.GRINDING_WHEEL.get());
                        output.accept(FItems.DIAMOND_GRINDING_WHEEL.get());
                        output.accept(FItems.GROUND_HARDENED_STEEL.get());
                        output.accept(FItems.SMALL_PULLEY.get());
                        output.accept(FItems.LARGE_PULLEY.get());
                        output.accept(FItems.BELT.get());
                        output.accept(FItems.JOINT_LINK.get());
                        output.accept(FItems.CLUTCH.get());
                        output.accept(FItems.OVERSPEED_TRIP.get());
                        output.accept(FItems.TIMER.get());
                        output.accept(FItems.CONTROLLER.get());
                        output.accept(FItems.PROGRAMMER.get());
                        output.accept(FItems.ELECTRIC_CONTROLLER.get());
                        output.accept(FItems.ELECTRIC_PROGRAMMER.get());
                        output.accept(FItems.COMPUTER.get());
                        output.accept(FItems.PUNCH_CARD.get());
                        output.accept(FItems.CIRCUIT_BOARD.get());
                        output.accept(FItems.FLOPPY_DISK.get());
                        output.accept(FItems.USB_DRIVE.get());
                        for (String app : io.github.soundgoodizerfan.feedback.control.computer.ComputerApps.DISK_APPS)
                            output.accept(io.github.soundgoodizerfan.feedback.control.computer.ComputerApps.disk(app));
                        output.accept(FItems.FUNCTION_CARD.get());
                        output.accept(FItems.MEMORY_DRUM.get());
                        output.accept(FItems.CARD_RACK.get());
                        output.accept(FItems.ESCAPEMENT.get());
                        output.accept(FItems.CALIPERS.get());
                        output.accept(FModular.DRAW_GAUGE.get());
                        output.accept(FItems.INSTRUMENT_BAG.get());
                        output.accept(FItems.WRENCH.get());
                        output.accept(FItems.TASTING_SPOON.get());
                        output.accept(FItems.GRAPPLE.get());
                        output.accept(FItems.GRAPPLE_WINCH.get());
                        net.minecraft.world.item.ItemStack feedbacker = new net.minecraft.world.item.ItemStack(FItems.FEEDBACKER.get());
                        feedbacker.set(FDataComponents.GEAR.get(), new io.github.soundgoodizerfan.feedback.combat.gear.GearStats(
                                io.github.soundgoodizerfan.feedback.Feedback.id("tempered_steel"), io.github.soundgoodizerfan.feedback.Feedback.id("leather"),
                                1.43f, 0.29f, 0f, 0f, 0f, 0f));
                        feedbacker.set(FDataComponents.GRANTS.get(), java.util.List.of(
                                io.github.soundgoodizerfan.feedback.combat.parry.ParryTuning.PUNCH_PROJECTILES));
                        output.accept(feedbacker);
                        output.accept(FItems.UNLEARNING_TOKEN.get());
                        output.accept(FItems.FIREBOX.get());
                        output.accept(FItems.HEARTH.get());
                        io.github.soundgoodizerfan.feedback.registry.FormItems.materials().forEach((id, material) -> {
                            if (material.forms().containsKey("plate") && material.thermal().conductivity().isPresent())
                                output.accept(io.github.soundgoodizerfan.feedback.machine.skillet.SkilletItem.of(id));
                        });
                        io.github.soundgoodizerfan.feedback.registry.FormItems.materials().forEach((id, material) -> {
                            if (material.forms().containsKey("plate") && material.thermal().conductivity().isPresent())
                                output.accept(io.github.soundgoodizerfan.feedback.machine.stockpot.StockpotItem.of(id));
                        });
                        output.accept(FItems.BREWING_STAND.get());
                        output.accept(FItems.ICE_BOX.get());
                        output.accept(FItems.REFRIGERATION_UNIT.get());
                        output.accept(FItems.CRYOCOOLER.get());
                        output.accept(FItems.DRYING_RACK.get());
                        output.accept(FItems.SALT.get());
                        output.accept(FItems.ALUMINA.get());
                        output.accept(FItems.SILICATE_SLAG.get());
                        output.accept(FItems.RED_MUD.get());
                        output.accept(FItems.RARE_EARTH_RESIDUE.get());
                        output.accept(FItems.FERTILISER.get());
                        output.accept(FItems.COPPER_BUCKET.get());
                        // One filled bucket per source fluid: the bucket is one item, so each fluid's "form" is a component.
                        FFluids.FLUIDS.getEntries().stream().map(h -> h.get())
                                .filter(f -> f.defaultFluidState().isSource()).forEach(f -> {
                                    ItemStack filled = new ItemStack(FItems.COPPER_BUCKET.get());
                                    filled.set(FDataComponents.BUCKET_CONTENT.get(), net.neoforged.neoforge.fluids.SimpleFluidContent.copyOf(
                                            new net.neoforged.neoforge.fluids.FluidStack(f,
                                                    io.github.soundgoodizerfan.feedback.item.CopperBucketItem.CAPACITY_MB)));
                                    output.accept(filled);
                                });
                        FCrops.SEEDS.forEach(seed -> output.accept(seed.get()));
                        output.accept(FItems.DOUGH.get());
                        output.accept(FItems.LOAF.get());
                        output.accept(FItems.FLATBREAD_DOUGH.get());
                        output.accept(FItems.FLATBREAD.get());
                        output.accept(FItems.PASTRY_DOUGH.get());
                        output.accept(FItems.PASTRY_SHELL.get());
                        output.accept(FItems.ROASTED_PUMPKIN.get());
                        output.accept(FItems.NOODLES.get());
                        output.accept(FItems.COOKED_NOODLES.get());
                        output.accept(FItems.FRIED_EGG.get());
                        output.accept(FItems.TURNIP.get());
                        output.accept(FItems.TOMATO.get());
                        output.accept(FItems.ONION.get());
                        output.accept(FItems.GARLIC.get());
                        output.accept(FItems.CABBAGE.get());
                        output.accept(FItems.CORN.get());
                        output.accept(FItems.RICE.get());
                        output.accept(FItems.STEAMED_RICE.get());
                        output.accept(FItems.SOYBEAN.get());
                        output.accept(FItems.CHILE_PEPPER.get());
                        output.accept(FItems.GINGER.get());
                        output.accept(FItems.PEPPER.get());
                        output.accept(FItems.JAR.get());
                        output.accept(FItems.SMALL_CRUCIBLE.get());
                        output.accept(FItems.LARGE_CRUCIBLE.get());
                        output.accept(FItems.ARC_HEARTH.get());
                        output.accept(FItems.ARC_HEAD.get());
                        // One electrode per material that can carry an arc.
                        io.github.soundgoodizerfan.feedback.registry.FormItems.materials().forEach((id, material) -> {
                            if (material.electrodeLimit().isPresent())
                                output.accept(io.github.soundgoodizerfan.feedback.machine.arc.ElectrodeItem.of(id));
                        });
                        output.accept(FItems.POWER_CUTTER.get());
                        output.accept(FItems.AUTO_ENGINEER.get());
                        output.accept(FItems.DATA_STICK.get());
                        // One cutter head of each kind per material hard enough to be one.
                        io.github.soundgoodizerfan.feedback.registry.FormItems.materials().forEach((id, material) -> {
                            boolean hard = material.kind().filter(k -> k == io.github.soundgoodizerfan.feedback.process.Material.Kind.METAL
                                    || k == io.github.soundgoodizerfan.feedback.process.Material.Kind.CERAMIC
                                    || k == io.github.soundgoodizerfan.feedback.process.Material.Kind.CRYSTAL).isPresent();
                            if (!hard)
                                return;
                            net.minecraft.world.item.ItemStack saw = new net.minecraft.world.item.ItemStack(FItems.SAW_HEAD.get());
                            saw.set(FDataComponents.MADE_OF.get(), new io.github.soundgoodizerfan.feedback.process.MadeOf(id, "ingot", 1));
                            output.accept(saw);
                            if (material.ampacity().isPresent()) {
                                net.minecraft.world.item.ItemStack plasma = new net.minecraft.world.item.ItemStack(FItems.PLASMA_HEAD.get());
                                plasma.set(FDataComponents.MADE_OF.get(), new io.github.soundgoodizerfan.feedback.process.MadeOf(id, "ingot", 1));
                                output.accept(plasma);
                            }
                        });
                        output.accept(FItems.INSULATION.get());
                        output.accept(FItems.CRYSTAL_HOLDER.get());
                        output.accept(FItems.CULTURE_DISH.get());
                        output.accept(FItems.CATALYST_TAP.get());
                        output.accept(FItems.ORB_ABSORBER.get());
                        output.accept(FItems.ORE_TAP.get());
                        output.accept(FItems.TRADE_TAP.get());
                        output.accept(FItems.SMELTER_TAP.get());
                        output.accept(FItems.DISENCHANT_STONE.get());
                        output.accept(FItems.SCULK_RELAY.get());
                        output.accept(FItems.DATA_SENSOR.get());
                        output.accept(FItems.CULTURE_SEED.get());
                        output.accept(FItems.CREEPER_PEN.get());
                        output.accept(FItems.CREEPER_SAC.get());
                        output.accept(FItems.CREEPER_CHARGE.get());
                        output.accept(FItems.RELAY_ORGAN.get());
                        io.github.soundgoodizerfan.feedback.registry.FormItems.materials().forEach((id, material) -> {
                            if (material.xpStore().filter(x -> x.conductance() > 0).isPresent())
                                output.accept(io.github.soundgoodizerfan.feedback.machine.xp.XpWireItem.of(id));
                        });
                        output.accept(FItems.DAMPER.get());
                        output.accept(FItems.VALVE.get());
                        output.accept(FItems.REDSTONE_EMITTER.get());
                        output.accept(FItems.ANALOG_ADAPTER.get());
                        output.accept(FItems.BELLOWS.get());
                        // One cable per conductor: every jar material that states an ampacity.
                        io.github.soundgoodizerfan.feedback.registry.FormItems.materials().forEach((id, material) -> {
                            if (material.ampacity().isPresent()) {
                                output.accept(io.github.soundgoodizerfan.feedback.machine.cable.CableItem.of(id, 1));
                                for (io.github.soundgoodizerfan.feedback.core.unit.VoltageBand band : io.github.soundgoodizerfan.feedback.core.unit.VoltageBand.values())
                                    output.accept(io.github.soundgoodizerfan.feedback.machine.cable.CableItem.of(id, 1, band));
                                // The other gauges: bare, and coated for the lowest band.
                                for (io.github.soundgoodizerfan.feedback.machine.cable.CableGauge gauge : io.github.soundgoodizerfan.feedback.machine.cable.CableGauge.values()) {
                                    if (gauge == io.github.soundgoodizerfan.feedback.machine.cable.CableGauge.STANDARD)
                                        continue;
                                    output.accept(io.github.soundgoodizerfan.feedback.machine.cable.CableItem.of(id, 1, null, gauge));
                                    output.accept(io.github.soundgoodizerfan.feedback.machine.cable.CableItem.of(id, 1,
                                            io.github.soundgoodizerfan.feedback.core.unit.VoltageBand.LV, gauge));
                                }
                                ItemStack post = new ItemStack(FItems.FENCE.get());
                                post.set(FDataComponents.MADE_OF.get(), new io.github.soundgoodizerfan.feedback.process.MadeOf(id, "wire", 1));
                                output.accept(post);
                            }
                        });
                        output.accept(FItems.GALVANIC_CELL_PACK.get());
                        output.accept(FItems.ELECTROSTATIC_GENERATOR.get());
                        output.accept(FItems.COPPER_COIL.get());
                        output.accept(FItems.IRON_COIL.get());
                        output.accept(FItems.INDUCTION_COIL.get());
                        output.accept(FItems.INDUCTION_STOVE.get());
                        output.accept(FItems.PREHEAT_COIL.get());
                        output.accept(FItems.BREAKER.get());
                        for (io.github.soundgoodizerfan.feedback.machine.cable.CableGauge gauge : io.github.soundgoodizerfan.feedback.machine.cable.CableGauge.values()) {
                            if (gauge == io.github.soundgoodizerfan.feedback.machine.cable.CableGauge.STANDARD)
                                continue;
                            ItemStack breaker = new ItemStack(FItems.BREAKER.get());
                            breaker.set(FDataComponents.CABLE_GAUGE.get(), gauge);
                            output.accept(breaker);
                        }
                        output.accept(FItems.FUSE.get());
                        output.accept(FItems.COHERER.get());
                        output.accept(FItems.REFLOW_OVEN.get());
                        output.accept(FItems.PHOSPHOR_SCREEN.get());
                        output.accept(FItems.OSCILLOSCOPE.get());
                        output.accept(FItems.TRACE_BOARD.get());
                        output.accept(FItems.SOLDERING_IRON.get());
                        output.accept(FItems.ARC_WELDER.get());
                        output.accept(FItems.WHISKER.get());
                        output.accept(FItems.RELAY.get());
                        output.accept(FItems.GALENA.get());
                        output.accept(FItems.PYRITE.get());
                        output.accept(FItems.HYDROMETER.get());
                        output.accept(FItems.JAR_BATON.get());
                        output.accept(FItems.ELECTROLYTIC_CELL.get());
                        output.accept(FItems.ELECTROLYSER.get());
                        output.accept(FItems.CHARGER.get());
                        output.accept(FItems.CHARGING_GANTRY.get());
                        output.accept(FItems.RECTIFIER.get());
                        output.accept(FItems.CONVERTER.get());
                        output.accept(FItems.CONVERTER_TAP.get());
                        output.accept(FItems.HEATSINK.get());
                        output.accept(FItems.BIMETALLIC_STRIP.get());
                        output.accept(FItems.STEAM_ENGINE.get());
                        output.accept(FItems.STEAM_TURBINE.get());
                        output.accept(FItems.CONDENSER.get());
                        output.accept(FItems.HUSK_SAND.get());
                        output.accept(FItems.VACUUM_DRYER.get());
                        output.accept(FItems.TUBE.get());
                        output.accept(FItems.LINED_TUBE.get());
                        output.accept(FItems.WIDE_TUBE.get());
                        output.accept(FItems.EVACUATED_TUBE.get());
                        output.accept(FItems.TUBE_JUNCTION.get());
                        output.accept(FItems.PROGRAMMED_JUNCTION.get());
                        output.accept(FItems.TUBE_STATION.get());
                        output.accept(FItems.SORTING_STATION.get());
                        output.accept(FItems.SORT_GAUGE.get());
                        output.accept(FItems.VACUUM_PUMP.get());
                        output.accept(FItems.LINEAR_MOTOR.get());
                        output.accept(FItems.CARRIER.get());
                        output.accept(FItems.MAGNETIC_CARRIER.get());
                        output.accept(FItems.POD.get());
                        output.accept(FItems.ENDER_PORT.get());
                        output.accept(FItems.ENDER_ANCHOR.get());
                        output.accept(FItems.GAS_TURBINE.get());
                        output.accept(FItems.FLARE.get());
                        output.accept(FItems.REACTOR.get());
                        output.accept(FItems.REBOILER.get());
                        output.accept(FItems.TRAY_BUBBLE_CAP.get());
                        output.accept(FItems.TRAY_SIEVE.get());
                        output.accept(FItems.TRAY_PACKED.get());
                        output.accept(FItems.DERRICK_BASE.get());
                        output.accept(FItems.DERRICK_CASING.get());
                        output.accept(FItems.DERRICK_BIT.get());
                        output.accept(FItems.WELLHEAD.get());
                        output.accept(FItems.SEEP.get());
                        output.accept(FItems.SOUL_SEEP.get());
                        output.accept(FItems.REACTOR_CORE.get());
                        output.accept(FItems.FUEL_CHANNEL.get());
                        output.accept(FItems.CONTROL_ASSEMBLY.get());
                        output.accept(FItems.SCRAM_TRIP.get());
                        output.accept(FItems.SPENT_FUEL_POOL.get());
                        output.accept(FItems.COOLANT_PORT.get());
                        output.accept(FItems.COOLANT_CHANNEL.get());
                        output.accept(FItems.NEUTRON_REFLECTOR.get());
                        output.accept(FItems.STAR_SEAT.get());
                        output.accept(FItems.FLUX_METER.get());
                        output.accept(FItems.GRAPHITE_MODERATOR.get());
                        output.accept(FItems.SHOCK_CAPSULE.get());
                        output.accept(FItems.SHEAVE.get());
                        output.accept(FItems.PURIFIED_REDSTONE.get());
                        output.accept(FItems.SUPERCONDUCTOR_BLEND.get());
                        output.accept(FItems.CALCINED_BLEND.get());
                        output.accept(FItems.SHEATHED_BILLET.get());
                        output.accept(FItems.IMPACT_PENDULUM.get());
                        output.accept(FItems.STRIKE_POST.get());
                        output.accept(FItems.RESPIRATOR.get());
                        output.accept(FItems.DIAMOND_GRIT.get());
                        output.accept(FItems.DIAMOND_SEED.get());
                        output.accept(FItems.FUEL_ROD.get());
                        output.accept(FItems.THORIUM_FUEL_ROD.get());
                        output.accept(FItems.ENRICHED_FUEL_ROD.get());
                        output.accept(FItems.BREEDER_ROD.get());
                        output.accept(FItems.SPENT_FUEL_ROD.get());
                        output.accept(FItems.GAS_CENTRIFUGE.get());
                        output.accept(FItems.CORE_SLAG.get());
                        output.accept(FItems.MAGMA_HEAD.get());
                        output.accept(FItems.BASTION_RUBBLE.get());
                        output.accept(FItems.CASTING_BASIN.get());
                        output.accept(FItems.THERMOMETER.get());
                        output.accept(FItems.PYROMETER.get());
                        output.accept(FItems.PRESSURE_GAUGE.get());
                        output.accept(FItems.XP_METER.get());
                        output.accept(FItems.STEEL_INGOT.get());
                        output.accept(FItems.BURNT_IRON.get());
                        output.accept(io.github.soundgoodizerfan.feedback.item.BloomItem.of(FItems.IRON_BLOOM.get(), 3));
                        output.accept(io.github.soundgoodizerfan.feedback.item.BloomItem.of(FItems.IRON_BLOOM.get(), 1));
                        output.accept(FItems.BLOOMERY_SLAG.get());
                        output.accept(FItems.SCRAP_METAL.get());
                        output.accept(FItems.CHARRED_REMAINS.get());
                        output.accept(FItems.CRACKED_CERAMIC.get());
                        output.accept(FItems.POROUS_SINTER.get());
                        output.accept(FItems.CERMET_POWDER.get());
                        output.accept(FItems.CERMET_GREEN_COMPACT.get());
                        output.accept(FItems.TUNGSTEN_CARBIDE_POWDER.get());
                        output.accept(FItems.TUNGSTEN_CARBIDE_GREEN_COMPACT.get());
                        output.accept(FItems.POROUS_AGGREGATE.get());
                        output.accept(FItems.SYNTHETIC_PEARL.get());
                        output.accept(FItems.TUNED_EYE.get());
                        output.accept(FItems.RARE_EARTH_MAGNET_POWDER.get());
                        output.accept(FItems.RARE_EARTH_MAGNET_GREEN_COMPACT.get());
                        output.accept(FItems.RARE_EARTH_MAGNET_BLANK.get());
                        output.accept(FItems.OSMIUM_SPONGE.get());
                        output.accept(FItems.OSMIUM_POWDER.get());
                        output.accept(FItems.OSMIUM_GREEN_COMPACT.get());
                        output.accept(FItems.HARDENED_STEEL.get());
                        output.accept(FItems.CAST_NETHERITE_INGOT.get());
                        output.accept(FItems.NETHERITE_POWDER.get());
                        output.accept(FItems.NETHERITE_GREEN_COMPACT.get());
                        output.accept(FItems.WORN_NETHERITE_BITS.get());
                        output.accept(FItems.GHAST_BLADDER.get());
                        FormItems.all().forEach(generated -> output.accept(generated.item().get()));
                        output.accept(FItems.TEMPERED_STEEL.get());
                        output.accept(FItems.CAST_IRON.get());
                        output.accept(FItems.TIN_INGOT.get());
                        output.accept(FItems.ZINC_INGOT.get());
                        output.accept(FItems.BRONZE_INGOT.get());
                        output.accept(FItems.BRASS_INGOT.get());
                        output.accept(FItems.GLASS_INGOT.get());
                        output.accept(FItems.CERAMIC.get());
                        output.accept(FItems.GLAZED_CERAMIC.get());
                        output.accept(FItems.KAOLINITE.get());
                        output.accept(FItems.FIREBRICK.get());
                        output.accept(FItems.GRAPHITE.get());
                        output.accept(FItems.GRAPHITE_ORE.get());
                        output.accept(FItems.DEEPSLATE_GRAPHITE_ORE.get());
                        output.accept(FItems.SALTPETRE.get());
                        output.accept(FItems.SALTPETRE_ORE.get());
                        output.accept(FItems.DEEPSLATE_SALTPETRE_ORE.get());
                        output.accept(FItems.SULFUR.get());
                        output.accept(FItems.SULFUR_ORE.get());
                        output.accept(FItems.COAL_ORE.get());
                        output.accept(FItems.DEEPSLATE_COAL_ORE.get());
                        output.accept(FItems.IRON_ORE.get());
                        output.accept(FItems.DEEPSLATE_IRON_ORE.get());
                        output.accept(FItems.COPPER_ORE.get());
                        output.accept(FItems.DEEPSLATE_COPPER_ORE.get());
                        output.accept(FItems.GOLD_ORE.get());
                        output.accept(FItems.DEEPSLATE_GOLD_ORE.get());
                        output.accept(FItems.LAPIS_ORE.get());
                        output.accept(FItems.DEEPSLATE_LAPIS_ORE.get());
                        output.accept(FItems.REDSTONE_ORE.get());
                        output.accept(FItems.DEEPSLATE_REDSTONE_ORE.get());
                        output.accept(FItems.DIAMOND_ORE.get());
                        output.accept(FItems.DEEPSLATE_DIAMOND_ORE.get());
                        output.accept(FItems.EMERALD_ORE.get());
                        output.accept(FItems.DEEPSLATE_EMERALD_ORE.get());
                        output.accept(FItems.NETHER_GOLD_ORE.get());
                        output.accept(FItems.NETHER_QUARTZ_ORE.get());
                        output.accept(FItems.TIN_ORE.get());
                        output.accept(FItems.DEEPSLATE_TIN_ORE.get());
                        output.accept(FItems.RAW_TIN.get());
                        output.accept(FItems.ZINC_ORE.get());
                        output.accept(FItems.DEEPSLATE_ZINC_ORE.get());
                        output.accept(FItems.RAW_ZINC.get());
                        output.accept(FItems.LEAD_ORE.get());
                        output.accept(FItems.DEEPSLATE_LEAD_ORE.get());
                        output.accept(FItems.GALENA_ORE.get());
                        output.accept(FItems.PYRITE_ORE.get());
                        output.accept(FItems.KAOLINITE_DEPOSIT.get());
                        output.accept(FItems.SALT_DEPOSIT.get());
                        output.accept(FItems.COPPER_FLOWER.get());
                        output.accept(FItems.ZINC_VIOLET.get());
                        output.accept(FItems.RUSTY_SOIL.get());
                        output.accept(FItems.BLEACHED_SOIL.get());
                        output.accept(FItems.SOOTY_SOIL.get());
                        output.accept(FItems.BLADDER_CAMPION.get());
                        output.accept(FItems.BLACK_SAND.get());
                        output.accept(FItems.GOSSAN_RUBBLE.get());
                        output.accept(FItems.TARNISHED_SOIL.get());
                        output.accept(FItems.LEAD_CHEST.get());
                        output.accept(FItems.RAW_LEAD.get());
                        output.accept(FItems.LEAD_INGOT.get());
                        output.accept(FItems.SILVER_INGOT.get());
                        output.accept(FItems.COPPER_TUBING.get());
                        output.accept(FItems.GLASS_TUBING.get());
                        output.accept(FItems.QUARTZ_GLASS_INGOT.get());
                        output.accept(FItems.FURNACE.get());
                        output.accept(FItems.SMOKER.get());
                        output.accept(FItems.BLAST_FURNACE.get());
                        output.accept(FItems.IMPROVED_FURNACE.get());
                        output.accept(FItems.KILN.get());
                        output.accept(FItems.ANNEALING_FURNACE.get());
                        output.accept(FItems.PUDDLING_FURNACE.get());
                        output.accept(FItems.INGOT_MOLD.get());
                        output.accept(FItems.TEMPERATURE_SENSOR.get());
                        output.accept(FItems.GALVANOMETER.get());
                        output.accept(FItems.COMPONENT_TESTER.get());
                        for (io.github.soundgoodizerfan.feedback.process.Cut cut : io.github.soundgoodizerfan.feedback.process.Cut.values()) {
                            net.minecraft.world.item.ItemStack probe = new net.minecraft.world.item.ItemStack(FItems.MASS_PROBE.get());
                            probe.set(FDataComponents.CUT.get(), cut);
                            output.accept(probe);
                        }
                        output.accept(FItems.ELECTROSCOPE.get());
                        output.accept(FItems.CURRENT_SENSOR.get());
                        output.accept(FItems.REFRACTORY_LINING.get());
                        output.accept(FItems.RELIEF_VALVE.get());
                        output.accept(FItems.AIR_INTAKE.get());
                        output.accept(FItems.CROWBAR.get());
                        output.accept(FItems.MORTAR.get());
                        output.accept(FItems.DATA_CONNECTOR.get());
                        // CREATIVE_CONTROLLER stays out, same as DEBUG_HELMET -- see buildOperatorTab below.
                    })
                    .build());

    private FCreativeTabs() {
    }

    public static void register(IEventBus modBus) {
        TABS.register(modBus);
        modBus.addListener(FCreativeTabs::buildOperatorTab);
    }

    /**
     * Development cheats belong with vanilla's own -- a command block and a barrier already
     * live here, and "obtainable only by someone who already knows it exists" is exactly what
     * this tab is for. Neither item is in {@link #MAIN}; this is their only creative-menu home.
     */
    private static void buildOperatorTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.OP_BLOCKS) {
            event.accept(FItems.DEBUG_HELMET.get());
            event.accept(FModular.CREATIVE_TREATISE.get());
            event.accept(FItems.CREATIVE_CONTROLLER.get());
            event.accept(FItems.CREATIVE_FLUID.get());
            event.accept(FItems.CREATIVE_AIR.get());
            event.accept(FItems.CREATIVE_HEAT.get());
            event.accept(FItems.HEAT_PROBE.get());
            event.accept(FItems.CREATIVE_ROTATION.get());
            event.accept(FItems.CREATIVE_ELECTRIC.get());
            event.accept(FItems.CREATIVE_XP.get());
            event.accept(FItems.MATERIAL_PROBE.get());
        }
    }
}
