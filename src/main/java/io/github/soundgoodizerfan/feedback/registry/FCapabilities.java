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

import java.util.List;

import io.github.soundgoodizerfan.feedback.machine.boiler.BoilerFluidHandler;
import io.github.soundgoodizerfan.feedback.machine.crucible.CrucibleItemHandler;
import io.github.soundgoodizerfan.feedback.machine.WorkpieceItemHandler;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * What the mod's blocks expose to other blocks. One place to look, like the registries beside it.
 * <p>
 * Not a {@code DeferredRegister} -- capabilities are attached on an event rather than registered
 * -- but the reason for gathering them here is the same. A machine that can be piped into is a
 * fact about the mod, not a detail of one block entity.
 */
public class FCapabilities {

    private FCapabilities() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(FCapabilities::registerCapabilities);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        // Every side, no filtering. A hammer has one anvil and it faces the room; which side a
        // hopper approaches from is not a fact the machine has any way to care about.
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, FBlockEntities.MECHANICAL_HAMMER.get(),
                (hammer, side) -> new WorkpieceItemHandler(hammer));
        // The machine shop, for the same reason: a hopper is a second machine racing the first.
        for (var type : List.of(FBlockEntities.MECHANICAL_PRESS, FBlockEntities.ROLLING_MILL,
                FBlockEntities.WIRE_DRAWER, FBlockEntities.LATHE, FBlockEntities.DRILL_PRESS,
                FBlockEntities.GRINDING_WHEEL))
            event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, type.get(),
                    (machine, side) -> new WorkpieceItemHandler(machine));

        // Same argument, and the same deliberate hole in it: an extractor takes whatever is in
        // the vessel, finished or not. A hopper under a crucible is a second machine racing the
        // first rather than a sensor that knows when the steel is ready.
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, FBlockEntities.CRUCIBLE.get(),
                (crucible, side) -> new CrucibleItemHandler(crucible));

        // Both sides of the thermal-to-rotation bridge, so a bucket, a hopper, or a future pipe
        // already works against either block without either of them knowing the other exists --
        // see FTuning's "--- the boiler ---" section.
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, FBlockEntities.BOILER.get(),
                (boiler, side) -> new BoilerFluidHandler(boiler));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, FBlockEntities.STEAM_ENGINE.get(),
                (engine, side) -> engine.getInput());
        // And the way back: an engine's exhaust into a condenser, its water out to a pump.
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, FBlockEntities.CONDENSER.get(),
                (condenser, side) -> condenser.getFluidHandler());
        // A valve on a fluid line: see ValveBlockEntity -- a cutoff holding nothing, one way.
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, FBlockEntities.VALVE.get(),
                (valve, side) -> valve.getFluidLine(side));
        // The electrolytic cell's water, by bucket or pipe; draining it takes dissolved metal along.
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, FBlockEntities.ELECTROLYTIC_CELL.get(),
                (cell, side) -> cell.getFluidHandler());
    }
}
