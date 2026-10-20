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
                        output.accept(FItems.HAND_HAMMER.get());
                        output.accept(FItems.LEATHER_GLOVES.get());
                        output.accept(FItems.TONGS.get());
                        // A plain Gun carrying the starter design (9 mm, iron), baked on first tick.
                        ItemStack pistol = new ItemStack(FWeapons.GUN.get());
                        pistol.set(FWeapons.GUN_DESIGN.get(), io.github.soundgoodizerfan.feedback.weapon.GunDesign.starter(
                                io.github.soundgoodizerfan.feedback.Feedback.id("iron")));
                        output.accept(pistol);
                        output.accept(FWeapons.GUN_STAND_ITEM.get());
                        output.accept(FWeapons.RELOADING_PRESS_ITEM.get());
                        output.accept(FWeapons.ASSEMBLY_BENCH_ITEM.get());
                        FModular.items().forEach(output::accept);
                        output.accept(FItems.BULLET_MOLD.get());
                        output.accept(new ItemStack(FWeapons.MAGAZINE_ITEM.get()));
                        ItemStack loaded = new ItemStack(FWeapons.MAGAZINE_ITEM.get());
                        loaded.set(FWeapons.MAGAZINE.get(), new io.github.soundgoodizerfan.feedback.weapon.Magazine(
                                java.util.Collections.nCopies(io.github.soundgoodizerfan.feedback.core.FTuning.GUN_MAGAZINE_STANDARD,
                                        io.github.soundgoodizerfan.feedback.weapon.Cartridge.reference(9))));
                        output.accept(loaded);
                        ItemStack round = new ItemStack(FWeapons.CARTRIDGE_ITEM.get());
                        round.set(FWeapons.CARTRIDGE.get(), io.github.soundgoodizerfan.feedback.weapon.Cartridge.reference(9));
                        output.accept(round);
                        ItemStack shell = new ItemStack(FWeapons.CARTRIDGE_ITEM.get());
                        shell.set(FWeapons.CARTRIDGE.get(), io.github.soundgoodizerfan.feedback.weapon.Cartridge.referenceShot(18));
                        output.accept(shell);
                        ItemStack slug = new ItemStack(FWeapons.CARTRIDGE_ITEM.get());
                        slug.set(FWeapons.CARTRIDGE.get(), io.github.soundgoodizerfan.feedback.weapon.Cartridge.referenceCoil(9));
                        output.accept(slug);
                        ItemStack coilMag = new ItemStack(FWeapons.MAGAZINE_ITEM.get());
                        coilMag.set(FWeapons.MAGAZINE.get(), new io.github.soundgoodizerfan.feedback.weapon.Magazine(
                                java.util.Collections.nCopies(io.github.soundgoodizerfan.feedback.core.FTuning.GUN_MAGAZINE_STANDARD,
                                        io.github.soundgoodizerfan.feedback.weapon.Cartridge.referenceCoil(9))));
                        output.accept(coilMag);
                        output.accept(FVehicles.LIFT_ITEM.get());
                        output.accept(FVehicles.FRAME_ITEM.get());
                        output.accept(FVehicles.ENGINE_STAND_ITEM.get());
                        output.accept(FVehicles.WELDER.get());
                        output.accept(FVehicles.PIVOT_SETTER.get());
                        output.accept(FVehicles.SEAT_ITEM.get());
                        output.accept(FItems.SHAFT.get());
                        output.accept(FItems.BEARING.get());
                        output.accept(FItems.FLYWHEEL.get());
                        output.accept(FItems.SMALL_COG.get());
                        output.accept(FItems.LARGE_COG.get());
                        output.accept(FItems.GEARBOX.get());
                        output.accept(FItems.HAND_CRANK.get());
                        output.accept(FItems.WATER_WHEEL.get());
                        output.accept(FItems.CRANK_LINKAGE.get());
                        output.accept(FItems.MECHANICAL_HAMMER.get());
                        output.accept(FItems.WIRE_DRAWER.get());
                        output.accept(FItems.ROLLING_MILL.get());
                        output.accept(FItems.MECHANICAL_PRESS.get());
                        output.accept(FItems.COPPER_ROD.get());
                        output.accept(FItems.COPPER_WIRE.get());
                        output.accept(FItems.COPPER_FINE_WIRE.get());
                        output.accept(FItems.STEEL_ROD.get());
                        output.accept(FItems.STEEL_WIRE.get());
                        output.accept(FItems.STEEL_CABLE.get());
                        output.accept(FItems.DRILL_PRESS.get());
                        output.accept(FItems.STEEL_SWARF.get());
                        output.accept(FItems.DRILLED_STEEL_PLATE.get());
                        output.accept(FItems.LATHE.get());
                        output.accept(FItems.LATHE_TOOL_BIT.get());
                        output.accept(FItems.TURNED_STEEL_ROD.get());
                        output.accept(FItems.GRINDING_WHEEL.get());
                        output.accept(FItems.GROUND_HARDENED_STEEL.get());
                        output.accept(FItems.SMALL_PULLEY.get());
                        output.accept(FItems.LARGE_PULLEY.get());
                        output.accept(FItems.BELT.get());
                        output.accept(FItems.CLUTCH.get());
                        output.accept(FItems.OVERSPEED_TRIP.get());
                        output.accept(FItems.TIMER.get());
                        output.accept(FItems.CONTROLLER.get());
                        output.accept(FItems.PROGRAMMER.get());
                        output.accept(FItems.PUNCH_CARD.get());
                        output.accept(FItems.CALIPERS.get());
                        output.accept(FItems.INSTRUMENT_BAG.get());
                        output.accept(FItems.WRENCH.get());
                        output.accept(FItems.COPPER_PLATE.get());
                        output.accept(FItems.COPPER_FOIL.get());
                        output.accept(FItems.COPPER_SCRAP.get());
                        output.accept(FItems.FIREBOX.get());
                        output.accept(FItems.SMALL_CRUCIBLE.get());
                        output.accept(FItems.LARGE_CRUCIBLE.get());
                        output.accept(FItems.INSULATION.get());
                        output.accept(FItems.DAMPER.get());
                        output.accept(FItems.VALVE.get());
                        output.accept(FItems.BELLOWS.get());
                        output.accept(FItems.COPPER_CABLE.get());
                        output.accept(FItems.GALVANIC_CELL.get());
                        output.accept(FItems.ELECTROSTATIC_GENERATOR.get());
                        output.accept(FItems.ELECTROMAGNET.get());
                        output.accept(FItems.DYNAMO.get());
                        output.accept(FItems.MOTOR.get());
                        output.accept(FItems.COPPER_COIL.get());
                        output.accept(FItems.IRON_COIL.get());
                        output.accept(FItems.BREAKER.get());
                        output.accept(FItems.FUSE.get());
                        output.accept(FItems.LEYDEN_JAR.get());
                        output.accept(FItems.ELECTROLYTIC_CELL.get());
                        output.accept(FItems.ROUND_CHARGER.get());
                        output.accept(FItems.BIMETALLIC_STRIP.get());
                        output.accept(FItems.BOILER.get());
                        output.accept(FItems.STEAM_ENGINE.get());
                        output.accept(FItems.CONDENSER.get());
                        output.accept(FItems.PUMP.get());
                        output.accept(FItems.THERMOMETER.get());
                        output.accept(FItems.STEEL_INGOT.get());
                        output.accept(FItems.BURNT_IRON.get());
                        output.accept(FItems.HARDENED_STEEL.get());
                        FormItems.all().forEach(generated -> output.accept(generated.item().get()));
                        output.accept(FItems.TEMPERED_STEEL.get());
                        output.accept(FItems.STEEL_PLATE.get());
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
                        output.accept(FItems.LEAD_INGOT.get());
                        output.accept(FItems.COPPER_TUBING.get());
                        output.accept(FItems.QUARTZ_GLASS_INGOT.get());
                        output.accept(FItems.FURNACE.get());
                        output.accept(FItems.SMOKER.get());
                        output.accept(FItems.BLAST_FURNACE.get());
                        output.accept(FItems.IMPROVED_FURNACE.get());
                        output.accept(FItems.KILN.get());
                        output.accept(FItems.ANNEALING_FURNACE.get());
                        output.accept(FItems.HEAT_EXCHANGER.get());
                        output.accept(FItems.PRESSURE_VESSEL.get());
                        output.accept(FItems.INGOT_MOLD.get());
                        output.accept(FItems.TEMPERATURE_SENSOR.get());
                        output.accept(FItems.GALVANOMETER.get());
                        output.accept(FItems.ELECTROSCOPE.get());
                        output.accept(FItems.CURRENT_SENSOR.get());
                        output.accept(FItems.REFRACTORY_LINING.get());
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
            event.accept(FItems.CREATIVE_CONTROLLER.get());
            event.accept(FItems.CREATIVE_HEAT.get());
            event.accept(FItems.CREATIVE_ROTATION.get());
            event.accept(FItems.CREATIVE_ELECTRIC.get());
            event.accept(FItems.MATERIAL_PROBE.get());
        }
    }
}
