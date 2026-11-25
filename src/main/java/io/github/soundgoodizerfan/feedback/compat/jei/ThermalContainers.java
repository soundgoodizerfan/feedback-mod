// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.jei;

import java.util.ArrayList;
import java.util.List;

import io.github.soundgoodizerfan.feedback.item.MoldItem;
import io.github.soundgoodizerfan.feedback.machine.ElectricMachineTuning;
import io.github.soundgoodizerfan.feedback.machine.vessel.ThermalVesselBlock;
import io.github.soundgoodizerfan.feedback.machine.vessel.VesselKind;
import io.github.soundgoodizerfan.feedback.machine.vessel.VesselTuning;
import io.github.soundgoodizerfan.feedback.process.ThermalProcess;
import io.github.soundgoodizerfan.feedback.registry.FAutomation;
import io.github.soundgoodizerfan.feedback.registry.FItems;
import io.github.soundgoodizerfan.feedback.registry.FModular;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * What a Thermal Process card can be run in, worked out from the row's own requirements and each container's figures: never a list
 * authored per row, so a new row gets its containers for free. A heating-rate limit rules nothing out, since a gently fired small vessel
 * holds one.
 */
final class ThermalContainers {

    private ThermalContainers() {
    }

    static List<ItemLike> of(ThermalProcess p) {
        List<ItemLike> out = new ArrayList<>();
        if (p.freeze().isPresent()) {
            String shape = p.freeze().get().mold();
            BuiltInRegistries.ITEM.stream().filter(i -> i instanceof MoldItem mold && mold.shape().equals(shape)).forEach(out::add);
            out.add(FItems.CASTING_BASIN.get());
        } else if (p.electrolysis().isPresent()) {
            out.add(FItems.ELECTROLYSER.get());
        } else if (p.dip().isPresent()) {
            // A potion soak is not done in a vessel.
            if (p.dip().get().imbue().isEmpty())
                vessels(out, p.dip().get().minTemperature().value(), false, false);
        } else if (p.phase().isPresent()) {
            var phase = p.phase().get();
            vessels(out, p.minTemperature().value(), false, false);
            for (var liquid : phase.liquid().getStacks())
                if (liquid.is(FluidTags.WATER)) {
                    out.add(FModular.BOILER.get());
                    break;
                }
            if (phase.lightShare().isPresent())
                out.add(FItems.REBOILER.get());
            out.add(FItems.CONDENSER.get());
        } else if (p.quench()) {
            if (p.medium().isEmpty())
                out.add(Items.WATER_BUCKET);
            else
                vessels(out, 0f, false, false);
        } else if (p.isReaction()) {
            out.add(FItems.REACTOR.get());
        } else {
            float need = p.minTemperature().value();
            boolean rabble = p.rabble().isPresent();
            vessels(out, need, p.airless(), rabble);
            if (p.inputs().size() == 1 && !rabble) {
                // The kiln zone runs one-piece rows in open air, into items only.
                if (!p.airless() && !p.hasFluidResult() && p.fire().isEmpty() && p.atmosphere().isEmpty())
                    out.add(FAutomation.KILN_ZONE_ITEM.get());
                // A buried campfire is the other sealed fire, and a cool one.
                if (p.airless() && p.atmosphere().isEmpty() && !p.hasFluidResult() && need <= VesselTuning.FIRE_TU_BURIED_CAMPFIRE.value()) {
                    out.add(FItems.CAMPFIRE.get());
                    out.add(FItems.SOUL_CAMPFIRE.get());
                }
            }
        }
        return out;
    }

    /** The crucibles and self-fired vessels that reach {@code need} Tu and offer what else the row asks. */
    private static void vessels(List<ItemLike> out, float need, boolean airless, boolean rabble) {
        if (!airless && !rabble) {
            out.add(FModular.CRUCIBLE.get());
            if (need <= ElectricMachineTuning.ARC_MAX_TU.value())
                out.add(FItems.ARC_HEARTH.get());
        }
        for (var item : List.of(FItems.FURNACE, FItems.SMOKER, FItems.BLAST_FURNACE, FItems.IMPROVED_FURNACE, FItems.KILN,
                FItems.ANNEALING_FURNACE, FItems.PUDDLING_FURNACE)) {
            Block block = item.get().getBlock();
            if (!(block instanceof ThermalVesselBlock vessel))
                continue;
            VesselKind kind = vessel.getKind();
            if (need <= kind.ceilingTu() && (!airless || kind.sealed()) && (!rabble || kind.rotates()))
                out.add(item.get());
        }
    }
}
