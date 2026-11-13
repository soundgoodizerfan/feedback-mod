// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.jade;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.client.tooltip.Readout;
import io.github.soundgoodizerfan.feedback.instrument.Quantity;
import io.github.soundgoodizerfan.feedback.machine.Headed;
import io.github.soundgoodizerfan.feedback.machine.Wearing;
import io.github.soundgoodizerfan.feedback.machine.WorkpieceHolder;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * What is in any one-slot machine -- the Hammer and the whole machine shop -- roughly how far
 * along it is, and the machine's condition. Progress is an adjective until calipers are owned,
 * the same rule {@link HammerComponent} states for the spec sheet's other half. Everything here
 * is already on the client: the workpiece is synced for rendering and carries its own Fu.
 */
public class WorkpieceComponent implements IBlockComponentProvider {

    public static final WorkpieceComponent INSTANCE = new WorkpieceComponent();

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (!(accessor.getBlockEntity() instanceof WorkpieceHolder holder))
            return;

        if (holder instanceof Headed headed && headed.getHead().isEmpty()) {
            tooltip.add(Component.translatable("feedback.spec.no_head"));
        } else if (holder instanceof Wearing wearing) {
            Component condition = Readout.conditionReading(wearing.getCondition());
            if (condition != null)
                tooltip.add(condition);
        }

        ItemStack workpiece = holder.getWorkpiece();
        if (workpiece.isEmpty())
            return;

        tooltip.add(workpiece.getHoverName());

        int worked = workpiece.getOrDefault(FDataComponents.WORK.get(), 0);
        int required = workpiece.getOrDefault(FDataComponents.WORK_REQUIRED.get(), 0);
        if (worked <= 0 || required <= 0)
            return;

        if (Readout.instrumented(Quantity.WORK))
            tooltip.add(Readout.reading(Quantity.WORK, "feedback.readout.work", worked, required));
        else
            tooltip.add(Readout.progress(worked, required));
    }

    @Override
    public ResourceLocation getUid() {
        return Feedback.id("workpiece");
    }
}
