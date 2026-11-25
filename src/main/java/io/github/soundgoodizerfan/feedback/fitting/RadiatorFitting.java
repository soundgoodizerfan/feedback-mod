// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.fitting;

import io.github.soundgoodizerfan.feedback.registry.FItems;

import net.minecraft.world.item.ItemStack;

/**
 * A radiator on one face of an engine: bronze header tanks round copper fins and coolant tubes, giving the engine a conductance to the air
 * ({@link io.github.soundgoodizerfan.feedback.vehicle.VehicleTuning#RADIATOR_CONDUCTANCE}) and the engine's coolant tank a port on that
 * face, which the pipe system reaches as any other fluid handler. With coolant in the tank the heat reaches the fins; dry it gives only a
 * share of its conductance. The engine does the heat; this only says it is there. Stateless: persisted as a flag per face
 * ({@link FittingStore}).
 */
public final class RadiatorFitting implements SidedFitting {

    @Override
    public ItemStack getPickItem() {
        return new ItemStack(FItems.RADIATOR.get());
    }
}
