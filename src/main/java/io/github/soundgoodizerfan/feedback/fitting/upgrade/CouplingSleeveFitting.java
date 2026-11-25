// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.fitting.upgrade;

import io.github.soundgoodizerfan.feedback.core.unit.St;
import io.github.soundgoodizerfan.feedback.fitting.UpgradeFitting;
import io.github.soundgoodizerfan.feedback.machine.MachineTuning;
import io.github.soundgoodizerfan.feedback.machine.bearing.CouplingSleeveItem;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * A sleeve of metal that locks a rotary bearing's body to the shaft as a gear would: the body no longer rides the run as
 * a flywheel but turns the shaft back with the same torque the bearing pulls it with, so a spinning body can drive a line
 * and a loaded line slows the body. The metal's yield strength says how much torque the sleeve carries
 * ({@link #grip()}); the bearing's own cap still bounds it.
 */
public class CouplingSleeveFitting implements UpgradeFitting {

    private final ResourceLocation material;

    public CouplingSleeveFitting(ResourceLocation material) {
        this.material = material;
    }

    public ResourceLocation material() {
        return material;
    }

    @Override
    public ResourceLocation tintMaterial() {
        return material;
    }

    /** 0 to 1: the share of the bearing's torque cap the sleeve's metal carries, its yield strength over {@link MachineTuning#COUPLING_REFERENCE_YIELD}. */
    public float grip() {
        var metal = MaterialTable.get().materials().get(material);
        float yield = metal == null ? 0f : metal.mechanical().yieldStrength().map(St::value).orElse(0f);
        return Math.min(1f, yield / MachineTuning.COUPLING_REFERENCE_YIELD);
    }

    @Override
    public ItemStack getPickItem() {
        return CouplingSleeveItem.of(material);
    }
}
