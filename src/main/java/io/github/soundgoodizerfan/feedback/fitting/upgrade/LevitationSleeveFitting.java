// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.fitting.upgrade;

import io.github.soundgoodizerfan.feedback.fitting.UpgradeFitting;
import io.github.soundgoodizerfan.feedback.machine.electromagnet.Diamagnetism;
import io.github.soundgoodizerfan.feedback.machine.levitation.LevitationSleeveItem;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * A sleeve of a diamagnetic material fitted round a shaft, flywheel or rotary bearing. While a powered electromagnet touches the
 * holder the sleeve holds the journal off by the field, and the holder's drag falls ({@code LevitationSleeves.cut}). The material
 * decides how well: bismuth fully, graphite nearly, gold hardly.
 */
public class LevitationSleeveFitting implements UpgradeFitting {

    private final ResourceLocation material;

    public LevitationSleeveFitting(ResourceLocation material) {
        this.material = material;
    }

    public ResourceLocation material() {
        return material;
    }

    /** 0 to 1: the sleeve material's diamagnetism. */
    public float diamagnetism() {
        return Diamagnetism.of(MaterialTable.get().materials().get(material));
    }

    @Override
    public ItemStack getPickItem() {
        return LevitationSleeveItem.of(material);
    }
}
